/*
 * Author: atotic
 * Created on Apr 7, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.eclipse.core.resources.IFile;

/**
 * Top-level node representing a python module.
 * 
 * I also think of it as a file.
 */
public class ModuleNode extends AbstractNode {

	Scope scope;
	IFile file;
	
	public ModuleNode(IFile file, int lines, int cols) {
		super(null);
		scope = new Scope(this);
		// FileNode always spans the entire file
		this.file = file;
		this.start = Location.MIN_LOCATION;
		this.end = new Location(lines, cols);
	}

	public String getName() {
		// TODO module needs a name, probably a file it comes from
		return "module";
	}
	
	public IFile getFile() {
		return file;
	}
	
	public Scope getScope() {
		return scope;
	}
}
