/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.blocks;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * A control for displaying a list of python paths.
 */
public class PythonPathBlock extends AbstractLaunchConfigurationTab {

    private List fPythonPathList;

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText("PYTHONPATH that will be used in the run:");
        fPythonPathList = new List(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);

        GridData gd = new GridData(GridData.FILL_BOTH);
        fPythonPathList.setLayoutData(gd);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Python path";
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration configuration) {

        try {
            String id = configuration.getType().getIdentifier();
            IInterpreterManager manager = null;
            if (Constants.ID_JYTHON_LAUNCH_CONFIGURATION_TYPE.equals(id)
                    || Constants.ID_JYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE.equals(id)) {
                manager = PydevPlugin.getJythonInterpreterManager();

            } else if (Constants.ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE.equals(id)
                    || Constants.ID_IRONPYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE.equals(id)) {
                manager = PydevPlugin.getIronpythonInterpreterManager();

            } else if (Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE.equals(id)
                    || Constants.ID_PYTHON_COVERAGE_LAUNCH_CONFIGURATION_TYPE.equals(id)
                    || Constants.ID_PYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE.equals(id)) {
                manager = PydevPlugin.getPythonInterpreterManager();
            } else {
                //Get from the project
                try {
                    //could throw core exception if project does not exist.
                    IProject project = PythonRunnerConfig.getProjectFromConfiguration(configuration);
                    PythonNature nature = PythonNature.getPythonNature(project);
                    if (nature != null) {
                        manager = PydevPlugin.getInterpreterManager(nature);
                    }
                } catch (Exception e) {
                    Log.log(e);
                }

                if (manager == null) {
                    Log.log("Could not recognize: '" + id + "' using default python interpreter manager.");
                    manager = PydevPlugin.getPythonInterpreterManager();
                }
            }
            String pythonPath = PythonRunnerConfig.getPythonpathFromConfiguration(configuration, manager);

            fPythonPathList.removeAll();
            java.util.List<String> paths = SimpleRunner.splitPythonpath(pythonPath);
            for (String p : paths) {
                fPythonPathList.add(p);
            }
            setErrorMessage(null);
        } catch (Exception e) {
            // Exceptions here may have several reasons
            // - The interpreter is incorrectly configured
            // - The arguments use an unresolved variable.
            // In each case, the exception contains a meaningful message, that is displayed
            Log.log(e);

            String message = e.getMessage();
            if (message == null) {
                message = "null (see error log for the traceback).";
            }
            String errorMsg = StringUtils.replaceNewLines(message, " ");

            fPythonPathList.removeAll();
            fPythonPathList.add(errorMsg);
            setErrorMessage(errorMsg);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        // Nothing to apply, this is a read-only control
        initializeFrom(configuration);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        // No defaults to set
    }
}
