package com.leosoto.bingo.ui.wizards.project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.perspective.PythonPerspectiveFactory;
import org.python.pydev.ui.wizards.project.IWizardNewProjectNameAndLocationPage;
import org.python.pydev.ui.wizards.project.PythonProjectWizard;

import com.leosoto.bingo.UniversalRunner;
import com.leosoto.bingo.plugin.nature.DjangoNature;
import com.leosoto.bingo.ui.wizards.project.DjangoSettingsPage.DjangoSettings;

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
public class DjangoProjectWizard extends PythonProjectWizard {

	public static final String WIZARD_ID = "org.python.pydev.ui.wizards.project.DjangoProjectWizard";

	protected DjangoSettingsPage settingsPage;

	protected static final String RUN_DJANGO_ADMIN =
		"from django.core import management\n" +
		"management.execute_from_command_line()";

	public DjangoProjectWizard() {
		super();
		settingsPage = createDjangoSettingsPage();
	}

	@Override
    protected IWizardNewProjectNameAndLocationPage createProjectPage(){
        return new DjangoNewProjectPage("Setting project properties");
    }

    protected DjangoSettingsPage createDjangoSettingsPage()  {
    	return new DjangoSettingsPage("Django Settings");
    }


    /**
     * Add wizard pages to the instance
     *
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
    	super.addPages();
    	addPage(settingsPage);
    }

    /**
     * Creates a project resource given the project handle and description.
     *
     * @param description the project description to create a project resource for
     * @param projectHandle the project handle to create a project resource for
     * @param monitor the progress monitor to show visual progress with
     * @param projectType
     * @param projectInterpreter
     *
     * @exception CoreException if the operation fails
     * @exception OperationCanceledException if the operation is canceled
     * @throws IOException
     */
    private void createDjangoProject(
    		IProjectDescription description, IProject projectHandle,
    		IProgressMonitor monitor, String projectType, String projectInterpreter,
    		DjangoSettingsPage.DjangoSettings djSettings)
    throws CoreException, OperationCanceledException, IOException {

        try {
            monitor.beginTask("", 2000); //$NON-NLS-1$

            projectHandle.create(description, new SubProgressMonitor(monitor, 1000));

            if (monitor.isCanceled()){
                throw new OperationCanceledException();
            }

            projectHandle.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));


            String projectPythonpath = projectHandle.getFullPath().toString(); // Project root is added to the pythonpath

            //we should rebuild the path even if there's no source-folder (this way we will re-create the astmanager)
            IPythonNature n = PythonNature.addNature(
            		projectHandle, null, projectType,
            		projectPythonpath, null, projectInterpreter, null);
            DjangoNature.addNature(projectHandle, null);

            while (n.getAstManager() == null); // Wait for the AST manager --
                                               // otherwise, we won't have a
                                               // pythonpath available to run
                                               // the django admin.
            UniversalRunner.runCodeAndGetOutput(
            		projectHandle,
            		RUN_DJANGO_ADMIN,
            		new String[]{"startproject", projectHandle.getName()},
            		projectHandle.getLocation().toFile(),
            		new NullProgressMonitor());

            IFile settingsFile = projectHandle.getFile(new Path(
            		projectHandle.getName() + "/settings.py"));

            String settings = readFile(settingsFile);
            settings = settings.replaceFirst(
            		"DATABASE_ENGINE = ''",
				    "DATABASE_ENGINE = '" + djSettings.databaseEngine + "'");
            settings = settings.replaceFirst(
            		"DATABASE_NAME = ''",
				    "DATABASE_NAME = '" + djSettings.databaseName + "'");
            settings = settings.replaceFirst(
            		"DATABASE_HOST = ''",
				    "DATABASE_HOST = '" + djSettings.databaseHost + "'");
            settings = settings.replaceFirst(
            		"DATABASE_PORT = ''",
				    "DATABASE_PORT = '" + djSettings.databasePort + "'");
            settings = settings.replaceFirst(
            		"DATABASE_USER = ''",
				    "DATABASE_USER = '" + djSettings.databaseUser + "'");
            settings = settings.replaceFirst(
            		"DATABASE_PASSWORD = ''",
				    "DATABASE_PASSWORD = '" + djSettings.databasePassword + "'");
            writeFile(settingsFile, settings);

            // TODO: Open settings.py in an editor

        } finally {
            monitor.done();
        }
    }

    private String readFile(IFile f) throws IOException {
    	return readFile(f.getLocation().toOSString());
    }

    private String readFile(String filePath) throws java.io.IOException{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            fileData.append(buf, 0, numRead);
        }
        reader.close();
        return fileData.toString();
    }

    private void writeFile(IFile f, String contents) throws IOException {
    	BufferedWriter out = new BufferedWriter(new FileWriter(
    			f.getLocation().toOSString()));
    	out.write(contents);
    	out.close();
    }

    /**
     * Creates a new project resource with the entered name.
     *
     * @return the created project resource, or <code>null</code> if the project was not created
     */


    @Override
    protected IProject createNewProject() {
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

        final String projectType = projectPage.getProjectType();
        final String projectInterpreter = projectPage.getProjectInterpreter();
        final DjangoSettings djSettings =
        	settingsPage.getSettings(
        			projectType.startsWith("jython") ? DjangoSettingsPage.JYTHON :
        					                           DjangoSettingsPage.CPYTHON);
        // define the operation to create a new project
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
            protected void execute(IProgressMonitor monitor) throws CoreException {
            	try {
            		createDjangoProject(description, newProjectHandle, monitor,
            				      projectType, projectInterpreter, djSettings);
            	} catch(IOException e) {
            		throw new RuntimeException(e);
            	}
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
    @Override
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
     * Set Django logo to top bar
     */
    protected void initializeDefaultPageImageDescriptor() {
        ImageDescriptor desc = PydevPlugin.imageDescriptorFromPlugin(PydevPlugin.getPluginID(), "icons/django_logo.png");//$NON-NLS-1$
        setDefaultPageImageDescriptor(desc);
    }
}
