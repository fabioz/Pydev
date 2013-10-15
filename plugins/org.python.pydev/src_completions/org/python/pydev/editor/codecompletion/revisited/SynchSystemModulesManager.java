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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_core.structure.OrderedSet;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.utils.ThreadPriorityHelper;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.dialogs.SelectNDialog;
import org.python.pydev.ui.dialogs.TreeNodeLabelProvider;
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

    public static final boolean DEBUG = false; //TODO: Make false before final

    private static class PythonpathChange {

        private String path;
        private boolean add;

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
        private Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo;
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
                Map<IInterpreterManager, Map<String, IInterpreterInfo>> localManagerToNameToInfo;
                synchronized (lock) {
                    localRoot = this.root;
                    localSelectElements = this.selectElements;
                    localManagerToNameToInfo = this.managerToNameToInfo;

                    this.root = null;
                    this.selectElements = null;
                    this.managerToNameToInfo = null;
                }

                if (localManagerToNameToInfo != null) {
                    synchronizeManagerToNameToInfoPythonpath(monitor, localManagerToNameToInfo, null);
                } else {
                    if (localRoot != null && localSelectElements != null) {
                        applySelectedChangesToInterpreterInfosPythonpath(localRoot, localSelectElements, monitor);
                    }
                }
            } finally {
                priorityHelper.restoreInitialPriority();
            }
            return Status.OK_STATUS;
        }

        public void stack(DataAndImageTreeNode root, List<TreeNode> selectElements) {
            synchronized (lock) {
                this.root = root;
                this.selectElements = selectElements;
                this.managerToNameToInfo = null;
            }
        }

        public void stack(Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo) {
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

                Map<String, IInterpreterInfo> map = new HashMap<>();
                Set<String> changedNames = new HashSet<>();

                //Initialize with the current infos
                IInterpreterInfo[] allInfos = manager.getInterpreterInfos();
                for (IInterpreterInfo info : allInfos) {
                    map.put(info.getName(), info);
                }

                //Override with the ones that should be changed.
                for (IInterpreterInfo info : changedInfos) {
                    if (info.getInterpreterType() == manager.getInterpreterType()) {
                        map.put(info.getName(), info);
                        changedNames.add(info.getName());
                    }
                }

                if (changedNames.size() > 0) {
                    if (DEBUG) {
                        System.out.println("Updating interpreters: " + changedNames);
                    }
                    manager.setInfos(
                            map.values().toArray(new IInterpreterInfo[map.size()]),
                            changedNames,
                            monitor
                            );
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
            Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo,
            CreateInterpreterInfoCallback callback) {
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
        Set<Entry<IInterpreterManager, Map<String, IInterpreterInfo>>> entrySet = managerToNameToInfo.entrySet();
        for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : entrySet) {
            IInterpreterManager manager = entry.getKey();

            Collection<IInterpreterInfo> interpreterInfos = entry.getValue().values();
            for (IInterpreterInfo internalInfo : interpreterInfos) {
                String executable = internalInfo.getExecutableOrJar();
                IInterpreterInfo newInterpreterInfo = callback.createInterpreterInfo(manager, executable, monitor);

                if (newInterpreterInfo == null) {
                    continue;
                }

                IPath rootWorkspaceFile = null;
                try {
                    IWorkspace workspace = ResourcesPlugin.getWorkspace();
                    rootWorkspaceFile = workspace.getRoot().getFullPath();
                } catch (Exception e) {
                    //Ignore (in tests).
                }

                OrderedSet<String> newEntries = new OrderedSet<String>(newInterpreterInfo.getPythonPath());
                newEntries.removeAll(internalInfo.getPythonPath());
                for (Iterator<String> it = newEntries.iterator(); it.hasNext();) {
                    String next = it.next();
                    if (!new File(next).exists()) {
                        it.remove();
                    }
                }
                if (rootWorkspaceFile != null) {
                    for (Iterator<String> it = newEntries.iterator(); it.hasNext();) {
                        String entryInPythonpath = it.next();
                        if (rootWorkspaceFile.isPrefixOf(Path.fromOSString(entryInPythonpath))) {
                            it.remove(); //Ignore entries in the workspace
                        }
                    }
                }

                OrderedSet<String> removedEntries = new OrderedSet<String>();
                List<String> pythonPath = internalInfo.getPythonPath();
                for (String string : pythonPath) {
                    if (!new File(string).exists()) {
                        //Don't remove if it's not in the PYTHONPATH, only if it was removed from the filesystem.
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
    }

    /**
     * Given a passed tree, selects the elements on the tree (and returns the selected elements in a flat list).
     */
    private List<TreeNode> selectElementsInDialog(final DataAndImageTreeNode root) {
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
                true);
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
            Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo,
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
        Set<Entry<IInterpreterManager, Map<String, IInterpreterInfo>>> entrySet = managerToNameToInfo
                .entrySet();
        for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : entrySet) {
            for (Entry<String, IInterpreterInfo> entry2 : entry.getValue().entrySet()) {
                //If it was changed or not, we must check the internal structure too!
                InterpreterInfo info = (InterpreterInfo) entry2.getValue();
                if (DEBUG) {
                    System.out.println("Synchronizing PYTHONPATH info: " + info.getNameForUI());
                }
                long initial = System.currentTimeMillis();
                builder.synchInfoToPythonPath(monitor, info);
                if (DEBUG) {
                    System.out.println("End Synchronizing PYTHONPATH info (" + (System.currentTimeMillis() - initial)
                            / 1000.0 + " secs.)");
                }
            }
        }
    }

    /**
     * Asynchronously selects the elements in a dialog (i.e.: will execute in the UI thread) and then
     * asynchronously again (in a non-ui thread) apply the changes selected.
     * @param managerToNameToInfo
     */
    /*default*/void asyncSelectAndScheduleElementsToChangePythonpath(final DataAndImageTreeNode root,
            final Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo) {
        RunInUiThread.async(new Runnable() {

            @Override
            public void run() {
                List<TreeNode> selectElements = selectElementsInDialog(root);

                if (selectElements != null && selectElements.size() > 0) {
                    jobApplyChanges.stack(root, selectElements);
                } else {
                    jobApplyChanges.stack(managerToNameToInfo);
                }
                jobApplyChanges.schedule();
            }
        });
    }

    /**
     * Note: it's public mostly for tests. Should not be instanced when not in tests!
     */
    public SynchSystemModulesManager() {

    }
}
