/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.core.edit;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public abstract class AbstractRemoveEdit extends AbstractTextEdit {

    public AbstractRemoveEdit(IRefactoringRequest req) {
        super(req);
    }

    @Override
    public TextEdit getEdit() {
        return new ReplaceEdit(getOffset(), getDeleteLength(), "");
    }

    protected abstract int getDeleteLength();

    @Override
    /* Not necessary, but enforced by superclass */
    public int getOffsetStrategy() {
        return 0;
    }
}
