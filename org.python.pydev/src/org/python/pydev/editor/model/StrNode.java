/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.model;

import org.python.pydev.parser.jython.ast.Str;

/**
 * @author Fabio Zadrozny
 */
public class StrNode extends AbstractNode {

	Str astNode;
	
	/**
	 * 
	 * @param parent
	 * @param astNode
	 */
	public StrNode(AbstractNode parent, Str astNode) {
		super(parent);	
		this.astNode = astNode;
		this.setStart(new Location(astNode.beginLine-1, astNode.beginColumn-1));
		this.setEnd(new Location(astNode.beginLine-1, astNode.beginColumn + astNode.s.length()));
	}
	

    public String getName() {
		return "astNode";
	}
}