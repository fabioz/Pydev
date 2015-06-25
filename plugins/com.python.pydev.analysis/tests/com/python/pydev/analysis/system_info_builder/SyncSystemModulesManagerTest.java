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
package com.python.pydev.analysis.system_info_builder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.ManagerInfoToUpdate;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.SyncSystemModulesManagerScheduler;
import org.python.pydev.editor.codecompletion.revisited.SyncSystemModulesManagerScheduler.IInfoTrackerListener;
import org.python.pydev.editor.codecompletion.revisited.SyncSystemModulesManagerScheduler.InfoTracker;
import org.python.pydev.editor.codecompletion.revisited.SynchSystemModulesManager;
import org.python.pydev.editor.codecompletion.revisited.SynchSystemModulesManager.PythonpathChange;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevTestUtils;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.testutils.TestUtils;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.analysis.additionalinfo.builders.InterpreterObserver;

import junit.framework.TestCase;

@SuppressWarnings({ "rawtypes", "unused", "unchecked" })
public class SyncSystemModulesManagerTest extends TestCase {

    private File baseDir;
    private File libDir;
    private File libDir2;
    private File libDir3;
    private File libZipFile;

    @Override
    protected void setUp() throws Exception {
        baseDir = new File(FileUtils.getFileAbsolutePath(new File("InterpreterInfoBuilderTest.temporary_dir")));
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }

        libDir = new File(baseDir, "Lib");
        libDir.mkdirs();
        FileUtils.writeStrToFile("class Module1:pass", new File(libDir, "module1.py"));
        FileUtils.writeStrToFile("class Module2:pass", new File(libDir, "module2.py"));
        FileUtils.writeStrToFile("class Module3:pass", new File(libDir, "module3.py"));

        libDir2 = new File(baseDir, "Lib2");
        libDir2.mkdirs();
        FileUtils.writeStrToFile("class Module4:pass", new File(libDir2, "module4.py"));
        FileUtils.writeStrToFile("class Module5:pass", new File(libDir2, "module5.py"));

        libDir3 = new File(baseDir, "Lib3");
        libDir3.mkdirs();

