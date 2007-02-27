package org.python.pydev.refactoring.ast.adapters;

import org.python.pydev.parser.jython.SimpleNode;

public class SimpleAdapter extends AbstractNodeAdapter<SimpleNode> {

	public SimpleAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, SimpleNode node) {
		super(module, parent, node);
	}

}
