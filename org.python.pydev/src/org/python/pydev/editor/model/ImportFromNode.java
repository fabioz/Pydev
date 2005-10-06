/*
 * Author: atotic
 * Created on Apr 9, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.NameTok;

/**
 * ImportFrom node spans first argument of "from import" statement.
 * 
 * from repr import Repr, this node would span "repr".
 */
public class ImportFromNode extends AbstractNode {

	public ImportFrom astNode;
	
	public ImportFromNode(AbstractNode parent, ImportFrom astNode) {
		super(parent);
		this.astNode = astNode;
		
		setStart(new Location(astNode.module.beginLine - 1, astNode.module.beginColumn - 1));

		findEnd(astNode.names);
		properties = PROP_CLICKABLE;
	}
	
	public String getName() {
		return ((NameTok)astNode.module).id;
	}
}
