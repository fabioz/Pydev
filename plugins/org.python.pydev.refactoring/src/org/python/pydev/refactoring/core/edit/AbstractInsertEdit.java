/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.edit;

import org.eclipse.text.edits.InsertEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public abstract class AbstractInsertEdit extends AbstractTextEdit {

    public AbstractInsertEdit(IRefactoringRequest req) {
        super(req);
    }

    @Override
    public InsertEdit getEdit() throws MisconfigurationException {
        return new InsertEdit(getOffset(), getFormattedNode());
    }

}
