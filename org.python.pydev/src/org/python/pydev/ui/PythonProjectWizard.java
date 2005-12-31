package org.python.pydev.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.project.PythonProjectGenerator;

/**
 * Python Project creation wizard
 * 
 * <ul>
 * <li>Asks users information about Python project
 * <li>Launches another thread to create Python project. A progress monitor is shown in
 * UI thread
 * </ul>
 * 
 * TODO: Add a checkbox asking should a skeleton of a Python program generated
 * 
 * TODO: Do not subclass BasicNewResourceWizard as its documentation discourages subclassing
 * 
 * @author Mikko Ohtamaa
 */
public class PythonProjectWizard extends BasicNewResourceWizard {
    
    public static final String WIZARD_ID = "org.python.pydev.ui.PythonProjectWizard";
    
    WelcomePage welcomePage = new WelcomePage("Creating a new modelled Python Project");
    
    ProjectPropertiesPage projectPage = new ProjectPropertiesPage("Setting project properties");
            
    Shell shell;
        
    /** Target project created by this wizard */
    IProject generatedProject;
    
    /** Exception throw by generator thread */
    Exception creationThreadException;

        
    /**
     * Add wizard pages to the instance
     * 
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages() {  
    	
    	if(!isInterpreterConfigured()) {
    		addPage(welcomePage);
    	}
    	addPage(projectPage);
    }
   
    /**
     * The user clicked Finish button
     * 
     * Launches another thread to create Python project. 
     * A progress monitor is shown in the UI thread.
     */
    public boolean performFinish() {
                		             
        final String projectName = projectPage.projectName.getText();            
        
        final PythonProjectGenerator generator = new PythonProjectGenerator();
        	
		shell = Display.getCurrent().getActiveShell();
                        	                
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
                        
	    try {
    	    dialog.run(true, true, new IRunnableWithProgress() {
    	        public void run(final IProgressMonitor monitor) throws InvocationTargetException {
    	          try {
    	        	  monitor.beginTask("Creating a Python project", 3);
    	              generatedProject = generator.createProject(projectName, monitor);
    	              monitor.done();
    	          } catch (Exception e) {    	              	
    	        	  creationThreadException = e;
    	          }
    	        }
    	      });
	    } catch(InterruptedException ie) {
	        // TODO: What if the user cancels the operation
	    } catch(InvocationTargetException ite) {
	    	// TODO: Handle invocation exceptions gracefully
	    	handleException(ite);
	    }
	    	    
	    if(creationThreadException != null) {
	    	// An exception was throw in a project construction thread
	    	// Forward this exception to UI thread
	    	// TODO: Handle gracefully
	    	handleException(creationThreadException);
	    }
	    
	    // Switch to default perspective
	    IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
	    
	    try {
	        getWorkbench().showPerspective(PythonPerspectiveFactory.PERSPECTIVE_ID, window);
	    } catch(WorkbenchException we) {
	        we.printStackTrace();
	    }
	    
	    // TODO: If initial program skeleton is generated, open default file
	    /*
	    if(generatedProject != null) {
	        IFile defaultFile = generatedProject.getFile(new Path("__init__.py"));
	        try {
	            window.getActivePage().openEditor(new FileEditorInput(defaultFile), PyDevPlugin.EDITOR_ID);
	        } catch(CoreException ce) {
	            ce.printStackTrace();
	        }
	    }*/
            
        return true;
    }
    
    
    /**
     * Graceful exception handler
     * 
     * Try produce a nice error dialog to a user 
     * 
     * @param e
     */
    protected void handleException(Throwable e) {
    	        
    	// ErrorDialog takes status objects as a parameter
        IStatus status;
            	                  	              
        if(e instanceof CoreException) {
        	// CoreException has internal status object
            CoreException ce = (CoreException) e;
            status = ce.getStatus();
        } else  {
        	
        	// Generate our own status objetc
            
            // new Status() causes IllegalArgumentException if message is null
            String message = e.getMessage();
            if(message == null) message = e.toString();
            
            status = new Status(
                    IStatus.ERROR, 
                    PydevPlugin.getPluginID(),
                    0,
                    message,
                    e);
        }
        
        ErrorDialog.openError(
                null,
                "Project creation failed",
                "An exception occured",
                status, 
                IStatus.ERROR                
                );                
         
    }
    
        

