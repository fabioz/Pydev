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

package org.python.pydev.refactoring.coderefactoring.extractlocal.edit;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.edit.AbstractReplaceEdit;
import org.python.pydev.shared_core.structure.Tuple;

public class ReplaceDuplicateWithVariableEdit extends AbstractReplaceEdit {

    private ITextSelection selection;
    private String variableName;
    private Tuple<ITextSelection, SimpleNode> dup;

    public ReplaceDuplicateWithVariableEdit(ExtractLocalRequest req, Tuple<ITextSelection, SimpleNode> dup) {
        super(req);
        this.dup = dup;

        this.selection = dup.o1;
        this.variableName = req.variableName;
    }

    @Override
    protected SimpleNode getEditNode() {
        Name name = new Name(variableName, expr_contextType.Load, false);
        return name;
    }

    @Override
    public int getOffsetStrategy() {
        return 0;
    }

    @Override
    public int getOffset() {
        return selection.getOffset();
    }

    @Override
    protected int getReplaceLength() {
        return selection.getLength();
    }

}
