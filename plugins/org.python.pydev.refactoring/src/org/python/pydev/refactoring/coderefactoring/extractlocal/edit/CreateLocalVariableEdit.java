/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal.edit;

import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class CreateLocalVariableEdit extends AbstractInsertEdit {

    private RefactoringInfo info;

    private String variableName;

    private exprType expression;

    public CreateLocalVariableEdit(ExtractLocalRequest req) {
        super(req);
        this.info = req.getRefactoringInfo();
        this.variableName = req.getVariableName();
        this.expression = req.getExpression();
    }

    @Override
    protected SimpleNode getEditNode() {
        exprType variable = new Name(variableName, expr_contextType.Store, false);
        exprType[] target = { variable };

        return new Assign(target, expression);
    }

    @Override
    public int getOffset() {
        PySelection selection = new PySelection(info.getDocument(), info.getExtendedSelection());
        return selection.getStartLineOffset();
    }

    @Override
    public int getOffsetStrategy() {
        return 0;
    }
}
