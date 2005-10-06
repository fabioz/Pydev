/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.ClassDef;
import org.python.parser.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;

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

        NameTok nameTok = (NameTok) astNode.name;
		int line = nameTok.beginLine - 1;
		int beginCol = nameTok.beginColumn -1;
		
		setStart(new Location(line, beginCol));
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
