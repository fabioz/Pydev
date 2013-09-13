/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.Tuple;

public abstract class AbstractRunEditorAction extends PyAction {

    protected Tuple<String, IInterpreterManager> getLaunchConfigurationTypeAndInterpreterManager(PyEdit pyEdit,
            boolean isUnitTest) {
        String launchConfigurationType;
        String defaultType = Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE;
        IInterpreterManager interpreterManager = PydevPlugin.getPythonInterpreterManager();

        try {
            IPythonNature nature = pyEdit.getPythonNature();
            if (nature == null) {
                launchConfigurationType = defaultType;
            } else {
                int interpreterType = nature.getInterpreterType();
                interpreterManager = nature.getRelatedInterpreterManager();
                switch (interpreterType) {
                    case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                        if (isUnitTest) {
                            launchConfigurationType = Constants.ID_PYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE;
                        } else {
                            launchConfigurationType = Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE;
                        }
                        break;
                    case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                        if (isUnitTest) {
                            launchConfigurationType = Constants.ID_IRONPYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE;
                        } else {
                            launchConfigurationType = Constants.ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE;
                        }
                        break;
                    case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                        if (isUnitTest) {
                            launchConfigurationType = Constants.ID_JYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE;
                        } else {
                            launchConfigurationType = Constants.ID_JYTHON_LAUNCH_CONFIGURATION_TYPE;
                        }
                        break;
                    default:
                        throw new RuntimeException("Cannot recognize type: " + interpreterType);
                }
            }
        } catch (Exception e) {
            Log.log(IStatus.INFO, "Problem determining nature type. Using regular python launch.", e);
            launchConfigurationType = defaultType;
        }

        return new Tuple<String, IInterpreterManager>(launchConfigurationType, interpreterManager);
    }

}
