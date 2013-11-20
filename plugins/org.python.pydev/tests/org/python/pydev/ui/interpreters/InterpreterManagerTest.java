/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.interpreters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.eclipse.jface.preference.PreferenceStore;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.PydevTestUtils;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author fabioz
 *
 */
public class InterpreterManagerTest extends TestCase {

    private File baseDir;
    private File stateLocation;
    private File additionalPythonpathEntry;

    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            InterpreterManagerTest test2 = new InterpreterManagerTest();
            //            test2.setUp();
            //            test2.testCompletion();
            //            test2.tearDown();

            System.out.println("Finished");

            junit.textui.TestRunner.run(InterpreterManagerTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        ProjectModulesManager.IN_TESTS = true;
        baseDir = PydevTestUtils.setTestPlatformStateLocation();
        baseDir = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC, "data_temporary_for_testing");
        FileUtils.deleteDirectoryTree(baseDir);

        baseDir.mkdirs();
        stateLocation = new File(baseDir, "pydev_plugin_state_location");
        stateLocation.mkdir();
        additionalPythonpathEntry = new File(baseDir, "additional_pythonpath_entry");
        additionalPythonpathEntry.mkdir();

    }

    @Override
    protected void tearDown() throws Exception {
        ProjectModulesManager.IN_TESTS = false;
        FileUtils.deleteDirectoryTree(baseDir);
    }

    public void testInterpreterManager() throws Exception {
        Collection<String> pythonpath = new ArrayList<String>();
        pythonpath.add(TestDependent.PYTHON_LIB);
        pythonpath.add(TestDependent.PYTHON_SITE_PACKAGES);

        PreferenceStore prefs = new PreferenceStore();
        String interpreterStr = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath).toString();
        prefs.setValue(IInterpreterManager.PYTHON_INTERPRETER_PATH, interpreterStr);
        PythonInterpreterManager manager = new PythonInterpreterManager(prefs);
        checkSameInterpreterInfo(manager);

        manager.clearCaches();
        InterpreterInfo info = checkSameInterpreterInfo(manager);

        pythonpath = new ArrayList<String>();
        pythonpath.add(TestDependent.PYTHON_LIB);
        pythonpath.add(TestDependent.PYTHON_SITE_PACKAGES);
        pythonpath.add(additionalPythonpathEntry.toString());
        interpreterStr = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath).toString();
        prefs.setValue(IInterpreterManager.PYTHON_INTERPRETER_PATH, interpreterStr);

        info = checkSameInterpreterInfo(manager);
    }

    private InterpreterInfo checkSameInterpreterInfo(PythonInterpreterManager manager) throws MisconfigurationException {
        InterpreterInfo infoInManager = manager.getInterpreterInfo(TestDependent.PYTHON_EXE, null);
        IInterpreterInfo[] interpreterInfos = manager.getInterpreterInfos();
        assertEquals(1, interpreterInfos.length);
        assertSame(interpreterInfos[0], infoInManager);
        return infoInManager;
    }

}
