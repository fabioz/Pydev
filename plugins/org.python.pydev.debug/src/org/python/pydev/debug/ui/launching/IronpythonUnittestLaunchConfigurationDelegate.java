package org.python.pydev.debug.ui.launching;


public class IronpythonUnittestLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate{

    /**
     * @return
     */
    protected String getRunnerConfigRun() {
        return PythonRunnerConfig.RUN_IRONPYTHON_UNITTEST;
    }

}
