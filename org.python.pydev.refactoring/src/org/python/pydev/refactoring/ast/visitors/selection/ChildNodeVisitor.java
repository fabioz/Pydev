package org.python.pydev.refactoring.ast.visitors.selection;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorBase;

public class ChildNodeVisitor extends VisitorBase {

	private SimpleNode child;

	private boolean found;

	public ChildNodeVisitor(SimpleNode child) {
		this.child = child;
		this.found = false;
	}

	@Override
	public void traverse(SimpleNode node) throws Exception {
		if (node != null && !(found)) {
			node.traverse(this);
		}
	}

	@Override
	protected Object unhandled_node(SimpleNode node) throws Exception {
		if (node.equals(child)) {
			this.found = true;
		}
		return null;
	}

	public boolean isFound() {
		return found;
	}

}
