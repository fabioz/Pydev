package org.python.pydev.ui.wizards.project;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectReferencePage;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.NotConfiguredInterpreterException;
import org.python.pydev.ui.perspective.PythonPerspectiveFactory;

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
 */
public class PythonProjectWizard extends Wizard implements INewWizard {

    /**
     * The workbench.
     */
    private IWorkbench workbench;

    /**
     * The current selection.
     */
    protected IStructuredSelection selection;

    public static final String WIZARD_ID = "org.python.pydev.ui.wizards.project.PythonProjectWizard";

    WelcomePage welcomePage = new WelcomePage("Creating a new modelled Python Project");

    CopiedWizardNewProjectNameAndLocationPage projectPage = new CopiedWizardNewProjectNameAndLocationPage("Setting project properties");

    WizardNewProjectReferencePage referencePage;

    Shell shell;

    /** Target project created by this wizard */
    IProject generatedProject;

    /** Exception throw by generator thread */
    Exception creationThreadException;

    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        this.workbench = workbench;
        this.selection = currentSelection;
        initializeDefaultPageImageDescriptor();
    }

    /**
     * Add wizard pages to the instance
     * 
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages() {

        if (!isInterpreterConfigured()) {
            addPage(welcomePage);
        }
        addPage(projectPage);
        // only add page if there are already projects in the workspace
        if (ResourcesPlugin.getWorkspace().getRoot().getProjects().length > 0) {
            referencePage = new WizardNewProjectReferencePage("Reference Page");
            referencePage.setTitle("Reference page");
            referencePage.setDescription("Select referenced projects");
            this.addPage(referencePage);
        }

    }

    /**
     * Creates a project resource given the project handle and description.
     * 
     * @param description the project description to create a project resource for
     * @param projectHandle the project handle to create a project resource for
     * @param monitor the progress monitor to show visual progress with
     * @param projectType
     * 
     * @exception CoreException if the operation fails
     * @exception OperationCanceledException if the operation is canceled
     */
    private void createProject(IProjectDescription description, IProject projectHandle, IProgressMonitor monitor, String projectType) throws CoreException, OperationCanceledException {
        try {
            monitor.beginTask("", 2000); //$NON-NLS-1$

            projectHandle.create(description, new SubProgressMonitor(monitor, 1000));

            if (monitor.isCanceled()){
                throw new OperationCanceledException();
            }

            projectHandle.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));
            IPythonNature nature = PythonNature.addNature(projectHandle, null);
            nature.setVersion(projectType);

            //also, after creating the project, create a default source folder and add it to the pythonpath.
            if(projectPage.shouldCreatSourceFolder()){
                IFolder folder = projectHandle.getFolder("src");
                folder.create(true, true, monitor);
            
                nature.getPythonPathNature().setProjectSourcePath(folder.getFullPath().toString());
            }
            //we should rebuild the path even if there's no source-folder (this way we will re-create the astmanager)
            nature.rebuildPath();
        } finally {
            monitor.done();
        }
    }

    public String getProjectType() {
        return projectPage.getProjectType();
    }

    /**
     * Creates a new project resource with the entered name.
     * 
     * @return the created project resource, or <code>null</code> if the project was not created
     */
    private IProject createNewProject() {
        // get a project handle
        final IProject newProjectHandle = projectPage.getProjectHandle();

        // get a project descriptor
        IPath defaultPath = Platform.getLocation();
        IPath newPath = projectPage.getLocationPath();
        if (defaultPath.equals(newPath)){
            newPath = null;
        }
        
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
        description.setLocation(newPath);

        // update the referenced project if provided
        if (referencePage != null) {
            IProject[] refProjects = referencePage.getReferencedProjects();
            if (refProjects.length > 0)
                description.setReferencedProjects(refProjects);
        }

        final String projectType = getProjectType();
        // define the operation to create a new project
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            protected void execute(IProgressMonitor monitor) throws CoreException {
                createProject(description, newProjectHandle, monitor, projectType);
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
                    MessageDialog.openError(getShell(), "IDEWorkbenchMessages.CreateProjectWizard_errorTitle", "IDEWorkbenchMessages.CreateProjectWizard_caseVariantExistsError");
                } else {
                    ErrorDialog.openError(getShell(), "IDEWorkbenchMessages.CreateProjectWizard_errorTitle", null, ((CoreException) t).getStatus());
                }
            } else {
                // Unexpected runtime exceptions and errors may still occur.
                PydevPlugin.log(IStatus.ERROR, t.toString(), t);
                MessageDialog.openError(getShell(), "IDEWorkbenchMessages.CreateProjectWizard_errorTitle", t.getMessage());
            }
            return null;
        }

        return newProjectHandle;
    }

    /**
     * The user clicked Finish button
     * 
     * Launches another thread to create Python project. A progress monitor is shown in the UI thread.
     */
    public boolean performFinish() {
        createNewProject();

        // Switch to default perspective
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

        try {
            workbench.showPerspective(PythonPerspectiveFactory.PERSPECTIVE_ID, window);
        } catch (WorkbenchException we) {
            we.printStackTrace();
        }

        // TODO: If initial program skeleton is generated, open default file
        /*
         * if(generatedProject != null) { IFile defaultFile = generatedProject.getFile(new Path("__init__.py")); try { window.getActivePage().openEditor(new FileEditorInput(defaultFile),
         * PyDevPlugin.EDITOR_ID); } catch(CoreException ce) { ce.printStackTrace(); } }
         */

        return true;
    }

    /**
     * Set Python logo to top bar
     */
    protected void initializeDefaultPageImageDescriptor() {
        ImageDescriptor desc = PydevPlugin.imageDescriptorFromPlugin(PydevPlugin.getPluginID(), "icons/python_logo.png");//$NON-NLS-1$
        setDefaultPageImageDescriptor(desc);
    }

    /**
     * Check thats user has set an Python interpreter in Preferences
     * 
     * @return
     */
    boolean isInterpreterConfigured() {
        try {
            PydevPlugin.getPythonInterpreterManager().getDefaultInterpreter();
        } catch (NotConfiguredInterpreterException ncie) {
            try {
                PydevPlugin.getJythonInterpreterManager().getDefaultInterpreter();
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

}
