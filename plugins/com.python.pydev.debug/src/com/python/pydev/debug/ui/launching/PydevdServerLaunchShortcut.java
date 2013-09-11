/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.ui.launching;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.debug.ui.launching.FileOrResource;
import org.python.pydev.plugin.PydevPlugin;

public class PydevdServerLaunchShortcut extends AbstractLaunchShortcut {

    @Override
    public ILaunchConfiguration createDefaultLaunchConfiguration(FileOrResource[] resources) {
        ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(getLaunchConfigurationType());
        if (type == null) {
            reportError("Python launch configuration not found", null);
            return null;
        }

        StringBuffer buffer = new StringBuffer("Debug Server");
        String name = buffer.toString().trim();

        try {

            ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);

            // Common Tab Arguments
            CommonTab tab = new CommonTab();
            tab.setDefaults(workingCopy);
            tab.dispose();

            // Python Main Tab Arguments
            workingCopy.setAttribute(Constants.ATTR_PROJECT, "PyDevd Debug Server");
            workingCopy.setAttribute(Constants.ATTR_RESOURCE_TYPE, 1);

            workingCopy.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false);
            workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);

            ILaunchConfiguration ret = workingCopy.doSave();
            return ret;
        } catch (CoreException e) {
            reportError(null, e);
            return null;
        }
    }

    @Override
    public void launch(FileOrResource[] file, String mode) {
        ILaunchConfiguration conf = createDefaultLaunchConfiguration(file);
        DebugUITools.launch(conf, mode);
    }

    @Override
    protected String getLaunchConfigurationType() {
        return "com.python.pydev.debug.pydevdServerLaunchConfigurationType";
    }

    @Override
    protected IInterpreterManager getInterpreterManager(IProject project) {
        return PydevPlugin.getPythonInterpreterManager();
    }

}
