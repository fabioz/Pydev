/*
 * Author: atotic
 * Created on Apr 8, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.Import;

/**
 * Represents import & import as statements
 */
public class ImportNode extends AbstractNode {

	public Import astNode;
	
	public ImportNode(AbstractNode parent, Import astNode) {
		super(parent);
		this.astNode = astNode;
		// instead of trying to calculate its arguments, just span the "import" keyword
		setStart(new Location(astNode.beginLine - 1, astNode.beginColumn - 8));
		setEnd(new Location(astNode.beginLine - 1, astNode.beginColumn - 2)); 
	}
	

}
