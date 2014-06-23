/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * A utility class that creates new {@link ILaunchConfiguration}s.
 *
 * @author radimkubacki@google.com (Radim Kubacki)
 */
public abstract class LaunchConfigurationCreator {

    /**
     * Builds a value of a location attribute used in launch configurations.
     * 
     * @param file an array of resources
     * @param makeRelative {@code true} to produce path relative to workspace location
     * @return default string for the location field
     */
    public static String getDefaultLocation(FileOrResource[] file, boolean makeRelative) {
        StringBuffer buffer = new StringBuffer();

        for (FileOrResource r : file) {
            if (buffer.length() > 0) {
                buffer.append('|');
            }

            String loc;
            if (r.resource != null) {

                if (makeRelative) {
                    IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
                    loc = makeFileRelativeToWorkspace(r.resource, varManager);
                } else {
                    loc = r.resource.getLocation().toOSString();
                }
            } else {
                loc = FileUtils.getFileAbsolutePath(r.file.getAbsolutePath());
            }
            buffer.append(loc);
        }
        return buffer.toString();
    }

    public static ILaunchConfigurationWorkingCopy createDefaultLaunchConfiguration(FileOrResource[] resource,
            String launchConfigurationType, String location, IInterpreterManager pythonInterpreterManager,
            String projName) throws CoreException {
        return createDefaultLaunchConfiguration(resource, launchConfigurationType, location, pythonInterpreterManager,
                projName, null, "", true);
    }

    /**
     * @param resource only used if captureOutput is true!
     * @param location only used if captureOutput is false!
     * @param captureOutput determines if the output should be captured or not (if captured a console will be
     * shown to it by default)
     */
    private static ILaunchConfigurationWorkingCopy createDefaultLaunchConfiguration(FileOrResource[] resource,
            String launchConfigurationType, String location, IInterpreterManager pythonInterpreterManager,
            String projName, String vmargs, String programArguments, boolean captureOutput) throws CoreException {

        ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(launchConfigurationType);
        if (type == null) {
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Python launch configuration not found",
                    null));
        }

        IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();

        String name;
        String baseDirectory;
        String moduleFile;
        int resourceType;

        if (captureOutput) {
            StringBuffer buffer = new StringBuffer(projName);
            buffer.append(" ");
            StringBuffer resourceNames = new StringBuffer();
            for (FileOrResource r : resource) {
                if (resourceNames.length() > 0) {
                    resourceNames.append(" - ");
                }
                if (r.resource != null) {
                    resourceNames.append(r.resource.getName());
                } else {
                    resourceNames.append(r.file.getName());
                }
            }
            buffer.append(resourceNames);
            name = buffer.toString().trim();

            if (resource[0].resource != null) {
                // Build the working directory to a path relative to the workspace_loc
                baseDirectory = resource[0].resource.getFullPath().removeLastSegments(1).makeRelative().toString();
                baseDirectory = varManager.generateVariableExpression("workspace_loc", baseDirectory);

                // Build the location to a path relative to the workspace_loc
                moduleFile = makeFileRelativeToWorkspace(resource, varManager);
                resourceType = resource[0].resource.getType();
            } else {
                baseDirectory = FileUtils.getFileAbsolutePath(resource[0].file.getParentFile());

                // Build the location to a path relative to the workspace_loc
                moduleFile = FileUtils.getFileAbsolutePath(resource[0].file);
                resourceType = IResource.FILE;
            }
        } else {
            captureOutput = true;
            name = location;
            baseDirectory = new File(location).getParent();
            moduleFile = location;
            resourceType = IResource.FILE;
        }

        name = manager.generateLaunchConfigurationName(name);

        ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
        // Python Main Tab Arguments

        workingCopy.setAttribute(Constants.ATTR_PROJECT, projName);
        workingCopy.setAttribute(Constants.ATTR_RESOURCE_TYPE, resourceType);
        workingCopy.setAttribute(Constants.ATTR_INTERPRETER, Constants.ATTR_INTERPRETER_DEFAULT);

        workingCopy.setAttribute(Constants.ATTR_LOCATION, moduleFile);
        workingCopy.setAttribute(Constants.ATTR_WORKING_DIRECTORY, baseDirectory);
        workingCopy.setAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, programArguments);
        workingCopy.setAttribute(Constants.ATTR_VM_ARGUMENTS, vmargs);

        workingCopy.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, captureOutput);
        workingCopy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, captureOutput);

        if (resource[0].resource != null) {
            workingCopy.setMappedResources(FileOrResource.createIResourceArray(resource));
        }
        return workingCopy;
    }

    private static String makeFileRelativeToWorkspace(FileOrResource[] resource, IStringVariableManager varManager) {
        FastStringBuffer moduleFile = new FastStringBuffer(80 * resource.length);
        for (FileOrResource r : resource) {
            if (moduleFile.length() > 0) {
                moduleFile.append("|");
            }

            if (r.resource != null) {
                moduleFile.append(makeFileRelativeToWorkspace(r.resource, varManager));
            } else {
                moduleFile.append(FileUtils.getFileAbsolutePath(r.file));
            }
        }
        return moduleFile.toString();
    }

    private static String makeFileRelativeToWorkspace(IResource r, IStringVariableManager varManager) {
        String m = r.getFullPath().makeRelative().toString();
        m = varManager.generateVariableExpression("workspace_loc", m);
        return m;
    }
}
