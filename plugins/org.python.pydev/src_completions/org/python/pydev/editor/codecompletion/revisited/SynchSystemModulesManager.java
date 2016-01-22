/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial API and implementation
******************************************************************************/
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_core.structure.OrderedSet;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ThreadPriorityHelper;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.dialogs.SelectNDialog;
import org.python.pydev.ui.dialogs.TreeNodeLabelProvider;
import org.python.pydev.ui.pythonpathconf.DefaultPathsForInterpreterInfo;
import org.python.pydev.ui.pythonpathconf.IInterpreterInfoBuilder;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * This is a helper class to keep the PYTHONPATH of interpreters configured inside Eclipse with the PYTHONPATH that
 * the interpreter actually has currently.
 *
 * I.e.: doing on the command-line:
 *
 * d:\bin\Python265\Scripts\pip-2.6.exe install path.py --egg
 * d:\bin\Python265\Scripts\pip-2.6.exe uninstall path.py
 *
 * which will change the pythonpath should actually request a pythonpath update
 *
 *
 * Also, doing:
 *
 * d:\bin\Python265\Scripts\pip-2.6.exe install path.py
 * d:\bin\Python265\Scripts\pip-2.6.exe uninstall path.py
 *
 * which will only download the path.py without actually changing the pythonpath must also work!
 *
 * @author Fabio
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SynchSystemModulesManager {

    public static final boolean DEBUG = false;

    public static class PythonpathChange {

        public final String path;
        public final boolean add;

        public PythonpathChange(String path, boolean add) {
            this.path = path;
            this.add = add;
        }

        @Override
        public String toString() {
            if (add) {
                return "Add to PYTHONPATH: " + path;
            }
            return "Remove from PYTHONPATH: " + path;
        }

        public void apply(OrderedSet<String> newPythonPath) {
            if (add) {
                newPythonPath.add(path);
            } else {
                newPythonPath.remove(path);
            }
        }
    }

    private class JobApplyChanges extends Job {

        private DataAndImageTreeNode root;
        private List<TreeNode> selectElements;
        private ManagerInfoToUpdate managerToNameToInfo;
        private final Object lock = new Object();

        public JobApplyChanges() {
            super("Apply PYTHONPATH changes");
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            ThreadPriorityHelper priorityHelper = new ThreadPriorityHelper(this.getThread());
            priorityHelper.setMinPriority();
            try {
                DataAndImageTreeNode localRoot;
                List<TreeNode> localSelectElements;
                ManagerInfoToUpdate localManagerToNameToInfo;
                synchronized (lock) {
                    localRoot = this.root;
                    localSelectElements = this.selectElements;
                    localManagerToNameToInfo = this.managerToNameToInfo;

                    this.root = null;
                    this.selectElements = null;
                    this.managerToNameToInfo = null;
                }

                if (localRoot != null && localSelectElements != null) {
                    applySelectedChangesToInterpreterInfosPythonpath(localRoot, localSelectElements, monitor);
                } else if (localManagerToNameToInfo != null) {
                    synchronizeManagerToNameToInfoPythonpath(monitor, localManagerToNameToInfo, null);
                }
            } finally {
                priorityHelper.restoreInitialPriority();
            }
            return Status.OK_STATUS;
        }

        public void stack(DataAndImageTreeNode root, List<TreeNode> selectElements,
                ManagerInfoToUpdate managerToNameToInfo) {
            synchronized (lock) {
                this.root = root;
                this.selectElements = selectElements;
                this.managerToNameToInfo = managerToNameToInfo; //important to check the initial time!
            }
        }

        public void stack(ManagerInfoToUpdate managerToNameToInfo) {
            synchronized (lock) {
                this.root = null;
                this.selectElements = null;
                this.managerToNameToInfo = managerToNameToInfo;
            }
        }
    }

    private final JobApplyChanges jobApplyChanges = new JobApplyChanges();

    public static class CreateInterpreterInfoCallback {

        public IInterpreterInfo createInterpreterInfo(IInterpreterManager manager, String executable,
                IProgressMonitor monitor) {
            boolean askUser = false;
            try {
                return manager.createInterpreterInfo(executable, monitor, askUser);
            } catch (Exception e) {
                Log.log(e);
            }
            return null;
        }
    }

    public void applySelectedChangesToInterpreterInfosPythonpath(
            final DataAndImageTreeNode root, List<TreeNode> selectElements, IProgressMonitor monitor) {
        List<IInterpreterInfo> changedInfos = computeChanges(root, selectElements);

        if (changedInfos.size() > 0) {
            IInterpreterManager[] allInterpreterManagers = PydevPlugin.getAllInterpreterManagers();

            for (IInterpreterManager manager : allInterpreterManagers) {
                if (manager == null) {
                    continue;
                }

                Map<String, IInterpreterInfo> changedInterpreterNameToInterpreter = new HashMap<>();
                for (IInterpreterInfo info : changedInfos) {
                    changedInterpreterNameToInterpreter.put(info.getName(), info);
                }

                IInterpreterInfo[] allInfos = manager.getInterpreterInfos();
                List<Object> newInfos = new ArrayList<>(allInfos.length);
                Set<String> changedNames = new HashSet<>();

                //Important: keep the order in which the user configured the interpreters.
                for (IInterpreterInfo info : allInfos) {
                    IInterpreterInfo changedInfo = changedInterpreterNameToInterpreter.remove(info.getName());
                    if (changedInfo != null) {
                        //Override with the ones that should be changed.
                        newInfos.add(changedInfo);
                        changedNames.add(changedInfo.getExecutableOrJar());

                    } else {
                        newInfos.add(info);
                    }
                }

                if (changedNames.size() > 0) {
                    if (DEBUG) {
                        System.out.println("Updating interpreters: " + changedNames);
                    }
                    manager.setInfos(
                            newInfos.toArray(new IInterpreterInfo[newInfos.size()]),
                            changedNames,
                            monitor);
                }
            }
        }
    }

    /**
     * Here we'll update the tree structure to be shown to the user with the changes (root).
     * The managerToNameToInfo structure has the information on the interpreter manager and related
     * interpreter infos for which the changes should be checked.
     */
    public void updateStructures(IProgressMonitor monitor, final DataAndImageTreeNode root,
            ManagerInfoToUpdate managerToNameToInfo, CreateInterpreterInfoCallback callback) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        ImageCache imageCache = SharedUiPlugin.getImageCache();
        if (imageCache == null) {
            imageCache = new ImageCache(null) { //create dummy for tests
                @Override
                public Image get(String key) {
                    return null;
                }
            };
        }

        for (Tuple<IInterpreterManager, IInterpreterInfo> infos : managerToNameToInfo.getManagerAndInfos()) {
            IInterpreterManager manager = infos.o1;
            IInterpreterInfo internalInfo = infos.o2;
            String executable = internalInfo.getExecutableOrJar();
            IInterpreterInfo newInterpreterInfo = callback.createInterpreterInfo(manager, executable, monitor);

            if (newInterpreterInfo == null) {
                continue;
            }
            DefaultPathsForInterpreterInfo defaultPaths = new DefaultPathsForInterpreterInfo();

            OrderedSet<String> newEntries = new OrderedSet<String>(newInterpreterInfo.getPythonPath());
            newEntries.removeAll(internalInfo.getPythonPath());
            //Iterate over the new entries to suggest what should be added (we already have only what's not there).
            for (Iterator<String> it = newEntries.iterator(); it.hasNext();) {
                String entryInPythonpath = it.next();
                if (!defaultPaths.selectByDefault(entryInPythonpath) || !defaultPaths.exists(entryInPythonpath)) {
                    it.remove(); //Don't suggest the addition of entries in the workspace or entries which do not exist.
                }
            }

            //Iterate over existing entries to suggest what should be removed.
            OrderedSet<String> removedEntries = new OrderedSet<String>();
            List<String> pythonPath = internalInfo.getPythonPath();
            for (String string : pythonPath) {
                if (!new File(string).exists()) {
                    //Only suggest a removal if it was removed from the filesystem.
                    removedEntries.add(string);
                }
            }

            if (newEntries.size() > 0 || removedEntries.size() > 0) {
                DataAndImageTreeNode<IInterpreterInfo> interpreterNode = new DataAndImageTreeNode<IInterpreterInfo>(
                        root,
                        internalInfo,
                        imageCache.get(
                                UIConstants.PY_INTERPRETER_ICON));

                for (String s : newEntries) {
                    new DataAndImageTreeNode(interpreterNode, new PythonpathChange(s, true),
                            imageCache.get(UIConstants.LIB_SYSTEM));
                }
                for (String s : removedEntries) {
                    new DataAndImageTreeNode(interpreterNode, new PythonpathChange(s, false),
                            imageCache.get(UIConstants.REMOVE_LIB_SYSTEM));
                }
            }
        }
    }

    /**
     * Given a passed tree, selects the elements on the tree (and returns the selected elements in a flat list).
     */
    private List<TreeNode> selectElementsInDialog(final DataAndImageTreeNode root, List<TreeNode> initialSelection) {
        List<TreeNode> selectElements = SelectNDialog.selectElements(root,
                new TreeNodeLabelProvider() {
                    @Override
                    public org.eclipse.swt.graphics.Image getImage(Object element) {
                        DataAndImageTreeNode n = (DataAndImageTreeNode) element;
                        return n.image;
                    };

                    @Override
                    public String getText(Object element) {
                        TreeNode n = (TreeNode) element;
                        Object data = n.getData();
                        if (data == null) {
                            return "null";
                        }
                        if (data instanceof IInterpreterInfo) {
                            IInterpreterInfo iInterpreterInfo = (IInterpreterInfo) data;
                            return iInterpreterInfo.getNameForUI();
                        }
                        return data.toString();
                    };
                },
                "System PYTHONPATH changes detected",
                "Please check which interpreters and paths should be updated.",
                true,
                initialSelection);
        return selectElements;
    }

    /**
     * Given the tree structure we created initially with all the changes (root) and the elements
     * that the user selected in the tree (selectElements), return a list of infos updated with the
     * proper pythonpath.
     */
    private List<IInterpreterInfo> computeChanges(final DataAndImageTreeNode root,
            List<TreeNode> selectElements) {
        List<IInterpreterInfo> changedInfos = new ArrayList<>();

        HashSet<TreeNode> set = new HashSet<TreeNode>(selectElements.size());
        set.addAll(selectElements);
        for (Object n : root.getChildren()) {
            DataAndImageTreeNode interpreterNode = (DataAndImageTreeNode) n;
            if (set.contains(interpreterNode)) {
                IInterpreterInfo info = (IInterpreterInfo) interpreterNode.getData();
                List<String> pythonPath = info.getPythonPath();

                boolean changed = false;
                OrderedSet<String> newPythonPath = new OrderedSet<String>(pythonPath);
                for (Object entryNode : interpreterNode.getChildren()) {
                    DataAndImageTreeNode pythonpathNode = (DataAndImageTreeNode) entryNode;
                    if (set.contains(pythonpathNode)) {
                        PythonpathChange change = (PythonpathChange) pythonpathNode.data;
                        change.apply(newPythonPath);
                        changed = true;
                    }
                }
                if (changed) {
                    InterpreterInfo copy = (InterpreterInfo) info.makeCopy();
                    copy.libs.clear();
                    copy.libs.addAll(newPythonPath);
                    changedInfos.add(copy);
                }
            }
        }
        return changedInfos;
    }

    public void synchronizeManagerToNameToInfoPythonpath(IProgressMonitor monitor,
            ManagerInfoToUpdate localManagerToNameToInfo,
            IInterpreterInfoBuilder builder) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        if (builder == null) {
            builder = (IInterpreterInfoBuilder) ExtensionHelper
                    .getParticipant(ExtensionHelper.PYDEV_INTERPRETER_INFO_BUILDER, false);

        }
        //Ok, all is Ok in the PYTHONPATH, so, check if something changed inside the interpreter info
        //and not on the PYTHONPATH.
        Tuple<IInterpreterManager, IInterpreterInfo>[] managerAndInfos = localManagerToNameToInfo.getManagerAndInfos();
        for (Tuple<IInterpreterManager, IInterpreterInfo> tuple : managerAndInfos) {
            //If it was changed or not, we must check the internal structure too!
            InterpreterInfo info = (InterpreterInfo) tuple.o2;
            if (DEBUG) {
                System.out.println("Synchronizing PYTHONPATH info: " + info.getNameForUI());
            }
            long initial = System.currentTimeMillis();
            builder.syncInfoToPythonPath(monitor, info);
            if (DEBUG) {
                System.out.println("End Synchronizing PYTHONPATH info (" + (System.currentTimeMillis() - initial)
                        / 1000.0 + " secs.)");
            }
        }
    }

    private boolean selectingElementsInDialog = false;

    public boolean getSelectingElementsInDialog() {
        synchronized (selectingElementsInDialogLock) {
            return selectingElementsInDialog;
        }
    }

    private final Object selectingElementsInDialogLock = new Object();

    /**
     * Asynchronously selects the elements in a dialog (i.e.: will execute in the UI thread) and then
     * asynchronously again (in a non-ui thread) apply the changes selected.
     * @param managerToNameToInfo
     */
    /*default*/void asyncSelectAndScheduleElementsToChangePythonpath(final DataAndImageTreeNode root,
            final ManagerInfoToUpdate managerToNameToInfo,
            final List<TreeNode> initialSelection) {
        RunInUiThread.async(new Runnable() {

            @Override
            public void run() {
                synchronized (selectingElementsInDialogLock) {
                    if (selectingElementsInDialog) {
                        if (DEBUG) {
                            System.out.println("Bailing out: a dialog is already showing.");
                        }
                        return;
                    }
                    selectingElementsInDialog = true;
                }
                try {
                    if (managerToNameToInfo.somethingChanged()) {
                        if (DEBUG) {
                            System.out.println("Not asking anything because something changed in the meanwhile.");
                        }
                        return; //If something changed, don't do anything (we should automatically reschedule in this case).
                    }
                    List<TreeNode> selectedElements = selectElementsInDialog(root, initialSelection);
                    saveUnselected(root, selectedElements, PydevPrefs.getPreferences());
                    if (selectedElements != null && selectedElements.size() > 0) {
                        jobApplyChanges.stack(root, selectedElements, managerToNameToInfo);
                    } else {
                        jobApplyChanges.stack(managerToNameToInfo);
                    }
                    jobApplyChanges.schedule();
                } finally {
                    synchronized (selectingElementsInDialogLock) {
                        selectingElementsInDialog = false;
                    }
                }
            }
        });
    }

    /**
     * When the user selects changes in selectElementsInDialog, it's possible that he doesn't check some of the
     * proposed changes, thus, in this case, we should save the unselected items in the preferences and the next
     * time such a change is proposed, it should appear unchecked (and if all changes are unchecked, we shouldn't
     * present the user with a dialog).
     *
     * @param root this is the initial structure, containing all the proposed changes.
     * @param selectedElements this is a structure which will hold only the selected changes.
     * @param iPreferenceStore this is the store where we'll keep the selected changes.
     */
    public void saveUnselected(DataAndImageTreeNode root, List<TreeNode> selectedElements,
            IPreferenceStore iPreferenceStore) {
        //root has null data, level 1 has IInterpreterInfo and level 2 has PythonpathChange.
        HashSet<TreeNode> selectionSet = new HashSet<>();
        if (selectedElements != null && selectedElements.size() > 0) {
            selectionSet.addAll(selectedElements);
        }

        boolean changed = false;
        for (DataAndImageTreeNode<IInterpreterInfo> interpreterNode : (List<DataAndImageTreeNode<IInterpreterInfo>>) root
                .getChildren()) {
            Set<TreeNode> addToIgnore = new HashSet<>();
            if (!selectionSet.contains(interpreterNode)) {
                //ignore all the entries below this interpreter.
                addToIgnore.addAll(interpreterNode.getChildren());
            } else {
                //check each entry and only add the ones not selected.
                for (TreeNode<PythonpathChange> pathNode : interpreterNode.getChildren()) {
                    if (!selectionSet.contains(pathNode)) {
                        addToIgnore.add(pathNode);
                    }
                }
            }

            if (addToIgnore.size() > 0) {
                IInterpreterInfo info = interpreterNode.getData();
                String key = createKeyForInfo(info);

                ArrayList<String> addToIgnorePaths = new ArrayList<String>(addToIgnore.size());
                for (TreeNode<PythonpathChange> node : addToIgnore) {
                    PythonpathChange data = node.getData();
                    addToIgnorePaths.add(data.path);
                }
                if (DEBUG) {
                    System.out.println("Setting key: " + key);
                    System.out.println("Paths ignored: " + addToIgnorePaths);
                }
                changed = true;
                iPreferenceStore.setValue(key, StringUtils.join("|||", addToIgnorePaths));
            }
        }
        if (changed) {
            if (iPreferenceStore instanceof IPersistentPreferenceStore) {
                IPersistentPreferenceStore iPersistentPreferenceStore = (IPersistentPreferenceStore) iPreferenceStore;
                try {
                    iPersistentPreferenceStore.save();
                } catch (IOException e) {
                    Log.log(e);
                }
            }
        }
    }

    public static String createKeyForInfo(IInterpreterInfo info) {
        return "synch_ignore_entries_"
                + StringUtils.md5(info.getName() + "_" + info.getExecutableOrJar());
    }

    public List<TreeNode> createInitialSelectionForDialogConsideringPreviouslyIgnored(DataAndImageTreeNode root,
            IPreferenceStore iPreferenceStore) {
        List<TreeNode> initialSelection = new ArrayList<>();
        for (DataAndImageTreeNode<IInterpreterInfo> interpreterNode : (List<DataAndImageTreeNode<IInterpreterInfo>>) root
                .getChildren()) {

            IInterpreterInfo info = interpreterNode.getData();
            String key = createKeyForInfo(info);

            String ignoredValue = iPreferenceStore.getString(key);
            if (ignoredValue != null && ignoredValue.length() > 0) {
                Set<String> previouslyIgnored = new HashSet(StringUtils.split(ignoredValue, "|||"));

                boolean added = false;
                for (TreeNode<PythonpathChange> pathNode : interpreterNode.getChildren()) {
                    if (!previouslyIgnored.contains(pathNode.data.path)) {
                        initialSelection.add(pathNode);
                        added = true;
                    } else {
                        if (SynchSystemModulesManager.DEBUG) {
                            System.out.println("Removed from initial selection: " + pathNode);
                        }
                    }
                }
                if (added) {
                    initialSelection.add(interpreterNode);
                }
            } else {
                //Node and children all selected initially (nothing ignored).
                initialSelection.add(interpreterNode);
                initialSelection.addAll(interpreterNode.getChildren());
            }
        }
        if (SynchSystemModulesManager.DEBUG) {
            for (TreeNode treeNode : initialSelection) {
                System.out.println("Initial selection: " + treeNode.getData());
            }
        }
        return initialSelection;
    }

    /**
     * Note: it's public mostly for tests. Should not be instanced when not in tests!
     */
    public SynchSystemModulesManager() {

    }

}
