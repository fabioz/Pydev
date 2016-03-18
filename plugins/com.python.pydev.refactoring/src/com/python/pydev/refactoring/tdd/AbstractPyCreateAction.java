/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.shared_ui.EditorUtils;


public abstract class AbstractPyCreateAction extends Action implements IEditorActionDelegate {

    public static final int LOCATION_STRATEGY_BEFORE_CURRENT = 0; //before the current method (in the same level)
    public static final int LOCATION_STRATEGY_END = 1; //end of file or end of class
    public static final int LOCATION_STRATEGY_FIRST_METHOD = 2; //In a class as the first method

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

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        if (targetEditor == null) {
            Status status = PydevPlugin.makeStatus(IStatus.ERROR, "Unable to do refactoring.", null);
            ErrorDialog.openError(EditorUtils.getShell(), "Unable to do refactoring.",
                    "Target editor is null (not PyEdit).", status);
            return;
        }

        try {
            RefactoringInfo refactoringInfo = new RefactoringInfo(targetEditor);
            execute(refactoringInfo, LOCATION_STRATEGY_BEFORE_CURRENT);
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

    public abstract void execute(RefactoringInfo refactoringInfo, int locationStrategyBeforeCurrent);

    public abstract ICompletionProposal createProposal(RefactoringInfo refactoringInfo, String actTok,
            int locationStrategy, List<String> parametersAfterCall);

}
