/*
 * Created on 14/08/2005
 */
package org.python.pydev.debug.ui.launching;


public class JythonLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate {

    @Override
    protected String getRunnerConfigRun() {
        return PythonRunnerConfig.RUN_JYTHON;
    }

}
