/*
 * Author: atotic
 * Created on Apr 7, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

/**
 * Top-level node representing a python module.
 * 
 * I also think of it as a file.
 */
public class ModuleNode extends AbstractNode {

	Scope scope;
	
	public ModuleNode(AbstractNode parent, int lines, int cols) {
		super(parent);
		scope = new Scope(this);
		// FileNode always spans the entire file
		this.start = Location.MIN_LOCATION;
		this.end = new Location(lines, cols);
	}
	
	public Scope getScope() {
		return scope;
	}
}
