/*
 * Author: atotic
 * Created on Apr 9, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.If;

/**
 * if __name__ == 'main' Node.
 */
public class NameEqualsMainNode extends AbstractNode {

	If astNode;
	
	public NameEqualsMainNode(AbstractNode parent, If astNode) {
		super(parent);	
		this.astNode = astNode;
		this.setStart(new Location(astNode.beginLine, astNode.beginColumn-1));
		this.setEnd(new Location(astNode.beginLine, astNode.beginColumn + 22));
	}
}
