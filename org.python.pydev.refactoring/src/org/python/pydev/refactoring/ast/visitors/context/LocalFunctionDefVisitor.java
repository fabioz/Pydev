package org.python.pydev.refactoring.ast.visitors.context;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;

public class LocalFunctionDefVisitor extends GlobalFunctionDefVisitor {

	public LocalFunctionDefVisitor(ModuleAdapter module,
			AbstractScopeNode<?> parent) {
		super(module, parent);
	}

	@Override
	public void visit(SimpleNode node) throws Exception {
		if (nodeHelper.isClassDef(node)) {
			return;
		}
		super.visit(node);
	}
}
