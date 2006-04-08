/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import java.io.File;

import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

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
    public Definition definition; //the definition that originated this ItemPointer (it might be null).
	
	public ItemPointer(Object file) {
		this(file, new Location(), new Location());
	}

	public ItemPointer(Object file, Location start, Location end) {
		this.file = file;
		this.start = start;
		this.end = end;
	}
    
    public ItemPointer(File file2, Location location, Location location2, Definition definition) {
        this(file2, location, location2);
        this.definition = definition;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("ItemPointer [");
        buffer.append(file);
        buffer.append(" - ");
        buffer.append(start);
        buffer.append(" - ");
        buffer.append(end);
        buffer.append("]");
        return buffer.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ItemPointer)){
            return false;
        }
        
        ItemPointer i = (ItemPointer) obj;
        if(!i.file.equals(file)){
            return false;
        }
        if(!i.start.equals(start)){
            return false;
        }
        if(!i.end.equals(end)){
            return false;
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        if(this.file != null){
            return this.file.hashCode() * 17;
        }else{
            return (this.end.column+1) * (this.start.line+2) * 9;
        }
    }
}
