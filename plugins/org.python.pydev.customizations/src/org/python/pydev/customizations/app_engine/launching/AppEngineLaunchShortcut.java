/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.launching;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.debug.ui.launching.FileOrResource;
import org.python.pydev.plugin.PydevPlugin;

public class AppEngineLaunchShortcut extends AbstractLaunchShortcut {

    @Override
    protected String getLaunchConfigurationType() {
        return AppEngineConstants.APP_ENGINE_LAUNCH_CONFIGURATION_TYPE;
    }

    /**
     * The only thing different is that we have to override the creation of the default launch configuration.
     */
    @Override
    public ILaunchConfiguration createDefaultLaunchConfiguration(FileOrResource[] resources) {

        try {
            ILaunchConfigurationWorkingCopy workingCopy = super
                    .createDefaultLaunchConfigurationWithoutSaving(resources);

            String mainDir = workingCopy.getAttribute(Constants.ATTR_LOCATION, "");

            //dev_appserver.py [options] <application root>
            workingCopy.setAttribute(Constants.ATTR_LOCATION, "${GOOGLE_APP_ENGINE}/dev_appserver.py");
            workingCopy.setAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, "\"" + mainDir + "\"");

            return workingCopy.doSave();
        } catch (CoreException e) {
            reportError(null, e);
            return null;
        }
    }

    @Override
    protected IInterpreterManager getInterpreterManager(IProject project) {
        return PydevPlugin.getPythonInterpreterManager();
    }
}
