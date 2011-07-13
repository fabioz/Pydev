/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.coderefactoring.inlinelocal.edit;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.refactoring.coderefactoring.inlinelocal.request.InlineLocalRequest;
import org.python.pydev.refactoring.core.edit.AbstractReplaceEdit;

public class ReplaceWithExpression extends AbstractReplaceEdit {
    private int offset;
    private int replaceLength;
    private exprType expression;

    public ReplaceWithExpression(InlineLocalRequest req, Name variable) {
        super(req);

        IDocument doc = req.info.getDocument();

        this.offset = org.python.pydev.parser.visitors.NodeUtils.getOffset(doc, variable);

        this.expression = req.assignment.value;
        this.replaceLength = variable.id.length();
    }

    @Override
    protected SimpleNode getEditNode() {
        return expression;
    }

    @Override
    public int getOffsetStrategy() {
        return 0;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    protected int getReplaceLength() {
        return replaceLength;
    }

}
