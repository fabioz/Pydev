/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.debug.ui.actions;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.console.ProcessConsoleManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.internal.ide.dialogs.OpenResourceDialog;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.django.launching.DjangoConstants;
import org.python.pydev.django.launching.PythonFileRunner;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.ConsoleColorCache;
import org.python.pydev.shared_ui.EditorUtils;

/**
 * Base class for django actions.
 */
public abstract class DjangoAction implements IObjectActionDelegate {

    /**
     * The project that was selected (may be null).
     */
    protected IProject selectedProject;

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // empty
    }

    /**
     * Actually remove the python nature from the project.
     */
    public abstract void run(IAction action);

    /**
     * A project was just selected
     */
    public void selectionChanged(IAction action, ISelection selection) {
        selectedProject = null;

        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            return;
        }

        IStructuredSelection selections = (IStructuredSelection) selection;
        Object project = selections.getFirstElement();
        if (!(project instanceof IProject)) {
            return;
        }

        this.selectedProject = (IProject) project;
    }

    public void setSelectedProject(IProject selectedProject) {
        this.selectedProject = selectedProject;
    }

    /**
     * May be used to run some command that uses the manage.py file.
     */
    @SuppressWarnings("restriction")
    public ILaunch launchDjangoCommand(final String command, boolean refreshAndShowMessageOnFinish) {
        PythonNature nature = PythonNature.getPythonNature(selectedProject);
        if (nature == null) {
            MessageDialog.openError(EditorUtils.getShell(), "PyDev nature not found",
                    "Unable to perform action because the Pydev nature is not properly set.");
            return null;
        }
        IPythonPathNature pythonPathNature = nature.getPythonPathNature();
        String manageVarible = null;
        Map<String, String> variableSubstitution = null;
        try {
            variableSubstitution = pythonPathNature.getVariableSubstitution();
            manageVarible = variableSubstitution.get(DjangoConstants.DJANGO_MANAGE_VARIABLE);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
        if (manageVarible == null) {
            manageVarible = askNewManageSubstitution(pythonPathNature, variableSubstitution,
                    StringUtils.format(
                            "Unable to perform action because the %s \n" + "substitution variable is not set.\n\n"
                                    + "Please select the manage.py to be used to run the action.",
                            DjangoConstants.DJANGO_MANAGE_VARIABLE));
            if (manageVarible == null) {
                return null;
            }
        }
        IFile manageDotPy = selectedProject.getFile(manageVarible);
        if (manageDotPy == null || !manageDotPy.exists()) {
            manageVarible = askNewManageSubstitution(pythonPathNature, variableSubstitution,
                    StringUtils.format(
                            "Unable to perform action because the %s \n"
                                    + "substitution variable is set to a non existing file.\n\n"
                                    + "Please select the manage.py to be used to run the action.",
                            DjangoConstants.DJANGO_MANAGE_VARIABLE));
            if (manageVarible == null) {
                return null;
            }
            //we shouldn't need to validate again (he can't choose a wrong file there right?)
            manageDotPy = selectedProject.getFile(manageVarible);
        }
        final IFile finalManageDotPy = manageDotPy;
        try {
            ILaunch launch = PythonFileRunner.launch(manageDotPy, command);

            //After the command completes, refresh and put message for user.
            final IProcess[] processes = launch.getProcesses();
            ProcessConsoleManager consoleManager = DebugUIPlugin.getDefault().getProcessConsoleManager();
            if (processes.length >= 1) {
                IConsole console = consoleManager.getConsole(processes[0]);

                final IOConsoleOutputStream outputStream = ((IOConsole) console).newOutputStream();
                HashMap<IOConsoleOutputStream, String> themeConsoleStreamToColor = new HashMap<IOConsoleOutputStream, String>();
                themeConsoleStreamToColor.put(outputStream, "console.output");
                ((IOConsole) console).setAttribute("themeConsoleStreamToColor", themeConsoleStreamToColor);

                ConsoleColorCache.getDefault().keepConsoleColorsSynched((IOConsole) console);

                Job j = new Job("Refresh on finish") {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        boolean allTerminated = false;
                        while (!allTerminated) {
                            allTerminated = true;
                            for (IProcess p : processes) {
                                if (!p.isTerminated()) {
                                    allTerminated = false;
                                    break;
                                }
                            }
                            synchronized (this) {
                                try {
                                    this.wait(50);
                                } catch (InterruptedException e) {
                                }
                            }

                        }
                        try {
                            outputStream.write(StringUtils.format("Finished \""
                                    + finalManageDotPy.getLocation().toOSString() + " " + command + "\" execution."));
                        } catch (IOException e1) {
                            Log.log(e1);
                        }

                        try {
                            outputStream.close();
                        } catch (IOException e1) {
                            Log.log(e1);
                        }
                        try {
                            selectedProject.refreshLocal(IResource.DEPTH_INFINITE, null);
                        } catch (CoreException e) {
                            Log.log(e);
                        }

                        return Status.OK_STATUS;
                    }
                };
                j.setSystem(true);
                j.schedule();
            }
            return launch;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Asks the user to select a new manage.py file and saves that selection.
     */
    private String askNewManageSubstitution(IPythonPathNature pythonPathNature,
            Map<String, String> variableSubstitution, String message) {
        String manageVarible = null;
        OpenResourceDialog manageSelectionDialog = createManageSelectionDialog(message);
        if (manageSelectionDialog.open() == OpenResourceDialog.OK) {
            Object firstResult = manageSelectionDialog.getFirstResult();
            if (firstResult instanceof IFile) {
                IFile iFile = (IFile) firstResult;
                IPath projectRelativePath = iFile.getProjectRelativePath();
                manageVarible = projectRelativePath.toPortableString();
                variableSubstitution.put(DjangoConstants.DJANGO_MANAGE_VARIABLE, manageVarible);
                try {
                    pythonPathNature.setVariableSubstitution(variableSubstitution);
                } catch (Exception e) {
                    Log.log(e);
                }

            } else {
                Log.log("Error. Expected IFile selected. Found: " + firstResult.getClass());
                return null;
            }

        } else { //dialog cancelled
            return null;
        }
        return manageVarible;
    }

    private OpenResourceDialog createManageSelectionDialog(String message) {
        OpenResourceDialog resourceDialog = new OpenResourceDialog(EditorUtils.getShell(), selectedProject,
                IResource.FILE);
        try {
            //Hack warning: changing the multi internal field to false because we don't want a multiple selection
            //(but the OpenResourceDialog didn't make available an API to change that -- even though
            //it'd be possible to create a FilteredItemsSelectionDialog in single selection mode)
            Field field = FilteredItemsSelectionDialog.class.getDeclaredField("multi");
            field.setAccessible(true);
            field.set(resourceDialog, false);
        } catch (Throwable e) {
            //just ignore any error here
        }
        resourceDialog.setInitialPattern("manage.py");
        resourceDialog.setMessage(message);
        return resourceDialog;
    }
}
