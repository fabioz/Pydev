/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * self.a.b.c.d => a,b,c & d are attributes.
 */
public class AttributeNode extends AbstractNode {

	public Attribute astNode;
	
	public AttributeNode(AbstractNode parent, Attribute astNode) {
		super(parent);
		this.astNode = astNode;
		
		int lineDefinition = NodeUtils.getLineDefinition(astNode);
		int colDefinition = NodeUtils.getColDefinition(astNode);
		int[] colLineEnd = NodeUtils.getColLineEnd(astNode);

		this.setStart(new Location(lineDefinition, colDefinition));
		this.setEnd(new Location(colLineEnd[0], colLineEnd[1]));
		properties = PROP_CLICKABLE;	
	}

	public String getName() {
		return ((NameTok)astNode.attr).id;
	}
	
	public String toString() {
		return super.toString()+ getName();
	}
}
