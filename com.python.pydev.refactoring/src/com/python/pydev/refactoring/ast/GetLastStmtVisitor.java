package com.python.pydev.refactoring.ast;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorBase;

public class GetLastStmtVisitor extends VisitorBase {

	private SimpleNode lastNode;

	@Override
	protected Object unhandled_node(SimpleNode node) throws Exception {
		this.lastNode = node;
		return null;
	}

	@Override
	public void traverse(SimpleNode node) throws Exception {
		node.traverse(this);
	}
	
	public SimpleNode getLastNode(){
		return lastNode;
	}
}
