/*
 * Author: Radim Kubacki (radim@kubacki.cz)
 */
package org.python.pydev.debug.ui.launching;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;

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

    /*
     * I want also this but need python project rather than Jython
     * 
     * config = new LaunchShortcut().createDefaultLaunchConfiguration(new IResource[] { mod1 } ); runnerConfig = new
     * PythonRunnerConfig(config, ILaunchManager.RUN_MODE, PythonRunnerConfig.RUN_REGULAR); argv =
     * runnerConfig.getCommandLine(true); assertFalse(arrayContains(argv, PythonRunnerConfig.getRunFilesScript()));
     * assertTrue(arrayContains(argv, mod1.getLocation().toOSString()));
     * 
     * config = new UnitTestLaunchShortcut().createDefaultLaunchConfiguration(new IResource[] { mod1 } ); runnerConfig =
     * new PythonRunnerConfig(config, ILaunchManager.RUN_MODE, PythonRunnerConfig.RUN_UNITTEST); argv =
     * runnerConfig.getCommandLine(true); assertTrue(arrayContains(argv, PythonRunnerConfig.getRunFilesScript()));
     * assertTrue(arrayContains(argv, mod1.getLocation().toOSString())); }
     */
}
