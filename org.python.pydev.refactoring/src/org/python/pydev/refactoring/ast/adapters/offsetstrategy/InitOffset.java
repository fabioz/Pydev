package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.position.LastLineVisitor;

public class InitOffset extends BeginOffset {

	public InitOffset(IASTNodeAdapter adapter, IDocument doc) {
		super(adapter, doc);
	}

	@Override
	protected int getLine() {
		SimpleNode node = adapter.getASTNode();
		if (nodeHelper.isClassDef(node)) {

			ClassDef classNode = (ClassDef) node;
			for (int i = 0; i < classNode.body.length; i++) {
				if (nodeHelper.isInit(classNode.body[i])) {
					FunctionDef func = (FunctionDef) classNode.body[i];
					stmtType lastStmt = func.body[func.body.length - 1];
					LastLineVisitor visitor = VisitorFactory.createVisitor(LastLineVisitor.class, lastStmt);
					return visitor.getLastLine() - 1;
				}
			}
		}
		return super.getLine();
	}

}
