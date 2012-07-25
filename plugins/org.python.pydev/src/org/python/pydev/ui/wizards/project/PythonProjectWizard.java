/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.wizards.project;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.wizards.gettingstarted.AbstractNewProjectWizard;

/**
 * Python Project creation wizard
 * 
 * <ul>
 * <li>Asks users information about Python project
 * <li>Launches another thread to create Python project. A progress monitor is shown in UI thread
 * </ul>
 * 
 * TODO: Add a checkbox asking should a skeleton of a Python program generated
 * 
 * @author Mikko Ohtamaa
 * @author Fabio Zadrozny
 */
public class PythonProjectWizard extends AbstractNewProjectWizard implements IExecutableExtension {

    /**
     * The current selection.
     */
    protected IStructuredSelection selection;

    public static final String WIZARD_ID = "org.python.pydev.ui.wizards.project.PythonProjectWizard";

    protected IWizardNewProjectNameAndLocationPage projectPage;

    Shell shell;

    /** Target project created by this wizard */
    IProject generatedProject;

    /** Exception throw by generator thread */
    Exception creationThreadException;

    private IProject createdProject;

    private IConfigurationElement fConfigElement;

    protected IWorkbench workbench;

    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        this.selection = currentSelection;
        this.workbench = workbench;
        initializeDefaultPageImageDescriptor();
        projectPage = createProjectPage();
    }

    /**
     * Creates the project page.
     */
    protected IWizardNewProjectNameAndLocationPage createProjectPage() {
        return new NewProjectNameAndLocationWizardPage("Setting project properties");
    }

    /**
     * Add wizard pages to the instance
     * 
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages() {
        addPage(projectPage);
        addProjectReferencePage();
    }

    /**
     * Creates a new project resource with the entered name.
     * 
     * @return the created project resource, or <code>null</code> if the project was not created
     */
    protected IProject createNewProject(final Object... additionalArgsToConfigProject) {
        // get a project handle
        final IProject newProjectHandle = projectPage.getProjectHandle();

        // get a project descriptor
        IPath defaultPath = Platform.getLocation();
        IPath newPath = projectPage.getLocationPath();
        if (defaultPath.equals(newPath)) {
            newPath = null;
        } else {
            //The user entered the path and it's the same as it'd be if he chose the default path.
            IPath withName = defaultPath.append(newProjectHandle.getName());
            if (newPath.toFile().equals(withName.toFile())) {
                newPath = null;
            }
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
        description.setLocation(newPath);

        // update the referenced project if provided
        if (referencePage != null) {
            IProject[] refProjects = referencePage.getReferencedProjects();
            if (refProjects.length > 0) {
                description.setReferencedProjects(refProjects);
            }
        }

        final String projectType = projectPage.getProjectType();
        final String projectInterpreter = projectPage.getProjectInterpreter();
        // define the operation to create a new project
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            protected void execute(IProgressMonitor monitor) throws CoreException {

                createAndConfigProject(newProjectHandle, description, projectType, projectInterpreter, monitor,
                        additionalArgsToConfigProject);
            }
        };

        // run the operation to create a new project
        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            return null;
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof CoreException) {
                if (((CoreException) t).getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
                    MessageDialog.openError(getShell(), "Unable to create project",
                            "Another project with the same name (and different case) already exists.");
                } else {
                    ErrorDialog
                            .openError(getShell(), "Unable to create project", null, ((CoreException) t).getStatus());
                }
            } else {
                // Unexpected runtime exceptions and errors may still occur.
                Log.log(IStatus.ERROR, t.toString(), t);
                MessageDialog.openError(getShell(), "Unable to create project", t.getMessage());
            }
            return null;
        }

        return newProjectHandle;
    }

    /**
     * This method can be overridden to provide a custom creation of the project.
     * 
     * It should create the project, configure the folders in the pythonpath (source folders and external folders
     * if applicable), set the project type and project interpreter.
     */
    protected void createAndConfigProject(final IProject newProjectHandle, final IProjectDescription description,
            final String projectType, final String projectInterpreter, IProgressMonitor monitor,
            Object... additionalArgsToConfigProject) throws CoreException {
        ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback = new ICallback<List<IContainer>, IProject>() {

            public List<IContainer> call(IProject projectHandle) {
                final int sourceFolderConfigurationStyle = projectPage.getSourceFolderConfigurationStyle();
                List<IContainer> ret = new ArrayList<IContainer>();
                switch (sourceFolderConfigurationStyle) {

                    case IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER:
                        //if the user hasn't selected to create a source folder, use the project itself for that.
                        ret = new ArrayList<IContainer>();
                        ret.add(projectHandle);
                        return ret;

                    case IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_NO_PYTHONPATH:
                        return new ArrayList<IContainer>();

                    default:
                        IContainer folder = projectHandle.getFolder("src");
                        ret = new ArrayList<IContainer>();
                        ret.add(folder);
                        return ret;
                }
            }
        };
        PyStructureConfigHelpers.createPydevProject(description, newProjectHandle, monitor, projectType,
                projectInterpreter, getSourceFolderHandlesCallback, null);
    }

    /**
     * The user clicked Finish button
     * 
     * Launches another thread to create Python project. A progress monitor is shown in the UI thread.
     */
    public boolean performFinish() {
        createdProject = createNewProject();

        // Switch to default perspective (will ask before changing)
        BasicNewProjectResourceWizard.updatePerspective(fConfigElement);

        return true;
    }

    public IProject getCreatedProject() {
        return createdProject;
    }

    /**
     * Set Python logo to top bar
     */
    protected void initializeDefaultPageImageDescriptor() {
        ImageDescriptor desc = PydevPlugin
                .imageDescriptorFromPlugin(PydevPlugin.getPluginID(), "icons/python_logo.png");//$NON-NLS-1$
        setDefaultPageImageDescriptor(desc);
    }

    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
        this.fConfigElement = config;
    }
}