        libZipFile = new File(baseDir, "entry.egg");
        FileOutputStream stream = new FileOutputStream(libZipFile);
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(stream));
        zipOut.putNextEntry(new ZipEntry("zip_mod.py"));
        zipOut.write("class ZipMod:pass".getBytes());
        zipOut.close();

        PydevTestUtils.setTestPlatformStateLocation();
        ExtensionHelper.testingParticipants = new HashMap<String, List<Object>>();

        //Note: needed to restore additional info!
        List list = Arrays.asList(new InterpreterObserver());
        ExtensionHelper.testingParticipants.put(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER,
                list);

        FileUtils.IN_TESTS = true;
        ProjectModulesManager.IN_TESTS = true;

        PydevPlugin.setPythonInterpreterManager(null);
        PydevPlugin.setIronpythonInterpreterManager(null);
        PydevPlugin.setJythonInterpreterManager(null);

    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectoryTree(baseDir);
        ProjectModulesManager.IN_TESTS = false;
        FileUtils.IN_TESTS = false;
        ExtensionHelper.testingParticipants = null;
    }

    private void setupEnv() throws MisconfigurationException {
        setupEnv(false);
    }

    private void setupEnv(boolean setupInitialInfoProperly) throws MisconfigurationException {
        Collection<String> pythonpath = new ArrayList<String>();
        pythonpath.add(libDir.toString());

        final InterpreterInfo info = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath);

        IPreferenceStore preferences = createPreferenceStore();
        final PythonInterpreterManager manager = new PythonInterpreterManager(preferences);
        PydevPlugin.setPythonInterpreterManager(manager);
        manager.setInfos(new IInterpreterInfo[] { info }, null, null);

        AdditionalSystemInterpreterInfo additionalInfo = new AdditionalSystemInterpreterInfo(manager,
                info.getExecutableOrJar());
        AdditionalSystemInterpreterInfo.setAdditionalSystemInfo(manager, info.getExecutableOrJar(), additionalInfo);

        //Don't load it (otherwise it'll get the 'proper' info).
        if (setupInitialInfoProperly) {
            InterpreterInfo infoOnManager = manager.getInterpreterInfo(info.getExecutableOrJar(),
                    null);
            assertEquals(infoOnManager.getPythonPath(), info.getPythonPath());

            NullProgressMonitor monitor = new NullProgressMonitor();
            info.restorePythonpath(monitor);
            AdditionalSystemInterpreterInfo.recreateAllInfo(manager, info.getExecutableOrJar(),
                    monitor);
            final ISystemModulesManager modulesManager = info.getModulesManager();
            assertEquals(3, modulesManager.getSize(false));
            assertEquals(3, infoOnManager.getModulesManager().getSize(false));
            additionalInfo = (AdditionalSystemInterpreterInfo) AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
                    manager, info.getExecutableOrJar());
            Collection<IInfo> allTokens = additionalInfo.getAllTokens();
            assertEquals(3, additionalInfo.getAllTokens().size());

        } else {
            final ISystemModulesManager modulesManager = info.getModulesManager();
            assertEquals(0, modulesManager.getSize(false));
            assertEquals(0, additionalInfo.getAllTokens().size());
        }
    }

    private PreferenceStore createPreferenceStore() {
        return new PreferenceStore(new File(baseDir, "preferenceStore").toString());
    }

    public void testUpdateWhenEggIsAdded() throws Exception {
        setupEnv(true);

        SynchSystemModulesManager synchManager = new SynchSystemModulesManager();

        final DataAndImageTreeNode root = new DataAndImageTreeNode(null, null, null);
        Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfoMap = PydevPlugin
                .getInterpreterManagerToInterpreterNameToInfo();
        ManagerInfoToUpdate managerToNameToInfo = new ManagerInfoToUpdate(managerToNameToInfoMap);
        checkUpdateStructures(synchManager, root, managerToNameToInfo);
        checkSynchronize(synchManager, root, managerToNameToInfo);

        root.clear();
        managerToNameToInfo = new ManagerInfoToUpdate(PydevPlugin.getInterpreterManagerToInterpreterNameToInfo());
        synchManager.updateStructures(null, root, managerToNameToInfo,
                new SynchSystemModulesManager.CreateInterpreterInfoCallback() {
                    @Override
                    public IInterpreterInfo createInterpreterInfo(IInterpreterManager manager, String executable,
                            IProgressMonitor monitor) {
                        Collection<String> pythonpath = new ArrayList<String>();
                        pythonpath.add(libDir.toString());
                        pythonpath.add(libZipFile.toString());

                        final InterpreterInfo info = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath);
                        return info;
                    }
                });
        assertTrue(root.hasChildren());

        List<TreeNode> selectElements = new ArrayList<>();
        selectElements.addAll(root.flattenChildren());
        synchManager.applySelectedChangesToInterpreterInfosPythonpath(root, selectElements, null);

        List<IInterpreterInfo> allInterpreterInfos = PydevPlugin.getAllInterpreterInfos();
        for (IInterpreterInfo interpreterInfo : allInterpreterInfos) {
            assertEquals(4, interpreterInfo.getModulesManager().getSize(false));

            AdditionalSystemInterpreterInfo additionalInfo = (AdditionalSystemInterpreterInfo) AdditionalSystemInterpreterInfo
                    .getAdditionalSystemInfo(
                            interpreterInfo.getModulesManager().getInterpreterManager(),
                            interpreterInfo.getExecutableOrJar());
            Collection<IInfo> allTokens = additionalInfo.getAllTokens();
            assertEquals(4, additionalInfo.getAllTokens().size());
        }
    }

    public void testScheduleCheckForUpdates() throws Exception {
        setupEnv();

        Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo = PydevPlugin
                .getInterpreterManagerToInterpreterNameToInfo();

        SyncSystemModulesManagerScheduler scheduler = new SyncSystemModulesManagerScheduler();
        final Set changes = Collections.synchronizedSet(new HashSet<>());
        try {
            Set<Entry<IInterpreterManager, Map<String, IInterpreterInfo>>> entrySet = managerToNameToInfo.entrySet();

            SyncSystemModulesManagerScheduler.IInfoTrackerListener listener = new IInfoTrackerListener() {

                @Override
                public void onChangedIInterpreterInfo(InfoTracker infoTracker, File file) {
                    changes.add(file);
                }
            };

            for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : entrySet) {
                Map<String, IInterpreterInfo> value = entry.getValue();
                scheduler.afterSetInfos(entry.getKey(), value.values().toArray(new IInterpreterInfo[value.size()]),
                        listener);
            }
            final File module4File = new File(libDir, "module4.py");
            FileUtils.writeStrToFile("class Module3:pass", module4File);
            TestUtils.waitUntilCondition(new ICallback<String, Object>() {

                @Override
                public String call(Object arg) {
                    if (changes.contains(module4File)) {
                        return null;
                    }
                    return "Changes not found.";
                }
            });

            changes.clear();
            final File myPthFile = new File(libDir, "my.pth");
            FileUtils.writeStrToFile("./setuptools-1.1.6-py2.6.egg", myPthFile);
            TestUtils.waitUntilCondition(new ICallback<String, Object>() {

                @Override
                public String call(Object arg) {
                    if (changes.contains(myPthFile)) {
                        return null;
                    }
                    return "Changes not found.";
                }
            });

            synchronized (this) {
                this.wait(250); //Wait a bit as we may have 2 notifications (for creation and modification of the pth).
            }
            //Now, add an unrelated directory: no notifications are expected then.
            changes.clear();
            final File myUnrelatedDir = new File(libDir, "unrelatedDir");
            myUnrelatedDir.mkdir();
            synchronized (this) {
                this.wait(250);
            }
            assertEquals(new HashSet<>(), changes); //no changes expected
        } finally {
            scheduler.stop();
        }
        changes.clear();
        final File myPthFile2 = new File(libDir, "my2.pth");
        FileUtils.writeStrToFile("./setuptools-1.1.7-py2.6.egg", myPthFile2);
        synchronized (this) {
            this.wait(250);
        }
        assertEquals(new HashSet<>(), changes);
    }

    public void testUpdateAndApply() throws Exception {
        setupEnv();

        SynchSystemModulesManager synchManager = new SynchSystemModulesManager();

        final DataAndImageTreeNode root = new DataAndImageTreeNode(null, null, null);
        Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfoMap = PydevPlugin
                .getInterpreterManagerToInterpreterNameToInfo();
        ManagerInfoToUpdate managerToNameToInfo = new ManagerInfoToUpdate(managerToNameToInfoMap);
        checkUpdateStructures(synchManager, root, managerToNameToInfo);
        checkSynchronize(synchManager, root, managerToNameToInfo);

        //Ok, the interpreter should be synchronized with the pythonpath which is currently set.
        //Now, check a different scenario: create a new path and add it to the interpreter pythonpath.
        //In this situation, the sync manager should ask the user if that path should actually be added
        //to this interpreter.
        root.clear();
        managerToNameToInfo = new ManagerInfoToUpdate(PydevPlugin.getInterpreterManagerToInterpreterNameToInfo());
        synchManager.updateStructures(null, root, managerToNameToInfo,
                new SynchSystemModulesManager.CreateInterpreterInfoCallback() {
                    @Override
                    public IInterpreterInfo createInterpreterInfo(IInterpreterManager manager, String executable,
                            IProgressMonitor monitor) {
                        Collection<String> pythonpath = new ArrayList<String>();
                        pythonpath.add(libDir.toString());
                        pythonpath.add(libDir2.toString());

                        final InterpreterInfo info = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath);
                        return info;
                    }
                });
        assertTrue(root.hasChildren());

        List<TreeNode> selectElements = new ArrayList<>();
        selectElements.addAll(root.flattenChildren());
        synchManager.applySelectedChangesToInterpreterInfosPythonpath(root, selectElements, null);

        List<IInterpreterInfo> allInterpreterInfos = PydevPlugin.getAllInterpreterInfos();
        for (IInterpreterInfo interpreterInfo : allInterpreterInfos) {
            assertEquals(5, interpreterInfo.getModulesManager().getSize(false));
        }
    }

    private void checkUpdateStructures(SynchSystemModulesManager synchManager, final DataAndImageTreeNode root,
            ManagerInfoToUpdate managerToNameToInfo) {
        synchManager.updateStructures(null, root, managerToNameToInfo,
                new SynchSystemModulesManager.CreateInterpreterInfoCallback() {
                    @Override
                    public IInterpreterInfo createInterpreterInfo(IInterpreterManager manager, String executable,
                            IProgressMonitor monitor) {
                        Collection<String> pythonpath = new ArrayList<String>();
                        pythonpath.add(libDir.toString());

                        //Still the same!
                        final InterpreterInfo info = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath);
                        return info;
                    }
                });

        Tuple<IInterpreterManager, IInterpreterInfo>[] managerAndInfos = managerToNameToInfo.getManagerAndInfos();
        int found = managerAndInfos.length;
        assertEquals(found, 1);
    }

    private void checkSynchronize(SynchSystemModulesManager synchManager, final DataAndImageTreeNode root,
            ManagerInfoToUpdate managerToNameToInfo) {
        //Ok, all is Ok in the PYTHONPATH, so, check if something changed inside the interpreter info
        //and not on the PYTHONPATH.

        assertFalse(root.hasChildren());
        InterpreterInfoBuilder builder = new InterpreterInfoBuilder();
        synchManager.synchronizeManagerToNameToInfoPythonpath(null, managerToNameToInfo, builder);
        Tuple<IInterpreterManager, IInterpreterInfo>[] managerAndInfos = managerToNameToInfo.getManagerAndInfos();
        for (Tuple<IInterpreterManager, IInterpreterInfo> tuple : managerAndInfos) {
            InterpreterInfo interpreterInfo = (InterpreterInfo) tuple.o2;
            assertEquals(3, interpreterInfo.getModulesManager().getSize(false));
        }
    }

    public void testSaveUserChoicesAfterSelection() throws Exception {
        setupEnv(false);

        IPreferenceStore preferences = createPreferenceStore();
        SynchSystemModulesManager synchManager = new SynchSystemModulesManager();

        final DataAndImageTreeNode root = new DataAndImageTreeNode(null, null, null);
        Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo = PydevPlugin
                .getInterpreterManagerToInterpreterNameToInfo();

        synchManager.updateStructures(null, root, new ManagerInfoToUpdate(managerToNameToInfo),
                new SynchSystemModulesManager.CreateInterpreterInfoCallback() {
                    @Override
                    public IInterpreterInfo createInterpreterInfo(IInterpreterManager manager, String executable,
                            IProgressMonitor monitor) {
                        Collection<String> pythonpath = new ArrayList<>();
                        pythonpath.add(libDir.toString());
                        pythonpath.add(libDir2.toString());
                        pythonpath.add(libDir3.toString());
                        pythonpath.add(libZipFile.toString());

                        final InterpreterInfo info = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath);
                        return info;
                    }
                });
        assertTrue(root.hasChildren());

        List<TreeNode> selectedElements = new ArrayList<>();
        TreeNode interpreterNode = (TreeNode) root.getChildren().get(0);
        selectedElements.add(interpreterNode);
        List<TreeNode> children = interpreterNode.getChildren();
        for (TreeNode<PythonpathChange> treeNode : children) {
            if (treeNode.getData().path.equals(libDir2.toString())) {
                selectedElements.add(treeNode);
            }
        }
        synchManager.saveUnselected(root, selectedElements, preferences);

        //Check that we ignored libDir3 and libZipFile
        String key = SynchSystemModulesManager.createKeyForInfo((IInterpreterInfo) ((TreeNode) root.getChildren()
                .get(0)).getData());
        String entry = preferences.getString(key);
        List<String> entries = StringUtils.split(entry, "|||");
        assertEquals(2, entries.size());
        HashSet<String> entriesSet = new HashSet<>(entries);
        assertEquals(new HashSet(entries), new HashSet(Arrays.asList(libDir3.toString(), libZipFile.toString())));

        //Check that only libDir2 is initially selected.
        List<TreeNode> initialSelection = synchManager.createInitialSelectionForDialogConsideringPreviouslyIgnored(
                root, preferences);
        assertEquals(2, initialSelection.size());
        TreeNode treeNode = initialSelection.get(0);
        TreeNode treeNode1 = initialSelection.get(1);
        TreeNode interpreterInfoNode;
        TreeNode pythonpathNode;

        if (treeNode.getData() instanceof IInterpreterInfo) {
            interpreterNode = treeNode;
            pythonpathNode = treeNode1;
        } else {
            interpreterNode = treeNode1;
            pythonpathNode = treeNode;
        }
        assertEquals(((PythonpathChange) pythonpathNode.getData()).path, libDir2.toString());
    }
}
