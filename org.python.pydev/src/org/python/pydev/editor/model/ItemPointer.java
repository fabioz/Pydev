/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

/**
 *
 * TODO Comment this class
 */
public class ItemPointer {
	
	public Object file;	// IFile or File...
	public Location start;
	public Location end;
	
	public ItemPointer(Object file) {
		this(file, new Location(), new Location());
	}

	public ItemPointer(Object file, Location start, Location end) {
		this.file = file;
		this.start = start;
		this.end = end;
	}

}
