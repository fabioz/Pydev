/******************************************************************************
* Copyright (C) 2006-2010  IFS Institute for Software and others
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
