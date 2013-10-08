/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.launching;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Used to *programatically* run a python file located inside a project 
 * (as opposed than throught the UI shourtcuts mechanism).
 * 
 * Automatically sets the right environment to run the script and
 * shows the output on a console.
 * 
 * Motivating use case: Django projects and their manage.py script.
 * 
 * @author Leo Soto
 */
public class PythonFileRunner {

    public static ILaunch launch(IFile file, String arguments) throws CoreException {
        try {
            ILaunchConfiguration conf = getLaunchConfiguration(file, arguments);
            return conf.launch(ILaunchManager.RUN_MODE, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ILaunchConfigurationWorkingCopy getLaunchConfiguration(IFile resource, String programArguments)
            throws CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
        String vmargs = ""; // Not sure if it should be a parameter or not
        IProject project = resource.getProject();
        PythonNature nature = PythonNature.getPythonNature(project);
        ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
        String launchConfigurationType = configurationFor(nature.getInterpreterType());
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(launchConfigurationType);
        if (type == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Python launch configuration not found",
                    null));
        }

        String location = resource.getRawLocation().toString();
        String name = manager.generateUniqueLaunchConfigurationNameFrom(resource.getName());
        String baseDirectory = new File(location).getParent();
        int resourceType = IResource.FILE;

        ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
        // Python Main Tab Arguments        
        workingCopy.setAttribute(Constants.ATTR_PROJECT, project.getName());
        workingCopy.setAttribute(Constants.ATTR_RESOURCE_TYPE, resourceType);
        workingCopy.setAttribute(Constants.ATTR_INTERPRETER, nature.getProjectInterpreter().getExecutableOrJar());
        workingCopy.setAttribute(Constants.ATTR_LOCATION, location);
        workingCopy.setAttribute(Constants.ATTR_WORKING_DIRECTORY, baseDirectory);
        workingCopy.setAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, programArguments);
        workingCopy.setAttribute(Constants.ATTR_VM_ARGUMENTS, vmargs);

        workingCopy.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
        workingCopy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);
        workingCopy.setMappedResources(new IResource[] { resource });
        return workingCopy;
    }

    private static String configurationFor(int interpreterType) {
        switch (interpreterType) {
            case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                return Constants.ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE;
            case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                return Constants.ID_JYTHON_LAUNCH_CONFIGURATION_TYPE;
            case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                return Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE;
            default:
                throw new RuntimeException("Unknown Python interpreter type");

        }
    }

}
