/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Aug 26, 2003
 */
package org.python.pydev.debug.ui.launching;

import org.eclipse.core.resources.IProject;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;

public class LaunchShortcut extends AbstractLaunchShortcut {

    @Override
    protected String getLaunchConfigurationType() {
        return Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE;
    }

    @Override
    protected boolean getRequireFile() {
        return true;
    }

    @Override
    protected IInterpreterManager getInterpreterManager(IProject project) {
        return PydevPlugin.getPythonInterpreterManager();
    }
}
