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

public class ReplaceWithVariableEdit extends AbstractReplaceEdit {

    private String variableName;

    private int offset;

    private int replaceLength;

    public ReplaceWithVariableEdit(ExtractLocalRequest req) {
        super(req);
        this.variableName = req.getVariableName();

        ITextSelection selection = req.getRefactoringInfo().getExtendedSelection();
        this.offset = selection.getOffset();

        this.replaceLength = selection.getLength();
        this.offsetAdapter = req.getOffsetNode();
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
        return offset;
    }

    @Override
    protected int getReplaceLength() {
        return replaceLength;
    }

}
