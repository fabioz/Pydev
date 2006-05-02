/*
 * Author: atotic
 * Created on Apr 9, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Str;

/**
 * if __name__ == '__main__' Node.
 */
public class NameEqualsMainNode extends AbstractNode {

	If astNode;
	
	public NameEqualsMainNode(AbstractNode parent, If astNode, Str mainStr) {
		super(parent);	
		this.astNode = astNode;
		
		String rep = mainStr.s;
		this.setStart(new Location(astNode.beginLine-1, astNode.beginColumn-1));
		this.setEnd(new Location(astNode.beginLine-1, astNode.beginColumn + mainStr.beginColumn+rep.length()));
	}
	
	public String getName() {
		return "if __name__  == '__main__'";
	}
}
