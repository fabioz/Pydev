package org.python.pydev.debug.pyunit;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.actions.RestartLaunchAction;
import org.python.pydev.plugin.PydevPlugin;

public class PyUnitLaunch implements IPyUnitLaunch{

    private ILaunchConfiguration configuration;
    private ILaunch launch;

    public PyUnitLaunch(ILaunch launch, ILaunchConfiguration configuration) {
        this.launch = launch;
        this.configuration = configuration;
    }

    public void stop() {
        try {
            this.launch.terminate(); //doing this should call dispose later on.
        } catch (DebugException e) {
            PydevPlugin.log(e);
        }
    }

    public void relaunch() {
        RestartLaunchAction.relaunch(launch, configuration);
    }

    public void relaunchTestResults(ArrayList<PyUnitTestResult> runsToRelaunch) {
        FastStringBuffer buf = new FastStringBuffer(100*runsToRelaunch.size());
        for (PyUnitTestResult pyUnitTestResult : runsToRelaunch) {
            buf.append(pyUnitTestResult.location).append("|").append(pyUnitTestResult.test).append('\n');
        }
        
        try {
            ILaunchConfigurationWorkingCopy workingCopy;
            String name = configuration.getName();
            if(name.indexOf("[errors relaunch]") != -1){
                //if it's already an errors relaunch, just change it
                workingCopy = configuration.getWorkingCopy();
            }else{
                //if it's not, create a copy, as we don't want to screw with the original launch
                workingCopy = configuration.copy(name+" [errors relaunch]");
            }
            //When running it, it'll put the contents we set in the buf string into a file and pass that 
            //file to the actual unittest run.
            workingCopy.setAttribute(Constants.ATTR_UNITTEST_CONFIGURATION_FILE, buf.toString());
            ILaunchConfiguration newConf = workingCopy.doSave();
            RestartLaunchAction.relaunch(launch, newConf);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        
    }

}
