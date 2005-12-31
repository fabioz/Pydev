package org.python.pydev.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Python module creation wizard
 * 
 * TODO: Create initial file content from a comment templates
 * 
 * @author Mikko Ohtamaa
 *
 */
public class PythonFileWizard extends Wizard implements INewWizard {

    public static final String WIZARD_ID = "org.python.pydev.ui.PythonFileWizard";
    
    /**
     * The workbench.
     */
    private IWorkbench workbench;

    /**
     * The current selection.
     */
    protected IStructuredSelection selection;    
    
    /** Wizard page asking filename */
    private FilePage filePage;
        
    /**
     * The <code>BasicNewResourceWizard</code> implementation of this 
     * <code>IWorkbenchWizard</code> method records the given workbench and
     * selection, and initializes the default banner image for the pages
     * by calling <code>initializeDefaultPageImageDescriptor</code>.
     * Subclasses may extend.
     */
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        this.workbench = workbench;
        this.selection = currentSelection;

        initializeDefaultPageImageDescriptor();
    }    
                
    /**
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages() {
       filePage = new FilePage("Create a new Python module");
       filePage.setDescription("Create a new Python module");       
       addPage(filePage);       
    }
       
   
    /**
     * User clicks Finish
     */
    public boolean performFinish() {
    	
    	// Create file object
    	IFile file = filePage.createNewFile();
    	if (file == null)
    		return false;

    	// Scroll to file in package explorer
    	BasicNewResourceWizard.selectAndReveal(file, workbench.getActiveWorkbenchWindow());

    	// Open editor on new file.
    	IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
    	try {
    		if (dw != null) {
    			IWorkbenchPage page = dw.getActivePage();
    			if (page != null) {
    				IDE.openEditor(page, file, true);
    			}
    		}
    	} catch (PartInitException e) {    		
    		PydevPlugin.log(e);
    		handleException(e);    		    		
    	}
    			
    	return true;
    }
    
    /**
     * Set Python logo to top bar
     */
    protected void initializeDefaultPageImageDescriptor() {             	    	
    	ImageDescriptor desc = PydevPlugin.imageDescriptorFromPlugin(PydevPlugin.getPluginID(), "icons/python-logo.png");//$NON-NLS-1$
        setDefaultPageImageDescriptor(desc);    	
    }    
    
    /**
     * Exception handling helper
     * 
     * Shows an error dialog 
     * 
     * @param e
     */
    protected void handleException(Throwable e) {
        
        IStatus status;
            	                  	              
        if(e instanceof CoreException) {
            CoreException ce = (CoreException) e;
            status = ce.getStatus();
        } else  {
            
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
           
    class FilePage extends WizardNewFileCreationPage {
            
        /**
         * Pass in the selection.
         */
        public FilePage(String pageId) {
          super(pageId, selection);
        }

        /**
         * The framework calls this to see if the file is correct.
         */
        protected boolean validatePage()
        {
          if (super.validatePage())
          {
            // Make sure the file ends in ".py".
            //
            String extension = new Path(getFileName()).getFileExtension();
            if (extension == null || !extension.equals("py"))
            {
              setErrorMessage("Filename must have \".py\" extension");
              return false;
            }
            else
            {
              return true;
            }
          }
          else
          {
            return false;
          }
        }
               
    }
}

