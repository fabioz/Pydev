/******************************************************************************
* Copyright (C) 2006-2009  IFS Institute for Software and others
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

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.ast.visitors.rewriter.Rewriter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public abstract class AbstractReplaceEdit extends AbstractTextEdit {

    public AbstractReplaceEdit(IRefactoringRequest req) {
        super(req);
    }

    @Override
    public TextEdit getEdit() throws MisconfigurationException {
        return new ReplaceEdit(getOffset(), getReplaceLength(), getFormattedNode());
    }

    @Override
    protected String getFormattedNode() throws MisconfigurationException {
        String source = Rewriter.createSourceFromAST(getEditNode(), adapterPrefs);
        return source.trim();
    }

    protected abstract int getReplaceLength();

}
