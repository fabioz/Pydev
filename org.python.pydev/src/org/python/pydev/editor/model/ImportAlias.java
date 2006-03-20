/*
 * Author: atotic
 * Created on Apr 9, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;

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
		
		
		SimpleNode name = astNode.name;
		setStart(new Location(name.beginLine - 1, name.beginColumn - 1));
		
		
		//for getting the end, it might change for the 'as' (if any)
		if(astNode.asname != null){
			name = astNode.asname;
		}

		setEnd(new Location(name.beginLine - 1, name.beginColumn - 1 + ((NameTok)astNode.name).id.length()));
		
		properties = PROP_CLICKABLE;
	}
	
	public String getName() {
		return ((NameTok)astNode.name).id;
	}
}
