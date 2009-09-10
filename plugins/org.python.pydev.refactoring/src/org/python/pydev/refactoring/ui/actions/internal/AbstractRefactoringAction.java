/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.actions.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.refactoring.core.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.ui.PythonRefactoringWizard;

public abstract class AbstractRefactoringAction extends Action implements IEditorActionDelegate {
    private AbstractPythonRefactoring refactoring;
    private ITextEditor targetEditor;

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        if(targetEditor instanceof ITextEditor){
            if(targetEditor.getEditorInput() instanceof FileEditorInput){
                this.targetEditor = (ITextEditor) targetEditor;
            }else{
                this.targetEditor = null;
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

    private static boolean saveAll() {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        return IDE.saveAllEditors(new IResource[] { workspaceRoot }, true);
    }

    private void setupRefactoring() throws MisconfigurationException {
        IPythonNature nature = null;
        if(targetEditor instanceof IPyEdit){
            nature = ((IPyEdit) targetEditor).getPythonNature();
        }
        RefactoringInfo info = new RefactoringInfo(targetEditor, nature);
        this.refactoring = this.createRefactoring(info);

        // Example showing errors: MessageDialog.openError(getShell(), Messages.errorTitle, msg);
    }

    private void openWizard(IAction action) {
        try{
            this.setupRefactoring();
        }catch(MisconfigurationException e){
            Log.log(e);
        }

        PythonRefactoringWizard wizard = new PythonRefactoringWizard(this.refactoring, targetEditor);
        wizard.run();

        targetEditor.getDocumentProvider().changed(targetEditor.getEditorInput());
    }

    public void run(IAction action) {
        /* TODO: check if inline is necessary */
        if(saveAll()){
            this.openWizard(action);
        }
    }

    /**
     * Create a refactoring.
     * 
     * Has to be implemented in the subclass
     * 
     * @param info 
     * @return
     */
    protected abstract AbstractPythonRefactoring createRefactoring(RefactoringInfo info);
}
