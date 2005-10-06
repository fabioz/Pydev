/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.Name;


/**
 * Local nodes represent locals variables.
 * A LocalNode is generated for every mention of the variable.
 * First mention is considered a declaration, and is stored inside Scope.
 */
public class LocalNode extends AbstractNode {

	// True if local is a builtin
	public static boolean isBuiltin(String name) {
		return (name.equals("None"));
	}

	Name astNode;
	/**
	 * @param parent
	 */
	public LocalNode(AbstractNode parent, Name astNode) {
		super(parent);
		this.astNode = astNode;
		this.setStart(new Location(astNode.beginLine - 1, astNode.beginColumn-1));
		this.setEnd(new Location(astNode.beginLine - 1, astNode.beginColumn -1+ astNode.id.length()));
		parent.getScope().addLocalDefinition(this);
		properties = PROP_CLICKABLE;
	}

	public String getName() {
		return astNode.id;
	}
	
	public String toString() {
		return super.toString() + astNode.id;
	}
}
