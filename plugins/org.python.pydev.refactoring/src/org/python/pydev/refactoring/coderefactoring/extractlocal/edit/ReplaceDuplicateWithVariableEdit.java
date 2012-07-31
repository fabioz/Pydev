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

import com.aptana.shared_core.structure.Tuple;

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
