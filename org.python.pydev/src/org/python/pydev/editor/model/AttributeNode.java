/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.Attribute;

/**
 * self.a.b.c.d => a,b,c & d are attributes.
 */
public class AttributeNode extends AbstractNode {

	public Attribute astNode;
	
	public AttributeNode(AbstractNode parent, Attribute astNode, String lineText) {
		super(parent);
		this.astNode = astNode;
		this.setStart(new Location(astNode.beginLine - 1, astNode.beginColumn-1));
		this.setEnd(new Location(astNode.beginLine - 1, astNode.beginColumn -1 + astNode.attr.length()));
		fixColumnLocation(start, lineText);
		fixColumnLocation(end, lineText);
		// HACK alert
		// For the final attribute the location produced by AST is wrong (probably intentionally)
		// So I'll resort to desperate measure of doing a text search to find a matching string
		// this will work sometimes.
		if (astNode.beginColumn <= astNode.value.beginColumn) {
			Location temp = new Location(0, astNode.value.beginColumn -1);
			fixColumnLocation(temp, lineText);
			int grep = lineText.indexOf(astNode.attr, temp.column+1);
			if (grep != -1 && grep > start.column) {
				start.column = grep;
				end.column = start.column + astNode.attr.length();
			}
		}
		properties = PROP_CLICKABLE;	
	}

	public String getName() {
		return astNode.attr;
	}
	
	public String toString() {
		return super.toString()+ astNode.attr;
	}
}
