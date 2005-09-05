/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * Represents a function definition.
 */
public class FunctionNode extends AbstractNode {

	public FunctionDef astNode;
	Scope scope;
	
	public FunctionNode(AbstractNode parent, FunctionDef node, String lineText) {
		super(parent);
		this.astNode = node;
		scope = new Scope(this);
		parent.getScope().addFunctionDefinition(this);

		NameTok nameTok = (NameTok) astNode.name;
		setStart(new Location(nameTok.beginLine - 1, nameTok.beginColumn -1 ));
        setEnd(new Location(nameTok.beginLine - 1, nameTok.beginColumn -1 + NodeUtils.getNameFromNameTok(nameTok).length()));
		fixColumnLocation(start, lineText);
		fixColumnLocation(end, lineText);
		properties = PROP_CLICKABLE;
	}
	
	public String getName() {
		return NodeUtils.getNameFromNameTok((NameTok) astNode.name);
	}

	public Scope getScope() {
		return scope;
	}
}
