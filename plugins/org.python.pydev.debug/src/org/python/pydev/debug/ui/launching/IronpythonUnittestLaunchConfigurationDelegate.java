package org.python.pydev.debug.ui.launching;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;


public class IronpythonUnittestLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate{

    /**
     * @return
     */
    protected String getRunnerConfigRun(ILaunchConfiguration conf, String mode, ILaunch launch) {
        return PythonRunnerConfig.RUN_IRONPYTHON_UNITTEST;
    }

}
