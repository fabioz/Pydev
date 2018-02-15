/******************************************************************************
* Copyright (C) 2010-2012  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.refactoring.core.base;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.internal.core.refactoring.TextChanges;
import org.eclipse.ltk.internal.core.refactoring.UndoDocumentChange;
import org.eclipse.text.edits.UndoEdit;

public class PyDocumentChangeForTests extends TextChange {

    private IDocument fDocument;
    private int fLength;

    /**
     * Creates a new <code>DocumentChange</code> for the given
     * {@link IDocument}.
     *
     * @param name the change's name. Has to be a human readable name.
     * @param document the document this change is working on
     */
    public PyDocumentChangeForTests(String name, IDocument document) {
        super(name);
        Assert.isNotNull(document);
        fDocument = document;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getModifiedElement() {
        return fDocument;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeValidationData(IProgressMonitor pm) {
        // as long as we don't have modification stamps on documents
        // we can only remember its length.
        fLength = fDocument.getLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
        pm.beginTask("", 1); //$NON-NLS-1$
        RefactoringStatus result = TextChanges.isValid(fDocument, fLength);
        pm.worked(1);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IDocument acquireDocument(IProgressMonitor pm) throws CoreException {
        return fDocument;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void commit(IDocument document, IProgressMonitor pm) throws CoreException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Change createUndoChange(UndoEdit edit) {
        return new UndoDocumentChange(getName(), fDocument, edit);
    }

    @Override
    public String getTextType() {
        return "py";
    }

}
