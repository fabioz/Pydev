/*
 * Created on Apr 8, 2006
 */
package com.python.pydev.refactoring.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.UndoEdit;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class PyRenameChange extends TextChange {

    private IProgressMonitor pm;
    private RefactoringRequest req;

    public PyRenameChange(IProgressMonitor pm, RefactoringRequest req) {
        super("RenameChange: "+req.duringProcessInfo.name);
        this.pm = pm;
        this.req = req;
    }

    @Override
    protected IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
        return req.doc;
    }

    @Override
    protected void commit(IDocument document, IProgressMonitor pm) throws CoreException {
    }

    @Override
    protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
    }

    @Override
    protected Change createUndoChange(UndoEdit edit) {
        return null;//no undo
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public Object getModifiedElement() {
        return req.pyEdit.getIFile();
    }


}
