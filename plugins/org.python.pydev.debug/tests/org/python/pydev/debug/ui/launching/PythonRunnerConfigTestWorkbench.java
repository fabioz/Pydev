/*
 * Author: Radim Kubacki (radim@kubacki.cz)
 */
package org.python.pydev.debug.ui.launching;

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

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
        ILaunchConfiguration config = new JythonLaunchShortcut().createDefaultLaunchConfiguration(new IResource[] { mod1 });
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE,
                PythonRunnerConfig.RUN_JYTHON);
        assertEquals(mod1.getLocation(), runnerConfig.resource[0]);
    }

    public void testOverridingResourceLocation() throws Exception {
        ILaunchConfiguration config = new JythonLaunchShortcut().createDefaultLaunchConfiguration(new IResource[] { mod1 });
        ILaunchConfigurationWorkingCopy configCopy = config.getWorkingCopy();
        String customResourcePath = "/foo/bar/acme.py";
        configCopy.setAttribute(Constants.ATTR_ALTERNATE_LOCATION, customResourcePath);
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(configCopy, ILaunchManager.RUN_MODE,
                PythonRunnerConfig.RUN_JYTHON);
        assertEquals(Path.fromOSString(customResourcePath), runnerConfig.resource[0]);
    }

    public void testUnittestCommandLine() throws Exception {
        ILaunchConfiguration config = new JythonLaunchShortcut().createDefaultLaunchConfiguration(new IResource[] { mod1 });
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE,
                PythonRunnerConfig.RUN_JYTHON);
        String[] argv = runnerConfig.getCommandLine(true);
        assertFalse(arrayContains(argv, PythonRunnerConfig.getRunFilesScript()));
        assertTrue(arrayContains(argv, mod1.getLocation().toOSString()));
    }
    
    public void testPythonUnittestCommandLine() throws Exception {
        ILaunchConfiguration config = new UnitTestLaunchShortcut().createDefaultLaunchConfiguration(new IResource[] { mod1 });
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE, PythonRunnerConfig.RUN_UNITTEST);
        String[] argv = runnerConfig.getCommandLine(true);
        assertTrue(arrayContains(argv, PythonRunnerConfig.getRunFilesScript()));
        assertTrue(arrayContains(argv, mod1.getLocation().toOSString())); 
    }

    
    public void testPythonCommandLine() throws Exception {
        PythonNature nature = PythonNature.getPythonNature(mod1);
        
        try{
            IInterpreterManager manager = PydevPlugin.getPythonInterpreterManager(true);
            InterpreterInfo info = (InterpreterInfo) manager.getInterpreterInfo(manager.getDefaultInterpreter(), new NullProgressMonitor());
            info.setEnvVariables(new String[]{"MY_CUSTOM_VAR_FOR_TEST=FOO", "MY_CUSTOM_VAR_FOR_TEST2=FOO2"});
            
            
            PythonRunnerConfig runnerConfig = createConfig();
            assertTrue(arrayContains(runnerConfig.envp, "MY_CUSTOM_VAR_FOR_TEST=FOO"));
            assertTrue(arrayContains(runnerConfig.envp, "MY_CUSTOM_VAR_FOR_TEST2=FOO2"));
            
            String[] argv = runnerConfig.getCommandLine(true); 
            assertFalse(arrayContains(argv, PythonRunnerConfig.getRunFilesScript()));
            assertTrue(arrayContains(argv, mod1.getLocation().toOSString()));
            
            
            nature.setVersion(IPythonNature.PYTHON_VERSION_LATEST, IPythonNature.DEFAULT_INTERPRETER);
            assertEquals(manager.getDefaultInterpreter(), nature.getProjectInterpreter());
            runnerConfig = createConfig();
            argv = runnerConfig.getCommandLine(true); 
            assertEquals(manager.getDefaultInterpreter(), argv[0]);
            
            nature.setVersion(IPythonNature.PYTHON_VERSION_LATEST, "c:\\interpreter\\py25.exe");
            assertEquals("c:\\interpreter\\py25.exe", nature.getProjectInterpreter());
            runnerConfig = createConfig();
            argv = runnerConfig.getCommandLine(true); 
            assertEquals("c:\\interpreter\\py25.exe", argv[0]);
            nature.setVersion(IPythonNature.PYTHON_VERSION_LATEST, IPythonNature.DEFAULT_INTERPRETER);

            ILaunchConfiguration config;
            
            config = new LaunchShortcut().createDefaultLaunchConfiguration(new IResource[] { mod1 });
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
        }catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }finally{
            //restore the default!
            nature.setVersion(IPythonNature.PYTHON_VERSION_LATEST, IPythonNature.DEFAULT_INTERPRETER);
        }
    }

    private PythonRunnerConfig createConfig() throws CoreException, InvalidRunException {
        ILaunchConfiguration config = new LaunchShortcut().createDefaultLaunchConfiguration(new IResource[] { mod1 });
        PythonRunnerConfig runnerConfig = new PythonRunnerConfig(config, ILaunchManager.RUN_MODE, PythonRunnerConfig.RUN_REGULAR);
        return runnerConfig;
    }

}
