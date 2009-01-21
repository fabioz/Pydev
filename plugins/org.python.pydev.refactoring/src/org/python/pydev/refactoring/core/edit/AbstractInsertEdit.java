/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.edit;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public abstract class AbstractInsertEdit extends AbstractTextEdit {

    public AbstractInsertEdit(IRefactoringRequest req) {
        super(req);
    }

    @Override
    public TextEdit getEdit() {
        return new InsertEdit(getOffset(), getFormatedNode());
    }

}
