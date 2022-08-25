/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: Radim Kubacki (radim@kubacki.cz)
 */
package org.python.pydev.debug.ui.launching;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.python.pydev.ast.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.nature.PythonNature;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for {@link PythonRunnerConfig}
 */
public class PythonRunnerConfigTestWorkbench extends AbstractWorkbenchTestCase {

    private static <T> boolean arrayContains(T[] array, T value) {
        for (T elem : array) {
            if (value.equals(elem)) {
                return true;
            }
        }
        return false;
    }

    // setUp from AbstractJavaIntegrationTestWorkbench opens the file in editor
    // something simpler can be enough

    public void testResourceLocation() throws Exception {
        ILaunchConfiguration config = new JythonLaunchShortcut().createDefaultLaunchConfiguration(FileOrResource
                .createArray(new IResource[] { mod1 }));
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE,
                PythonRunnerConfig.RUN_JYTHON);
        assertEquals(mod1.getLocation(), runnerConfig.resource[0]);
    }

    public void testOverridingResourceLocation() throws Exception {
        ILaunchConfiguration config = new JythonLaunchShortcut().createDefaultLaunchConfiguration(FileOrResource
                .createArray(new IResource[] { mod1 }));
        ILaunchConfigurationWorkingCopy configCopy = config.getWorkingCopy();
        String customResourcePath = "/foo/bar/acme.py";
        configCopy.setAttribute(Constants.ATTR_ALTERNATE_LOCATION, customResourcePath);
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(configCopy, ILaunchManager.RUN_MODE,
                PythonRunnerConfig.RUN_JYTHON);
        assertEquals(Path.fromOSString(customResourcePath), runnerConfig.resource[0]);
    }

    public void testUnittestCommandLine() throws Exception {
        ILaunchConfiguration config = new JythonLaunchShortcut().createDefaultLaunchConfiguration(FileOrResource
                .createArray(new IResource[] { mod1 }));
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE,
                PythonRunnerConfig.RUN_JYTHON);
        String[] argv = runnerConfig.getCommandLine(false);
        assertFalse(arrayContains(argv, PythonRunnerConfig.getRunFilesScript()));
        assertTrue(arrayContains(argv, mod1.getLocation().toOSString()));
    }

    public void testPythonUnittestCommandLine() throws Exception {
        ILaunchConfiguration config = new UnitTestLaunchShortcut().createDefaultLaunchConfiguration(FileOrResource
                .createArray(new IResource[] { mod1 }));
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE,
                PythonRunnerConfig.RUN_UNITTEST);
        String[] argv = runnerConfig.getCommandLine(false);
        assertTrue(arrayContains(argv, PythonRunnerConfig.getRunFilesScript()));
        assertTrue(arrayContains(argv, mod1.getLocation().toOSString()));
    }

    public void testPythonCommandLine() throws Exception {
        PythonNature nature = PythonNature.getPythonNature(mod1);

        // Create a temporary variable for testing
        IStringVariableManager variableManager = VariablesPlugin.getDefault().getStringVariableManager();
        IValueVariable myCustomVariable = variableManager.newValueVariable("pydev_python_runner_config_test_var", "",
                true, "my_custom_value");
        variableManager.addVariables(new IValueVariable[] { myCustomVariable });

        try {
            IInterpreterManager manager = InterpreterManagersAPI.getPythonInterpreterManager(true);
            InterpreterInfo info = (InterpreterInfo) manager.getDefaultInterpreterInfo(false);
            info.setEnvVariables(new String[] { "MY_CUSTOM_VAR_FOR_TEST=FOO", "MY_CUSTOM_VAR_FOR_TEST2=FOO2",
                    "MY_CUSTOM_VAR_WITH_VAR=${pydev_python_runner_config_test_var}" });

            // Make sure variable hasn't been expanded too early
            assertTrue(arrayContains(info.getEnvVariables(),
                    "MY_CUSTOM_VAR_WITH_VAR=${pydev_python_runner_config_test_var}"));

            PythonRunnerConfig runnerConfig = createConfig();
            assertTrue(arrayContains(runnerConfig.envp, "MY_CUSTOM_VAR_FOR_TEST=FOO"));
            assertTrue(arrayContains(runnerConfig.envp, "MY_CUSTOM_VAR_FOR_TEST2=FOO2"));
            assertTrue(arrayContains(runnerConfig.envp, "MY_CUSTOM_VAR_WITH_VAR=my_custom_value"));

            String[] argv = runnerConfig.getCommandLine(false);
            assertFalse(arrayContains(argv, PythonRunnerConfig.getRunFilesScript()));
            assertTrue(arrayContains(argv, mod1.getLocation().toOSString()));

            nature.setVersion(IPythonNature.Versions.PYTHON_VERSION_LATEST, IPythonNature.DEFAULT_INTERPRETER);
            assertEquals(manager.getDefaultInterpreterInfo(false).getExecutableOrJar(), nature.getProjectInterpreter()
                    .getExecutableOrJar());
            runnerConfig = createConfig();
            argv = runnerConfig.getCommandLine(false);
            assertEquals(manager.getDefaultInterpreterInfo(false).getExecutableOrJar(), argv[0]);

            IInterpreterManager interpreterManager = nature.getRelatedInterpreterManager();

            InterpreterInfo info2 = new InterpreterInfo(IPythonNature.PYTHON_VERSION_3_8, "c:\\interpreter\\py25.exe",
                    new ArrayList<String>());
            interpreterManager.setInfos(new IInterpreterInfo[] { info, info2 }, null, null);

            nature.setVersion(IPythonNature.Versions.PYTHON_VERSION_LATEST, "c:\\interpreter\\py25.exe");
            assertEquals("c:\\interpreter\\py25.exe", nature.getProjectInterpreter().getExecutableOrJar());
            runnerConfig = createConfig();
            argv = runnerConfig.getCommandLine(false);
            assertEquals("c:\\interpreter\\py25.exe", argv[0]);
            nature.setVersion(IPythonNature.Versions.PYTHON_VERSION_LATEST, IPythonNature.DEFAULT_INTERPRETER);

            ILaunchConfiguration config;
            config = new LaunchShortcut().createDefaultLaunchConfiguration(FileOrResource
                    .createArray(new IResource[] { mod1 }));
            ILaunchConfigurationWorkingCopy workingCopy = config.getWorkingCopy();
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("VAR_SPECIFIED_IN_LAUNCH", "BAR");
            map.put("MY_CUSTOM_VAR_FOR_TEST2", "BAR2"); //The one in the launch configuration always has preference.
            workingCopy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, map);
            config = workingCopy.doSave();

            runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE, PythonRunnerConfig.RUN_REGULAR);
            assertTrue(arrayContains(runnerConfig.envp, "VAR_SPECIFIED_IN_LAUNCH=BAR"));
            assertTrue(arrayContains(runnerConfig.envp, "MY_CUSTOM_VAR_FOR_TEST=FOO"));
            assertTrue(arrayContains(runnerConfig.envp, "MY_CUSTOM_VAR_FOR_TEST2=BAR2"));
            assertTrue(arrayContains(runnerConfig.envp, "MY_CUSTOM_VAR_WITH_VAR=my_custom_value"));
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            //restore the default!
            nature.setVersion(IPythonNature.Versions.PYTHON_VERSION_LATEST, IPythonNature.DEFAULT_INTERPRETER);
            variableManager.removeVariables(new IValueVariable[] { myCustomVariable });
        }
    }

    private PythonRunnerConfig createConfig() throws CoreException, InvalidRunException, MisconfigurationException {
        ILaunchConfiguration config = new LaunchShortcut().createDefaultLaunchConfiguration(FileOrResource
                .createArray(new IResource[] { mod1 }));
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE,
                PythonRunnerConfig.RUN_REGULAR);
        return runnerConfig;
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(PythonRunnerConfigTestWorkbench.class.getName());
        suite.addTestSuite(PythonRunnerConfigTestWorkbench.class);
        return suite;
    }

}
