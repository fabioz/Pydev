/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.refactoring.core.base.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.ui.core.PythonRefactoringWizard;
import org.python.pydev.shared_ui.EditorUtils;


public abstract class AbstractRefactoringAction extends Action implements IEditorActionDelegate {
    protected AbstractPythonRefactoring refactoring;
    protected PyEdit targetEditor;

    @Override
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        if (targetEditor instanceof ITextEditor) {
            if (targetEditor instanceof PyEdit) {
                this.targetEditor = (PyEdit) targetEditor;
            } else {
                this.targetEditor = null;
                Log.log(new RuntimeException("Editor not a PyEdit."));
            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    /**
     * Save all dirty editors in the workbench.. Opens a dialog to prompt the
     * user. Return true if successful. Return false if the user has canceled
     * the command.
     * 
     * @return <code>true</code> if the command succeeded, and
     *         <code>false</code> if the operation was canceled by the user or
     *         an error occurred while saving
     */
    protected static boolean saveAll() {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        return IDE.saveAllEditors(new IResource[] { workspaceRoot }, true);
    }

    protected int getWizardFlags() {
        return RefactoringWizard.WIZARD_BASED_USER_INTERFACE;
    }

    @Override
    public void run(IAction action) {
        if (targetEditor == null) {
            Status status = PydevPlugin.makeStatus(IStatus.ERROR, "Unable to do refactoring.", null);
            ErrorDialog.openError(EditorUtils.getShell(), "Unable to do refactoring.",
                    "Target editor is null (not PyEdit).", status);
            return;
        }

        boolean allFilesSaved = saveAll();
        if (!allFilesSaved) {
            return;
        }

        RefactoringInfo info;
        try {
            info = new RefactoringInfo(this.targetEditor);
            PythonRefactoringWizard wizard = new PythonRefactoringWizard(this.createRefactoring(info),
                    this.targetEditor, this.createPage(info), this.getWizardFlags());

            wizard.run();

            this.targetEditor.getDocumentProvider().changed(this.targetEditor.getEditorInput());
        } catch (Throwable e) {
            Log.log(e);
            Throwable initial = e;
            while (e.getCause() != null) {
                e = e.getCause();
            }
            //get the root cause
            Status status = PydevPlugin.makeStatus(IStatus.ERROR, "Error making refactoring", initial);
            ErrorDialog.openError(EditorUtils.getShell(), "Error making refactoring", e.getMessage(), status);
        }
    }

    /**
     * Create a refactoring.
     * 
     * Has to be implemented in the subclass
     */
    protected abstract AbstractPythonRefactoring createRefactoring(RefactoringInfo info);

    protected abstract IWizardPage createPage(RefactoringInfo info);
}
