/*
 * License: Common Public License v1.0
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
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;

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
    public static String getDefaultLocation(IResource[] file, boolean makeRelative) {
        StringBuffer buffer = new StringBuffer();

        for (IResource r : file) {
            if (buffer.length() > 0) {
                buffer.append('|');
            }

            String loc;

            if (makeRelative) {
                IStringVariableManager varManager = VariablesPlugin.getDefault().getStringVariableManager();
                loc = makeFileRelativeToWorkspace(file, varManager);
            } else {
                loc = r.getLocation().toOSString();
            }
            buffer.append(loc);
        }
        return buffer.toString();
    }

    public static ILaunchConfigurationWorkingCopy createDefaultLaunchConfiguration(IResource[] resource,
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
    private static ILaunchConfigurationWorkingCopy createDefaultLaunchConfiguration(IResource[] resource,
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
            for (IResource r : resource) {
                if (resourceNames.length() > 0) {
                    resourceNames.append(" - ");
                }
                resourceNames.append(r.getName());
            }
            buffer.append(resourceNames);
            name = buffer.toString().trim();

            // Build the working directory to a path relative to the workspace_loc
            baseDirectory = resource[0].getFullPath().removeLastSegments(1).makeRelative().toString();
            baseDirectory = varManager.generateVariableExpression("workspace_loc", baseDirectory);

            // Build the location to a path relative to the workspace_loc
            moduleFile = makeFileRelativeToWorkspace(resource, varManager);
            resourceType = resource[0].getType();
        } else {
            captureOutput = true;
            name = location;
            baseDirectory = new File(location).getParent();
            moduleFile = location;
            resourceType = IResource.FILE;
        }

        name = manager.generateUniqueLaunchConfigurationNameFrom(name);

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

        workingCopy.setMappedResources(resource);
        return workingCopy;
    }

    private static String makeFileRelativeToWorkspace(IResource[] resource, IStringVariableManager varManager) {
        FastStringBuffer moduleFile = new FastStringBuffer(80 * resource.length);
        for (IResource r : resource) {
            String m = r.getFullPath().makeRelative().toString();
            m = varManager.generateVariableExpression("workspace_loc", m);
            if (moduleFile.length() > 0) {
                moduleFile.append("|");
            }
            moduleFile.append(m);
        }
        return moduleFile.toString();
    }
}
