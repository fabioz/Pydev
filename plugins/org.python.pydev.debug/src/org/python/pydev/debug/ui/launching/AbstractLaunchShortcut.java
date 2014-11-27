/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Aug 26, 2003
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.StatusInfo;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.dialogs.ProjectSelectionDialog;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.dialogs.PythonModulePickerDialog;

/**
 * Called when "Run Script..." popup menu item is selected.
 * 
 * <p>Manages configurations (store/load/default)
 * <p>Launches the "Run python..." window
 * <p>code almost all copied from AntLaunchShortcut:
 * Based on org.eclipse.ui.externaltools.internal.ant.launchConfigurations.AntLaunchShortcut
 */
public abstract class AbstractLaunchShortcut implements ILaunchShortcut {

    //=============================================================================================
    // ILaunchShortcut IMPL
    //=============================================================================================
    @SuppressWarnings("unchecked")
    public void launch(ISelection selection, String mode) {
        boolean requireFile = getRequireFile();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;

            //single selection
            if (structuredSelection.size() == 1) {
                Object object = structuredSelection.getFirstElement();
                if (object instanceof IAdaptable) {

                    IResource resource = (IFile) ((IAdaptable) object).getAdapter(IFile.class);
                    if (resource != null) {
                        launch(new FileOrResource(resource), mode);
                        return;
                    }

                    IContainer folder = (IContainer) ((IAdaptable) object).getAdapter(IContainer.class);
                    if (folder != null) {

                        String launchConfigurationType = this.getLaunchConfigurationType();
                        if (launchConfigurationType.equals(Constants.ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE)
                                || launchConfigurationType
                                        .equals(Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE)
                                || launchConfigurationType.equals(Constants.ID_JYTHON_LAUNCH_CONFIGURATION_TYPE)) {
                            //I.e.: on a regular run, we can run if we have a __main__.py (otherwise, if it's a folder
                            //for these configurations we must skip it).
                            IResource mainMember = folder.findMember("__main__.py");
                            if (mainMember != null && mainMember.exists()) {
                                launch(new FileOrResource(folder), mode);
                                return;
                            }
                        }

                        if (requireFile) {
                            if (folder instanceof IProject) {
                                Shell parent = EditorUtils.getShell();
                                PythonModulePickerDialog dialog = new PythonModulePickerDialog(parent,
                                        "Select python file", "Select the python file to be launched.",
                                        (IProject) folder, false); //this is for single selection, so never running a Unittest
                                int result = dialog.open();
                                if (result == PythonModulePickerDialog.OK) {
                                    Object results[] = dialog.getResult();
                                    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
                                        resource = (IResource) results[0];
                                    }
                                }
                            }

                        } else {
                            resource = folder;
                        }

                        if (resource != null) {
                            launch(new FileOrResource(resource), mode);
                        }
                        return;
                    }
                }

                //multiple selection
            } else if (structuredSelection.size() > 1) {

                //for multiple selection, we must accept folders or files!
                Assert.isTrue(!requireFile);

                List<IResource> sel = new ArrayList<IResource>();
                for (Iterator<Object> it = structuredSelection.iterator(); it.hasNext();) {
                    Object object = it.next();
                    if (object instanceof IAdaptable) {
                        IContainer folder = (IContainer) ((IAdaptable) object).getAdapter(IContainer.class);
                        if (folder != null) {
                            sel.add(folder);
                        } else {
                            IFile file = (IFile) ((IAdaptable) object).getAdapter(IFile.class);
                            if (file != null) {
                                sel.add(file);
                            }
                        }
                    }
                }
                if (sel.size() > 0) {
                    launch(FileOrResource.createArray(sel.toArray(new IResource[sel.size()])), mode);
                }
                return;
            }
            StringBuffer buf = new StringBuffer();
            for (Iterator<Object> it = structuredSelection.iterator(); it.hasNext();) {
                buf.append(it.next());
            }
            reportError("Unable to discover launch config for: " + buf, null);
            return;
        } else {
            Log.log("Expecting instance of IStructuredSelection. Received: " + selection.getClass().getName());
        }

    }

    /**
     * Subclasses can reimplement to signal that they only work with files (so, if a container is selected,
     * the user will be asked to choose a file contained in that container)
     * 
     * @return true if the launch configuration requires files (and does not work with containers) and false otherwise.
     */
    protected boolean getRequireFile() {
        return false;
    }

    /**
     * Launch for a selected editor.
     */
    public void launch(IEditorPart editor, String mode) {
        //we have an editor to run
        IEditorInput input = editor.getEditorInput();
        IFile file = (IFile) input.getAdapter(IFile.class);
        if (file != null) {
            launch(new FileOrResource(file), mode);
            return;
        }

        if (editor instanceof PyEdit) {
            PyEdit pyEdit = (PyEdit) editor;
            File editorFile = pyEdit.getEditorFile();
            if (editorFile != null) {
                launch(new FileOrResource(editorFile), mode);
                return;
            }
        }
        fileNotFound();
    }

    //=============================================================================================
    // END ILaunchShortcut IMPL
    //=============================================================================================

    public void fileNotFound() {
        String msg = "Unable to launch the file. " + "Possible reasons may include:\n"
                + "    - the launch was cancelled;\n"
                + "    - the file (editor) being launched is not under a project in the workspace;\n"
                + "    - the file was deleted.";
        reportError(msg, null);
    }

    /**
     * Report some error to the user.
     */
    protected static void reportError(String message, Throwable throwable) {
        if (message == null) {
            message = "Unexpected error";
        }

        IStatus status = null;
        if (throwable instanceof CoreException) {
            status = ((CoreException) throwable).getStatus();
        } else {
            status = new Status(IStatus.ERROR, "org.python.pydev.debug", 0, message, throwable);
        }
        ErrorDialog.openError(PydevDebugPlugin.getActiveWorkbenchWindow().getShell(), "Python pydev.debug error",
                "Python launch failed", status);
    }

    /**
     * COPIED/MODIFIED from AntLaunchShortcut
     * Returns a list of existing launch configuration for the given file.
     */
    protected List<ILaunchConfiguration> findExistingLaunchConfigurations(FileOrResource[] file) {
        ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(getLaunchConfigurationType());
        List<ILaunchConfiguration> validConfigs = new ArrayList<ILaunchConfiguration>();
        if (type == null) {
            return validConfigs;
        }

        try {
            ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);

            //let's see if we can find it with a location relative or not.
            String defaultLocation = LaunchConfigurationCreator.getDefaultLocation(file, true);
            String defaultLocation2 = LaunchConfigurationCreator.getDefaultLocation(file, false);

            for (int i = 0; i < configs.length; i++) {
                String configPath = configs[i].getAttribute(Constants.ATTR_LOCATION, "");
                if (defaultLocation.equals(configPath) || defaultLocation2.equals(configPath)) {
                    validConfigs.add(configs[i]);
                }
            }
        } catch (CoreException e) {
            reportError("Unexpected error", e);
        }
        return validConfigs;
    }

    /**
     * @return a string with the launch configuration type that should be used for the run.
     */
    protected abstract String getLaunchConfigurationType();

    public ILaunchConfiguration createDefaultLaunchConfiguration(FileOrResource[] resource) {
        try {
            ILaunchConfigurationWorkingCopy createdConfiguration = createDefaultLaunchConfigurationWithoutSaving(resource);
            if (createdConfiguration == null) {
                return null;
            }
            return createdConfiguration.doSave();
        } catch (CoreException e) {
            reportError(null, e);
            return null;
        }
    }

    public ILaunchConfigurationWorkingCopy createDefaultLaunchConfigurationWithoutSaving(FileOrResource[] resource)
            throws CoreException {
        IProject project;
        if (resource[0].resource != null) {
            project = resource[0].resource.getProject();
        } else {
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            List<IProject> projectsLst = ArrayUtils.filter(projects, new ICallback<Boolean, IProject>() {

                @Override
                public Boolean call(IProject arg) {
                    IProject project = arg;
                    try {
                        return project.isOpen() && project.hasNature(PythonNature.PYTHON_NATURE_ID);
                    } catch (CoreException e) {
                        return false;
                    }
                }
            });
            final Object[] found = new Object[1];
            if (projectsLst.size() == 0) {
                found[0] = new CoreException(new StatusInfo(IStatus.ERROR,
                        "Found no projects  with the Python nature in the workspace."));
            } else if (projectsLst.size() == 1) {
                found[0] = projectsLst.get(0);
            } else {
                RunInUiThread.sync(new Runnable() {

                    public void run() {
                        ProjectSelectionDialog dialog = new ProjectSelectionDialog(EditorUtils.getShell(),
                                PythonNature.PYTHON_NATURE_ID);
                        dialog.setMessage("Choose the project that'll provide the interpreter and\n"
                                + "PYTHONPATH to be used in the launch of the file.");
                        if (dialog.open() == Window.OK) {
                            Object firstResult = dialog.getFirstResult();
                            if (firstResult instanceof IProject) {
                                found[0] = firstResult;
                            } else {
                                found[0] = new CoreException(new StatusInfo(IStatus.ERROR,
                                        "Expected project to be selected."));
                            }
                        }
                    }
                });
            }

            if (found[0] == null) {
                return null;
            }
            if (found[0] instanceof IProject) {
                project = (IProject) found[0];
            } else {
                if (found[0] instanceof CoreException) {
                    throw (CoreException) found[0];
                } else {
                    throw new CoreException(new StatusInfo(IStatus.ERROR, "Expected project, found: " + found[0]));
                }
            }
        }
        IInterpreterManager pythonInterpreterManager = getInterpreterManager(project);
        String projName = project.getName();
        ILaunchConfigurationWorkingCopy createdConfiguration = LaunchConfigurationCreator
                .createDefaultLaunchConfiguration(resource, getLaunchConfigurationType(),
                        LaunchConfigurationCreator.getDefaultLocation(resource, false), //it'll be made relative later on
                        pythonInterpreterManager, projName);

        // Common Tab Arguments
        CommonTab tab = new CommonTab();
        tab.setDefaults(createdConfiguration);
        tab.dispose();
        return createdConfiguration;
    }

    /**
     * @param project 
     * @return the interpreter manager associated with this shortcut (may be overridden if it is not python)
     */
    protected abstract IInterpreterManager getInterpreterManager(IProject project);

    /**
     * COPIED/MODIFIED from AntLaunchShortcut
     */
    protected ILaunchConfiguration chooseConfig(List<ILaunchConfiguration> configs) {
        if (configs.isEmpty()) {
            return null;
        }
        ILabelProvider labelProvider = DebugUITools.newDebugModelPresentation();
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(Display.getDefault().getActiveShell(),
                labelProvider);
        dialog.setElements(configs.toArray(new ILaunchConfiguration[configs.size()]));
        dialog.setTitle("Pick a Python configuration");
        dialog.setMessage("Choose a python configuration to run");
        dialog.setMultipleSelection(false);
        int result = dialog.open();
        labelProvider.dispose();
        if (result == Window.OK) {
            return (ILaunchConfiguration) dialog.getFirstResult();
        } else {
            return null;
        }
    }

    protected void launch(FileOrResource file, String mode) {
        launch(new FileOrResource[] { file }, mode);
    }

    /**
     * Launch the given targets in the given build file. The targets are
     * launched in the given mode.
     * 
     * @param resources the resources to launch
     * @param mode the mode in which the file should be executed
     */
    protected void launch(FileOrResource[] resources, String mode) {
        ILaunchConfiguration conf = null;
        List<ILaunchConfiguration> configurations = findExistingLaunchConfigurations(resources);
        if (configurations.isEmpty()) {
            conf = createDefaultLaunchConfiguration(resources);
        } else {
            if (configurations.size() == 1) {
                conf = configurations.get(0);
            } else {
                conf = chooseConfig(configurations);
                if (conf == null) {
                    // User canceled selection
                    return;
                }
            }
        }

        if (conf != null) {
            DebugUITools.launch(conf, mode);
            return;
        }
        fileNotFound();
    }

}
