package org.python.pydev.ui.wizards.gettingstarted;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.perspective.PythonPerspectiveFactory;

/**
 * This is a getting started wizard for Pydev:
 * 
 * It'll guide the user to configure the initial interpreter and create an initial project.
 */
public class PythonGettingStartedWizard extends AbstractNewProjectWizard {

    private IWorkbench workbench;


    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        
    }
    
    public void addPages() {
        addGettingStartedPage();
        addProjectReferencePage();
    }
    
    protected GettingStartedPage gettingStartedPage;

    /**
     * Adds the general info page to the wizard.
     */
    protected void addGettingStartedPage(){
        // only add page if there are already projects in the workspace
        if (ResourcesPlugin.getWorkspace().getRoot().getProjects().length > 0) {
            gettingStartedPage = new GettingStartedPage("Getting Started");
            gettingStartedPage.setTitle("Getting Started");
            gettingStartedPage.setDescription("Basic Getting Started on Configuring Pydev");
            this.addPage(gettingStartedPage);
        }
    }    
    
    
    @Override
    public boolean performFinish() {
        
        // Switch to default 'Pydev' perspective
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        
        try {
            workbench.showPerspective(PythonPerspectiveFactory.PERSPECTIVE_ID, window);
        } catch (WorkbenchException we) {
            PydevPlugin.log(we);
        }
        return true;
    }
}
