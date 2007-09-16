/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import java.io.File;

import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.jython.SimpleNode;

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
    public String zipFilePath; //the path within the zip file for this pointer (null if we're not dealing with a zip file)
	
	public ItemPointer(Object file) {
		this(file, new Location(), new Location());
	}

	public ItemPointer(Object file, SimpleNode n) {
        int line = n.beginLine;
        int col = n.beginColumn;
        
        this.file = file;
        this.start = new Location(line-1, col-1);
        this.end = new Location(line-1, col-1);
    }
    
	public ItemPointer(Object file, Location start, Location end) {
		this.file = file;
		this.start = start;
		this.end = end;
	}
    
    public ItemPointer(File file2, Location location, Location location2, Definition definition, String zipFilePath) {
        this(file2, location, location2);
        this.definition = definition;
        this.zipFilePath = zipFilePath;
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
