/*
 * Author: atotic
 * Created on Apr 9, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;

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

		if (!findEnd(astNode.names)){
			setEnd(new Location(astNode.module.beginLine - 1, astNode.module.beginColumn - 1 + ((NameTok)astNode.module).id.length()));
		}
		properties = PROP_CLICKABLE;
	}
	
	public String getName() {
		return ((NameTok)astNode.module).id;
	}
}
