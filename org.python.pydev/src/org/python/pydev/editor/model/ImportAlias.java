/*
 * Author: atotic
 * Created on Apr 9, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.parser.ast.aliasType;

/**
 * ImportAlias represents individual imports.
 * 
 * For example "import os,sys,network" generates 3 alias nodes.
 */
public class ImportAlias extends AbstractNode {

	aliasType astNode;
	
	public ImportAlias(AbstractNode parent, aliasType astNode) {
		super(parent);
		this.astNode = astNode;
		setStart(new Location(astNode.beginLine - 1, astNode.beginColumn - 1));
		setEnd(new Location(astNode.beginLine - 1, astNode.beginColumn - 1 + astNode.name.length()));
		properties = PROP_CLICKABLE;
	}
}
