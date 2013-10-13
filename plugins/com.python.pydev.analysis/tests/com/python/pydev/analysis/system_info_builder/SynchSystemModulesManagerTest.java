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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.SynchSystemModulesManager;
import org.python.pydev.editor.codecompletion.revisited.SynchSystemModulesManagerScheduler;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevTestUtils;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;

public class SynchSystemModulesManagerTest extends TestCase {

    private File baseDir;
    private File libDir;
    private File libDir2;

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
        FileUtils.writeStrToFile("class Module3:pass", new File(libDir2, "module4.py"));
        FileUtils.writeStrToFile("class Module4:pass", new File(libDir2, "module5.py"));

        PydevTestUtils.setTestPlatformStateLocation();
        ExtensionHelper.testingParticipants = new HashMap<String, List<Object>>();
        FileUtilsFileBuffer.IN_TESTS = true;
        ProjectModulesManager.IN_TESTS = true;
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectoryTree(baseDir);
        ProjectModulesManager.IN_TESTS = false;
        FileUtilsFileBuffer.IN_TESTS = false;
        ExtensionHelper.testingParticipants = null;
    }

    private void setupEnv() throws MisconfigurationException {
        Collection<String> pythonpath = new ArrayList<String>();
        pythonpath.add(libDir.toString());

        final InterpreterInfo info = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath);

        IPreferenceStore preferences = new PreferenceStore();
        final PythonInterpreterManager manager = new PythonInterpreterManager(preferences);
        PydevPlugin.setPythonInterpreterManager(manager);
        manager.setInfos(new IInterpreterInfo[] { info }, new HashSet<String>(), null);

        final AdditionalSystemInterpreterInfo additionalInfo = new AdditionalSystemInterpreterInfo(manager,
                info.getExecutableOrJar());
        AdditionalSystemInterpreterInfo.setAdditionalSystemInfo(manager, info.getExecutableOrJar(), additionalInfo);

        //Don't load it (otherwise it'll get the 'proper' info).
        //AdditionalSystemInterpreterInfo.loadAdditionalSystemInfo(manager, info.getExecutableOrJar());

        final ISystemModulesManager modulesManager = info.getModulesManager();
        assertEquals(0, modulesManager.getSize(false));
        assertEquals(0, additionalInfo.getAllTokens().size());
    }

    public void testScheduleCheckForUpdates() throws Exception {
        setupEnv();

        Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo = PydevPlugin
                .getInterpreterManagerToInterpreterNameToInfo();

        SynchSystemModulesManagerScheduler scheduler = new SynchSystemModulesManagerScheduler();
        Set<Entry<IInterpreterManager, Map<String, IInterpreterInfo>>> entrySet = managerToNameToInfo.entrySet();
        for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : entrySet) {
            Map<String, IInterpreterInfo> value = entry.getValue();
            scheduler.afterSetInfos(entry.getKey(), value.values().toArray(new IInterpreterInfo[value.size()]));
        }
        FileUtils.writeStrToFile("class Module3:pass", new File(libDir, "module4.py"));
        synchronized (this) {
            wait(2000);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testIt() throws Exception {
        setupEnv();

        SynchSystemModulesManager synchManager = new SynchSystemModulesManager();

        final DataAndImageTreeNode root = new DataAndImageTreeNode(null, null, null);
        Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo = PydevPlugin
                .getInterpreterManagerToInterpreterNameToInfo();
        checkUpdateStructures(synchManager, root, managerToNameToInfo);
        checkSynchronize(synchManager, root, managerToNameToInfo);

        //Ok, the interpreter should be synchronized with the pythonpath which is currently set.
        //Now, check a different scenario: create a new path and add it to the interpreter pythonpath.
        //In this situation, the synch manager should ask the user if that path should actually be added
        //to this interpreter.
        root.clear();
        managerToNameToInfo = PydevPlugin.getInterpreterManagerToInterpreterNameToInfo();
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
        selectElements.addAll(root.flatten());
        synchManager.applySelectedChangesToInterpreterInfosPythonpath(root, selectElements, null);

        List<IInterpreterInfo> allInterpreterInfos = PydevPlugin.getAllInterpreterInfos();
        for (IInterpreterInfo interpreterInfo : allInterpreterInfos) {
            assertEquals(5, interpreterInfo.getModulesManager().getSize(false));
        }
    }

    private void checkUpdateStructures(SynchSystemModulesManager synchManager, final DataAndImageTreeNode root,
            Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo) {
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

        int found = 0;
        Set<Entry<IInterpreterManager, Map<String, IInterpreterInfo>>> entrySet = managerToNameToInfo
                .entrySet();
        for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : entrySet) {
            for (Entry<String, IInterpreterInfo> entry2 : entry.getValue().entrySet()) {
                found++;
            }
        }
        assertEquals(found, 1);
    }

    private void checkSynchronize(SynchSystemModulesManager synchManager, final DataAndImageTreeNode root,
            Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo) {
        //Ok, all is Ok in the PYTHONPATH, so, check if something changed inside the interpreter info
        //and not on the PYTHONPATH.

        assertFalse(root.hasChildren());
        InterpreterInfoBuilder builder = new InterpreterInfoBuilder();
        synchManager.synchronizeManagerToNameToInfoPythonpath(null, managerToNameToInfo, builder);
        Set<Entry<IInterpreterManager, Map<String, IInterpreterInfo>>> entrySet = managerToNameToInfo.entrySet();
        for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : entrySet) {
            for (Entry<String, IInterpreterInfo> entry2 : entry.getValue().entrySet()) {
                InterpreterInfo interpreterInfo = (InterpreterInfo) entry2.getValue();
                assertEquals(3, interpreterInfo.getModulesManager().getSize(false));
            }
        }
    }
}
