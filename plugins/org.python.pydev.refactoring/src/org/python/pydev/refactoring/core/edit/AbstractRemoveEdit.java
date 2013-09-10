/******************************************************************************
* Copyright (C) 2007-2009  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
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
