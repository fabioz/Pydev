/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;

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
		parent.getScope().addFunctionDefinition(this);

		NameTok nameTok = (NameTok) astNode.name;
		int line = nameTok.beginLine - 1;
		int beginCol = nameTok.beginColumn -1;
		
		setStart(new Location(line, beginCol ));
        setEnd(new Location(line, beginCol + NodeUtils.getNameFromNameTok(nameTok).length()));
		
        properties = PROP_CLICKABLE;
	}
	
	public String getName() {
		return NodeUtils.getNameFromNameTok((NameTok) astNode.name);
	}

	public Scope getScope() {
		return scope;
	}
}
