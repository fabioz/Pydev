/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IInterpreterManagerListener;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.SynchSystemModulesManager.CreateInterpreterInfoCallback;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.path_watch.EventsStackerRunnable;
import org.python.pydev.shared_core.path_watch.IFilesystemChangesListener;
import org.python.pydev.shared_core.path_watch.IPathWatch;
import org.python.pydev.shared_core.path_watch.PathWatch;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.utils.ThreadPriorityHelper;
import org.python.pydev.ui.interpreters.AbstractInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterGeneralPreferencesPage;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SyncSystemModulesManagerScheduler implements IInterpreterManagerListener {

    private final IPathWatch pathWatch = new PathWatch();

    public SyncSystemModulesManagerScheduler() {
        pathWatch.setDirectoryFileFilter(filter, dirFilter);
    }

    private final SynchJob job = new SynchJob("Sync System PYTHONPATH");

    /**
     * Registers some interpreter manager to be tracked for changes.
     * @param interpreterInfos
     * @return true if there are infos to be tracked and false otherwise.
     */
    private void registerInterpreterManager(IInterpreterManager iInterpreterManager, IInterpreterInfo[] interpreterInfos) {
        //This will make us start tracking changes in the filesystem.
        afterSetInfos(iInterpreterManager, interpreterInfos);

        //Make sure we're called again when the paths in the interpreter manager change.
        iInterpreterManager.addListener(this);
    }

    /**
     * To be called when we start the plugin.
     *
     * Should be called only once (when we'll make a full check for the current integrity of the information)
     * Later on, we'll start to check if things change in the PYTHONPATH based on changes in the filesystem.
     */
    public void start() {

        boolean scheduleInitially = false;
        boolean reCheckOnFilesystemChanges = InterpreterGeneralPreferencesPage.getReCheckOnFilesystemChanges();

        IInterpreterManager[] managers = PydevPlugin.getAllInterpreterManagers();
        for (IInterpreterManager iInterpreterManager : managers) {
            if (iInterpreterManager != null) {
                IInterpreterInfo[] interpreterInfos = iInterpreterManager.getInterpreterInfos();
                if (reCheckOnFilesystemChanges) {
                    this.registerInterpreterManager(iInterpreterManager, interpreterInfos);
                }
                scheduleInitially = scheduleInitially || (interpreterInfos != null && interpreterInfos.length > 0);
            }
        }

        int timeout = 1000 * 30; //Default is waiting 30 seconds after startup

        IPreferenceStore preferences = PydevPrefs.getPreferences();
        boolean alreadyChecked = preferences.getBoolean("INTERPRETERS_CHECKED_ONCE"); //Now we add builtin indexing on our checks (so, force it at least once).
        boolean force = false;
        if (!alreadyChecked) {
            preferences.setValue("INTERPRETERS_CHECKED_ONCE", true); //Now we add builtin indexing on our checks (so, force it at least once).
            force = true;
            timeout = 1000 * 7; //In this case, wait only 7 seconds after startup
        }

        if (force || InterpreterGeneralPreferencesPage.getCheckConsistentOnStartup()) {
            if (scheduleInitially) {
                //Only do the initial schedule if there's something to be tracked (otherwise, wait for some interpreter
                //to be configured and work only on deltas already).

                //The initial job will do a full check on what's available and if it's synched with the filesystem.
                job.addAllToTrack();
                job.scheduleLater(timeout); //Wait a minute before starting our sync process.
            }
        }
    }

    public void addToCheck(AbstractInterpreterManager manager, IInterpreterInfo[] infos) {
        for (IInterpreterInfo info : infos) {
            job.addToTrack(manager, info);
        }
        //Give some seconds for it to start...
        job.scheduleLater(4 * 1000);
    }

    public void checkAllNow() {
        //Add all to be tracked
        Map<IInterpreterManager, Map<String, IInterpreterInfo>> addedToTrack = job.addAllToTrack();

        //remove from the preferences any ignore the user had set previously
        Set<Entry<IInterpreterManager, Map<String, IInterpreterInfo>>> entrySet = addedToTrack.entrySet();
        IPreferenceStore preferences = PydevPrefs.getPreferences();
        for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : entrySet) {
            Set<Entry<String, IInterpreterInfo>> entrySet2 = entry.getValue().entrySet();
            for (Entry<String, IInterpreterInfo> entry2 : entrySet2) {
                String key = SynchSystemModulesManager.createKeyForInfo(entry2.getValue());
                preferences.setValue(key, "");
            }
        }
        if (preferences instanceof IPersistentPreferenceStore) {
            IPersistentPreferenceStore iPersistentPreferenceStore = (IPersistentPreferenceStore) preferences;
            try {
                iPersistentPreferenceStore.save();
            } catch (IOException e) {
                Log.log(e);
            }
        }

        //schedule changes to be executed.
        job.scheduleLater(0);
    }

    /**
     * Stops the synchronization.
     */
    public void stop() {
        job.cancel();
        IInterpreterManager[] managers = PydevPlugin.getAllInterpreterManagers();
        synchronized (lockSetInfos) {
            for (IInterpreterManager iInterpreterManager : managers) {
                if (iInterpreterManager != null) {
                    stopTrack(iInterpreterManager, pathWatch);
                }
            }
        }
        pathWatch.dispose();
    }

    private static final class PyFilesFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            //Only consider python files
            String name = pathname.getName();
            return PythonPathHelper.isValidFileMod(name) || name.endsWith(".pth");
        }
    }

    private static final class PyDirFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return PythonPathHelper.isFolderWithInit(pathname);
        }
    }

    private static final FileFilter filter = new PyFilesFilter();
    private static final FileFilter dirFilter = new PyDirFilter();

    private static final class SynchJob extends Job {

        private SynchJob(String name) {
            super(name);
            setPriority(Job.BUILD);
        }

        private final SynchSystemModulesManager fSynchManager = new SynchSystemModulesManager();

        private Object fManagerToNameToInfoLock = new Object();

        private Map<IInterpreterManager, Map<String, IInterpreterInfo>> fManagerToNameToInfo = null;

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            boolean selectingElementsInDialog = fSynchManager.getSelectingElementsInDialog();
            if (selectingElementsInDialog) {
                //No point in starting a process if the user already has a dialog related to this process open.
                if (SynchSystemModulesManager.DEBUG) {
                    System.out.println("Dialog already showing: rescheduling new check for later.");
                }
                this.scheduleLater(20000);
                return Status.OK_STATUS;
            }
            if (SynchSystemModulesManager.DEBUG) {
                System.out.println("Running SynchJob!");
            }

            if (monitor == null) {
                monitor = new NullProgressMonitor();
            }
            ManagerInfoToUpdate managerToNameToInfo;
            synchronized (fManagerToNameToInfoLock) {
                if (this.fManagerToNameToInfo == null || this.fManagerToNameToInfo.size() == 0) {
                    return Status.OK_STATUS; //nothing to do if there's nothing there...
                }
                managerToNameToInfo = new ManagerInfoToUpdate(this.fManagerToNameToInfo);
                this.fManagerToNameToInfo = null;
            }

            long initialTime = System.currentTimeMillis();
            ThreadPriorityHelper priorityHelper = new ThreadPriorityHelper(this.getThread());
            priorityHelper.setMinPriority();

            try {
                final DataAndImageTreeNode root = new DataAndImageTreeNode(null, null, null);
                if (monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                fSynchManager.updateStructures(monitor, root, managerToNameToInfo, new CreateInterpreterInfoCallback());
                long delta = System.currentTimeMillis() - initialTime;
                if (SynchSystemModulesManager.DEBUG) {
                    System.out.println("Time to check polling for changes in interpreters: " + delta
                            / 1000.0 + " secs.");
                }

                List<TreeNode> initialSelection = new ArrayList<>(0);
                if (root.hasChildren()) {
                    initialSelection = fSynchManager.createInitialSelectionForDialogConsideringPreviouslyIgnored(root,
                            PydevPrefs.getPreferences());
                }

                if (root.hasChildren() && initialSelection.size() > 0) {
                    if (SynchSystemModulesManager.DEBUG) {
                        System.out.println("Changes found in PYTHONPATH.");
                    }
                    fSynchManager.asyncSelectAndScheduleElementsToChangePythonpath(root, managerToNameToInfo,
                            initialSelection);
                } else {
                    if (SynchSystemModulesManager.DEBUG) {
                        System.out.println("PYTHONPATH remained the same.");
                    }
                    fSynchManager.synchronizeManagerToNameToInfoPythonpath(monitor, managerToNameToInfo, null);
                }

            } finally {
                //As jobs are from a thread pool, restore the priority afterwards
                priorityHelper.restoreInitialPriority();
            }
            return Status.OK_STATUS;
        }

        /**
         * @return a (shallow) copy of the elements added to track.
         */
        public Map<IInterpreterManager, Map<String, IInterpreterInfo>> addAllToTrack() {
            synchronized (fManagerToNameToInfoLock) {
                fManagerToNameToInfo = PydevPlugin
                        .getInterpreterManagerToInterpreterNameToInfo();
                Map<IInterpreterManager, Map<String, IInterpreterInfo>> copy = new HashMap<IInterpreterManager, Map<String, IInterpreterInfo>>();

                Set<Entry<IInterpreterManager, Map<String, IInterpreterInfo>>> entrySet = fManagerToNameToInfo
                        .entrySet();
                for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : entrySet) {
                    HashMap<String, IInterpreterInfo> value = new HashMap<>(entry.getValue());
                    copy.put(entry.getKey(), value);
                }
                return copy;
            }

        }

        public void addToTrack(IInterpreterManager manager, IInterpreterInfo info) {
            synchronized (fManagerToNameToInfoLock) {
                if (fManagerToNameToInfo == null) {
                    fManagerToNameToInfo = new HashMap<>();
                }
                Map<String, IInterpreterInfo> map = fManagerToNameToInfo.get(manager);
                if (map == null) {
                    map = new HashMap<>();
                    fManagerToNameToInfo.put(manager, map);
                }
                map.put(info.getName(), info);
            }
        }

        private Thread scheduleThread;
        private volatile long runAt;
        private final Object scheduleThreadLock = new Object();

        /**
         * Differently from the regular schedule, this will create a thread which will
         * call the actual schedule() only after the given amount of time passes, but if it's
         * already scheduled, it'll only make it execute after more time passes.
         */
        public void scheduleLater(long millis) {
            if (SynchSystemModulesManager.DEBUG) {
                System.out.println("(Re)Scheduling change for: " + millis / 1000.0 + " secs.");
            }
            runAt = System.currentTimeMillis() + millis;

            synchronized (scheduleThreadLock) {
                if (scheduleThread == null) {
                    scheduleThread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                long currentTimeMillis = System.currentTimeMillis();
                                long delta = currentTimeMillis - runAt;
                                while (delta < 0) {
                                    try {
                                        //Don't sleep too much as the time can be changed to lower by a new call.
                                        sleep(250);
                                    } catch (InterruptedException e) {

                                    }
                                    currentTimeMillis = System.currentTimeMillis();
                                    delta = currentTimeMillis - runAt;
                                }
                                synchronized (scheduleThreadLock) {
                                    if (SynchSystemModulesManager.DEBUG) {
                                        System.out.println("Actually schedulling job!");
                                    }
                                    SynchJob.this.schedule();
                                    scheduleThread = null;
                                }
                            } catch (Exception e) {
                                Log.log(e);
                            }
                        };
                    };
                    scheduleThread.start();
                }
            }
        }
    }

    public static interface IInfoTrackerListener {

        void onChangedIInterpreterInfo(InfoTracker infoTracker, File file);

    }

    /**
     * Helper class: when a path in the IInterpreterInfo changes some content (or actual path),
     * it calls a listener to take action (i.e.: validate contents).
     */
    public static final class InfoTracker implements IFilesystemChangesListener {

        public final IInterpreterInfo info;
        public final IInterpreterManager manager;
        public final List<File> filepathsTracked = new ArrayList<>();
        private final IInfoTrackerListener listener;

        public InfoTracker(IInterpreterManager manager, IInterpreterInfo info, IInfoTrackerListener listener) {
            this.manager = manager;
            this.info = info;
            this.listener = listener;
        }

        @Override
        public void added(File file) {
            //Note: report directly as we should be only listening to what we want with the passed filter.
            listener.onChangedIInterpreterInfo(this, file);
        }

        @Override
        public void removed(File file) {
            //Note: report directly as we should be only listening to what we want with the passed filter.
            listener.onChangedIInterpreterInfo(this, file);
        }

        public void registerTracking(File f) {
            filepathsTracked.add(f);
        }

    }

    private final Map<IInterpreterManager, List<InfoTracker>> managerToPathsTracker = new HashMap<>();
    private final IInfoTrackerListener fListener = new IInfoTrackerListener() {

        @Override
        public void onChangedIInterpreterInfo(InfoTracker infoTracker, File file) {
            if (SynchSystemModulesManager.DEBUG) {
                System.out.println("File changed :" + file + " starting track of: " + infoTracker.info.getNameForUI());
            }
            job.addToTrack(infoTracker.manager, infoTracker.info);
            if (file.exists() && file.isDirectory()) {
                //If it's a directory, it may be a copy operation, so, check until the copy finishes (i.e.:
                //poll for changes and when there are no changes anymore scheduleLater it right away).

                job.scheduleLater(1000);
                long lastFound = 0;
                while (true) {
                    long lastModified = FileUtils.getLastModifiedTimeFromDir(file, filter, dirFilter,
                            EventsStackerRunnable.LEVELS_TO_GET_MODIFIED_TIME);
                    if (lastFound == lastModified) {
                        break;
                    }
                    lastFound = lastModified;

                    //If we don't have a change in the directory structure for 500 millis, stop it.
                    synchronized (this) {
                        try {
                            this.wait(500);
                        } catch (InterruptedException e) {
                            //Ignore
                        }
                    }
                    if (lastFound == 0) {
                        return; //I.e.: found no interesting file.
                    }
                    job.scheduleLater(1000);
                }

            } else {
                job.scheduleLater(5 * 1000); // 5 seconds
            }
        }
    };

    private final Object lockSetInfos = new Object();

    @Override
    public void afterSetInfos(IInterpreterManager manager, IInterpreterInfo[] interpreterInfos) {
        this.afterSetInfos(manager, interpreterInfos, fListener);
    }

    public void afterSetInfos(IInterpreterManager manager, IInterpreterInfo[] interpreterInfos,
            IInfoTrackerListener listener) {
        synchronized (lockSetInfos) {
            stopTrack(manager, pathWatch);

            List<InfoTracker> currTrackers = new ArrayList<>();
            managerToPathsTracker.put(manager, currTrackers);
            for (IInterpreterInfo info : interpreterInfos) {
                List<String> pythonPath = info.getPythonPath();
                InfoTracker tracker = new InfoTracker(manager, info, listener);
                for (String string : pythonPath) {
                    File f = new File(string);
                    if (SynchSystemModulesManager.DEBUG) {
                        System.out.println("Tracking file: " + f + " for: " + info.getNameForUI());
                    }
                    tracker.registerTracking(f);
                    pathWatch.track(f, tracker);
                    currTrackers.add(tracker);
                }
            }
        }
    }

    /**
     * Must be synchronized (lockSetInfos).
     */
    private void stopTrack(IInterpreterManager manager, IPathWatch pathWatch) {
        List<InfoTracker> currTrackers = managerToPathsTracker.remove(manager);
        if (currTrackers != null) {
            for (InfoTracker infoTracker : currTrackers) {
                for (File f : infoTracker.filepathsTracked) {
                    pathWatch.stopTrack(f, infoTracker);
                }
            }
        }
    }

}
