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
	
	public ImportFromNode(AbstractNode parent, ImportFrom astNode, String lineText) {
		super(parent);
		this.astNode = astNode;
		setStart(new Location(astNode.beginLine - 1, astNode.beginColumn - 1));
		setEnd(new Location(astNode.beginLine - 1, astNode.beginColumn -1 + ((NameTok)astNode.module).id.length()));
		fixColumnLocation(start, lineText);
		fixColumnLocation(end, lineText);
		properties = PROP_CLICKABLE;
	}
	
	public String getName() {
		return ((NameTok)astNode.module).id;
	}
}
