/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.FunctionDef;

/**
 * Represents a function definition.
 */
public class FunctionNode extends AbstractNode {

	public FunctionDef astNode;
	Scope scope;
	
	public FunctionNode(AbstractNode parent, FunctionDef node) {
		super(parent);
		this.astNode = node;
		scope = new Scope(this);
		setStart(new Location(astNode.beginLine - 1, astNode.beginColumn + 3));
		setEnd(new Location(astNode.beginLine - 1, astNode.beginColumn + 3 + astNode.name.length()));
		properties = PROP_CLICKABLE;
	}
	
	public Scope getScope() {
		return scope;
	}
}
