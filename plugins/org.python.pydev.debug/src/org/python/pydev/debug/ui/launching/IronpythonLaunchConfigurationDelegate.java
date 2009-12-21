package org.python.pydev.debug.ui.launching;


public class IronpythonLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate {

    @Override
    protected String getRunnerConfigRun() {
        return PythonRunnerConfig.RUN_IRONPYTHON;
    }

}
