/*
 * Created on Jan 17, 2006
 */
package org.python.pydev.ui.wizards.files;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.python.pydev.plugin.PydevPlugin;

public class AbstractPythonWizard extends Wizard implements INewWizard {


    /**
     * The workbench.
     */
    protected IWorkbench workbench;

    /**
     * The current selection.
     */
    protected IStructuredSelection selection;

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
        
        initializeDefaultPageImageDescriptor();

    }

    /**
     * Set Python logo to top bar
     */
    protected void initializeDefaultPageImageDescriptor() {
        ImageDescriptor desc = PydevPlugin.imageDescriptorFromPlugin(PydevPlugin.getPluginID(), "icons/python_logo.png");//$NON-NLS-1$
        setDefaultPageImageDescriptor(desc);
    }

    
    /** Wizard page asking filename */
    protected PythonAbstractPathPage filePage;

    /**
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages() {
        filePage = new PythonAbstractPathPage("Create a new Python module", selection);
        filePage.setDescription("Create a new Python module");
        addPage(filePage);
    }

    
    /**
     * User clicks Finish
     */
    @Override
    public boolean performFinish() {
        // Create file object
        IFile file = filePage.createNewFile();
        if (file == null){
            return false;
        }

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
        }

        return true;
    }

}
