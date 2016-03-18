/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 17, 2006
 */
package org.python.pydev.ui.wizards.files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;


public abstract class AbstractPythonWizard extends Wizard implements INewWizard {

    public static void startWizard(AbstractPythonWizard wizard, String title) {
        IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
        IStructuredSelection sel = (IStructuredSelection) part.getSite().getSelectionProvider().getSelection();

        startWizard(wizard, title, sel);
    }

    /**
     * Must be called in the UI thread.
     * @param sel will define what appears initially in the project/source folder/name.
     */
    public static void startWizard(AbstractPythonWizard wizard, String title, IStructuredSelection sel) {
        IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();

        wizard.init(part.getSite().getWorkbenchWindow().getWorkbench(), sel);
        wizard.setWindowTitle(title);

        Shell shell = part.getSite().getShell();
        if (shell == null) {
            shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        }
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.setPageSize(350, 500);
        dialog.setHelpAvailable(false);
        dialog.create();
        dialog.open();
    }

    /**
     * The workbench.
     */
    protected IWorkbench workbench;

    /**
     * The current selection.
     */
    protected IStructuredSelection selection;

    protected String title;
    protected String description = "";

    public AbstractPythonWizard(String title) {
        this.title = title;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;

        initializeDefaultPageImageDescriptor();

    }

    /**
     * Set Python logo to top bar
     */
    protected void initializeDefaultPageImageDescriptor() {
        ImageDescriptor desc = PydevPlugin
                .imageDescriptorFromPlugin(PydevPlugin.getPluginID(), "icons/python_logo.png");//$NON-NLS-1$
        setDefaultPageImageDescriptor(desc);
    }

    /** Wizard page asking filename */
    protected AbstractPythonWizardPage filePage;

    /**
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        filePage = createPathPage();
        filePage.setTitle(this.title);
        filePage.setDescription(this.description);
        addPage(filePage);
    }

    /**
     * @return
     */
    protected abstract AbstractPythonWizardPage createPathPage();

    /**
     * User clicks Finish
     */
    @Override
    public boolean performFinish() {
        try {
            // Create file object
            IFile file = doCreateNew(new NullProgressMonitor());
            if (file == null) {
                //that's ok, as it just didn't create a file (but maybe a folder)...
                return true;
            }

            // Scroll to file in package explorer
            BasicNewResourceWizard.selectAndReveal(file, workbench.getActiveWorkbenchWindow());

            // Open editor on new file.
            IWorkbenchWindow dw = workbench.getActiveWorkbenchWindow();
            try {
                if (dw != null) {
                    IWorkbenchPage page = dw.getActivePage();
                    if (page != null) {
                        IEditorPart openEditor = IDE.openEditor(page, file, true);
                        afterEditorCreated(openEditor);
                    }
                }
            } catch (PartInitException e) {
                Log.log(e);
                return false;
            }
        } catch (Exception e) {
            Log.log(e);
            return false;
        }
        return true;
    }

    /**
     * Subclasses may override to do something after the editor was opened with a given file.
     * 
     * @param openEditor the opened editor
     */
    protected void afterEditorCreated(IEditorPart openEditor) {

    }

    /**
     * This method must be overriden to create the needed resource (either the package -- in which case it will return the __init__.py
     * or the file, in which case it will return the file itself).
     * @return the created resource
     */
    protected abstract IFile doCreateNew(IProgressMonitor monitor) throws CoreException;

}
