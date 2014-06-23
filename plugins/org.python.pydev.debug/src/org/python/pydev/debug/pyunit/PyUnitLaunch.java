/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.Launch;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.debug.RestartLaunchAction;

public class PyUnitLaunch implements IPyUnitLaunch {

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
            Log.log(e);
        }
    }

    public void relaunch() {
        RestartLaunchAction.relaunch(launch, configuration);
    }

    public void relaunchTestResults(List<PyUnitTestResult> runsToRelaunch) {
        this.relaunchTestResults(runsToRelaunch, null);
    }

    public void relaunchTestResults(List<PyUnitTestResult> runsToRelaunch, String mode) {
        FastStringBuffer buf = new FastStringBuffer(100 * runsToRelaunch.size());
        for (PyUnitTestResult pyUnitTestResult : runsToRelaunch) {
            buf.append(pyUnitTestResult.location).append("|").append(pyUnitTestResult.test).append('\n');
        }

        try {
            ILaunchConfigurationWorkingCopy workingCopy;
            String name = configuration.getName();
            if (name.indexOf("[pyunit run]") != -1) {
                //if it's already an errors relaunch, just change it
                workingCopy = configuration.getWorkingCopy();
            } else {
                //if it's not, create a copy, as we don't want to screw with the original launch
                workingCopy = configuration.copy(name + " [pyunit run]");
            }
            //When running it, it'll put the contents we set in the buf string into a file and pass that 
            //file to the actual unittest run.
            workingCopy.setAttribute(Constants.ATTR_UNITTEST_CONFIGURATION_FILE, buf.toString());
            ILaunchConfiguration newConf = workingCopy.doSave();
            ILaunch l = launch;
            if (mode != null) {
                String launchMode = launch.getLaunchMode();
                if (!mode.equals(launchMode)) {
                    l = new Launch(newConf, mode, launch.getSourceLocator());
                }
            }
            RestartLaunchAction.relaunch(l, newConf);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }

    }

}
