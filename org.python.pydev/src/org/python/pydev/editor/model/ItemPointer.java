/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

/**
 * Pointer points to a python resource inside a file system. 
 * 
 * You can create one of these, and use PyOpenAction to open the 
 * right editor.
 */
public class ItemPointer {

	public Object file;	// IFile or File object
	public Location start; // (first character)
	public Location end;   // (last character)
	
	public ItemPointer(Object file) {
		this(file, new Location(), new Location());
	}

	public ItemPointer(Object file, Location start, Location end) {
		this.file = file;
		this.start = start;
		this.end = end;
	}
}
