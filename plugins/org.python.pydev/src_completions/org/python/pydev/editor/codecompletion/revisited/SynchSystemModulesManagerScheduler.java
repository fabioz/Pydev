package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IInterpreterManagerListener;
import org.python.pydev.core.path_watch.IFilesystemChangesListener;
import org.python.pydev.core.path_watch.PathWatch;
import org.python.pydev.editor.codecompletion.revisited.SynchSystemModulesManager.CreateInterpreterInfoCallback;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_core.utils.ThreadPriorityHelper;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class SynchSystemModulesManagerScheduler implements IInterpreterManagerListener {

    private final SynchSystemModulesManager synchManager = new SynchSystemModulesManager();

    private final Job job = new Job("Synch System PYTHONPATH") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            long initialTime = System.currentTimeMillis();
            ThreadPriorityHelper priorityHelper = new ThreadPriorityHelper(this.getThread());
            priorityHelper.setMinPriority();

            try {
                final DataAndImageTreeNode root = new DataAndImageTreeNode(null, null, null);
                Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo = PydevPlugin
                        .getInterpreterManagerToInterpreterNameToInfo();

                synchManager.updateStructures(monitor, root, managerToNameToInfo, new CreateInterpreterInfoCallback());
                long delta = System.currentTimeMillis() - initialTime;
                if (SynchSystemModulesManager.DEBUG) {
                    System.out.println("Time to check polling for changes in interpreters: " + delta
                            / 1000.0 + " secs.");
                }

                if (root.hasChildren()) {
                    synchManager.asyncSelectAndScheduleElementsToChangePythonpath(root, managerToNameToInfo);
                } else {
                    synchManager.synchronizeManagerToNameToInfoPythonpath(monitor, managerToNameToInfo, null);
                }

            } finally {
                //As jobs are from a thread pool, restore the priority afterwards
                priorityHelper.restoreInitialPriority();
            }
            return Status.OK_STATUS;
        }
    };

    public void registerInterpreterManager(IInterpreterManager iInterpreterManager) {
        afterSetInfos(iInterpreterManager, iInterpreterManager.getInterpreterInfos());
        iInterpreterManager.addListener(this);
    }

    public void start() {
        //Should be called only once, at which point we'll start to check if things change in the pythonpath
        //based on changes in the filesystem.
        job.setPriority(Job.BUILD);
        job.schedule(1000 * 60);

        IInterpreterManager[] managers = PydevPlugin.getAllInterpreterManagers();
        for (IInterpreterManager iInterpreterManager : managers) {
            this.registerInterpreterManager(iInterpreterManager);
        }
    }

    public void stop() {
        job.cancel();
    }

    private static class InfoTracker implements IFilesystemChangesListener {

        public final IInterpreterInfo info;
        public final IInterpreterManager manager;
        public final List<File> filepathsTracked = new ArrayList<>();

        public InfoTracker(IInterpreterManager manager, IInterpreterInfo info) {
            this.manager = manager;
            this.info = info;
        }

        @Override
        public void added(File file) {
            System.out.println("Added: " + file);
            if (file.isDirectory()) {
                //When a directory is added, wait and see if a __init__.py/__init__.pyc is added to it.
                System.out.println("Added possible folder to be in the pythonpath: " + file);

            } else {
                String filename = file.getName();
                if (PythonPathHelper.isValidSourceFile(filename) || filename.endsWith(".pth")) {
                    System.out.println("Added file of interest: " + file);

                }
            }
        }

        @Override
        public void removed(File file) {
            String filename = file.getName();
            if (PythonPathHelper.isValidSourceFile(filename) || filename.endsWith(".pth")) {
                System.out.println("Removed file of interest: " + file);

            } else {
                List<String> pythonPath = info.getPythonPath();
                for (String string : pythonPath) {
                    if (new File(string).equals(file)) {
                        //entry removed from pythonpath
                        System.out.println("Removed entry from pythonpath: " + file);
                    }
                }
            }
        }

        public void registerTracking(File f) {
            filepathsTracked.add(f);
        }

    }

    private final Map<IInterpreterManager, List<InfoTracker>> managerToPathsTracker = new HashMap<>();
    private final Object lock = new Object();

    @Override
    public void afterSetInfos(IInterpreterManager manager, IInterpreterInfo[] interpreterInfos) {
        synchronized (lock) {
            PathWatch pathWatch = PathWatch.get();

            List<InfoTracker> currTrackers = managerToPathsTracker.remove(manager);
            if (currTrackers != null) {
                for (InfoTracker infoTracker : currTrackers) {
                    for (File f : infoTracker.filepathsTracked) {
                        pathWatch.stopTrack(f, infoTracker);
                    }
                }
            }

            currTrackers = new ArrayList<>();
            managerToPathsTracker.put(manager, currTrackers);
            for (IInterpreterInfo info : interpreterInfos) {
                List<String> pythonPath = info.getPythonPath();
                InfoTracker tracker = new InfoTracker(manager, info);
                for (String string : pythonPath) {
                    File f = new File(string);
                    tracker.registerTracking(f);
                    pathWatch.track(f, tracker);
                    currTrackers.add(tracker);
                }
            }
        }
    }
}