    /**
     * Set Python logo to top bar
     */
    protected void initializeDefaultPageImageDescriptor() {             	    	
    	ImageDescriptor desc = PydevPlugin.imageDescriptorFromPlugin(PydevPlugin.getPluginID(), "icons/python-logo.png");//$NON-NLS-1$
        setDefaultPageImageDescriptor(desc);    	
    }
    
    /**
     * Check thats user has set an Python interpreter in Preferences 
     * @return
     */
    boolean isInterpreterConfigured() {    	
    	try {
    		PydevPlugin.getPythonInterpreterManager().getDefaultInterpreter();
    	} catch(NotConfiguredInterpreterException ncie) {
    		return false;
    	}
    	return true;    	    	
    }
    
	/**
	 * The first page in Project generation wizard
	 * 
	 * Checks that Python interpreter is configured
	 */
    class WelcomePage extends WizardPage {
    
        
        Composite mainContainer;
        Composite mainControl;
                        
        public WelcomePage(String pageName) {
            super(pageName);
        }
        
        public WelcomePage(String pageName, String title,
                ImageDescriptor titleImage) {
            super(pageName, title, titleImage); 
        }
        
        public void createControl(Composite parent) {
        	            
            setDescription("Python project creration requirements");
        	
            mainContainer=new Composite(parent, SWT.NONE);
            GridData data=new GridData(GridData.FILL_BOTH);
            mainContainer.setLayoutData(data);
            GridLayout layout=new GridLayout(1, false);
            mainContainer.setLayout(layout);
            setControl(mainContainer);
                                            
            mainControl=getDefaultMain(mainContainer);
            
            // Never finish this page because there is no interpreter
            setPageComplete(false);   
        }
        
        protected Composite getDefaultMain(Composite parent)
        {
            Composite panel=new Composite(parent, SWT.NONE);
            GridLayout layout=new GridLayout(1, false);
            panel.setLayout(layout);
            GridData data;
            
            Text interpreterNoteText = new Text(panel, SWT.MULTI);
            interpreterNoteText.setEditable(false);
            interpreterNoteText.setText("Please configure a Python interpreter in Window -> Preferences -> PyDev \nbefore creating a new Python project");
                                                
            setErrorMessage(null);        
            return panel;        
        }    
        
    }
	/**
	 * A page asking inital properties of a Python project
	 * 
	 * Properties:
	 *  <ul>
	 *  <li>Project name. Project name cannot contain spaces. 
	 *  TODO: Not sure if this limit applies to Eclipse anymore.
	 *  </ul>
	 *  
	 *  Don't allow finishing until user has typed a name
	 *  without spaces to a project name field.
	 *  
	 */
    class ProjectPropertiesPage extends WizardPage {
       
        
        Composite mainContainer;
        Composite mainControl;
        Text projectName;
                        
        public ProjectPropertiesPage(String pageName) {
            super(pageName);
        }
        
        public ProjectPropertiesPage(String pageName, String title,
                ImageDescriptor titleImage) {
            super(pageName, title, titleImage);
        }
        
        public void createControl(Composite parent) {
            mainContainer=new Composite(parent, SWT.NONE);
            GridData data=new GridData(GridData.FILL_BOTH);            
            mainContainer.setLayoutData(data);            
            GridLayout layout=new GridLayout(1, false);
            mainContainer.setLayout(layout);
            setControl(mainContainer);
            
            data=new GridData(GridData.FILL_HORIZONTAL);
            
            this.setTitle("Project name");
            final String desc = "Give name for your Python project. Spaces are not allowed.";
            this.setDescription(desc);
                        
            
            Label l = new Label(mainContainer, SWT.NONE);
            l.setText("Project name:");
                                   
            projectName = new Text(mainContainer, SWT.BORDER);
            projectName.setLayoutData(data);
            projectName.addModifyListener(new ModifyListener() {
                	public void modifyText(ModifyEvent e) {
                	    // Allow move to next page when project name is correct
                	    String text = projectName.getText();
                	                    	    
                	    if(text.indexOf(' ') >= 0) {
                	        setErrorMessage("No spaces allowed");                	        
                	        setPageComplete(false);                	        
                	    } else {                	        
                	        setErrorMessage(null);                	        
                	        if(text.length() > 0) {
                    	        setPageComplete(true);
                    	    }
                	    }
                	}             
                }
            );
            
            projectName.setFocus();
            
            setPageComplete(false);            
            
        }
    }
    
}

