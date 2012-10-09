/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.position.LastLineVisitor;

public class InitOffset extends BeginOffset {

    public InitOffset(IASTNodeAdapter<? extends SimpleNode> adapter, IDocument doc, AdapterPrefs adapterPrefs) {
        super(adapter, doc, adapterPrefs);
    }

    @Override
    protected int getLine() {
        SimpleNode node = adapter.getASTNode();
        if (nodeHelper.isClassDef(node)) {

            ClassDef classNode = (ClassDef) node;
            for (stmtType statement : classNode.body) {
                if (nodeHelper.isInit(statement)) {
                    FunctionDef func = (FunctionDef) statement;
                    stmtType lastStmt = func.body[func.body.length - 1];
                    LastLineVisitor visitor = VisitorFactory.createVisitor(LastLineVisitor.class, lastStmt);
                    return visitor.getLastLine();
                }
            }
        }
        return super.getLine();
    }

}
