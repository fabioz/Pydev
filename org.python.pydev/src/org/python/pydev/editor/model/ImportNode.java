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

		
		setStart(new Location(astNode.beginLine - 1, astNode.beginColumn - 1));
		
		findEnd(astNode.names);
	}

	
	public String getName() {
		return "Import has a list of ImportAliases as its children";
	}
}
