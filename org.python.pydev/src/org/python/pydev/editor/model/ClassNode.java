/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.ClassDef;

/**
 * Represents a class definition.
 */
public class ClassNode extends AbstractNode {

	public ClassDef astNode;
	Scope scope;
	
	public ClassNode(AbstractNode parent, ClassDef astNode) {
		super(parent);
		this.astNode = astNode;
		scope = new Scope(this);
		setStart(new Location(astNode.beginLine - 1, astNode.beginColumn + 5));
		setEnd(new Location(astNode.beginLine - 1, astNode.beginColumn + 5 + astNode.name.length()));
		properties = PROP_CLICKABLE;
	}
	
	public Scope getScope() {
		return scope;
	}
}
