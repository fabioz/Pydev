/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.Pass;

/**
 * @author Fabio Zadrozny
 */
public class PassNode extends AbstractNode {

	Pass astNode;
	
	/**
	 * 
	 * @param parent
	 * @param astNode
	 */
	public PassNode(AbstractNode parent, Pass astNode) {
		super(parent);	
		this.astNode = astNode;
		this.setStart(new Location(astNode.beginLine-1, astNode.beginColumn-1));
		this.setEnd(new Location(astNode.beginLine-1, astNode.beginColumn + 22));
	}
	

    public String getName() {
		return "pass";
	}
}