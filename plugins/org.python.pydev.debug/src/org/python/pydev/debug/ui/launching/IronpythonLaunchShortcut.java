/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.launching;

import org.eclipse.core.resources.IProject;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;

public class IronpythonLaunchShortcut extends AbstractLaunchShortcut {

    @Override
    protected String getLaunchConfigurationType() {
        return Constants.ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE;
    }

    @Override
    protected IInterpreterManager getInterpreterManager(IProject project) {
        return PydevPlugin.getIronpythonInterpreterManager();
    }

    @Override
    protected boolean getRequireFile() {
        return true;
    }

}
