/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.model;

import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * Pointer points to a python resource inside a file system. 
 * 
 * You can create one of these, and use PyOpenAction to open the 
 * right editor.
 */
public class ItemPointer {

    /**
     * IFile or File object (may be null)
     */
    public final Object file;
    
    /**
     * Position of the 1st character 
     */
    public final Location start; 
    
    /**
     * Position of the last character
     */
    public final Location end;
    
    /**
     * The definition that originated this ItemPointer (good chance of being null).
     */
    public final Definition definition;
    
    /**
     * The path within the zip file for this pointer (null if we're not dealing with a zip file)
     */
    public final String zipFilePath;
    
    
    public ItemPointer(Object file) {
        this(file, new Location(), new Location());
    }

    public ItemPointer(Object file, SimpleNode n) {
        int line = n.beginLine;
        int col = n.beginColumn;
        
        this.file = file;
        this.start = new Location(line-1, col-1);
        this.end = new Location(line-1, col-1);
        this.definition = null;
        this.zipFilePath = null;
    }
    
    public ItemPointer(Object file, Location start, Location end) {
        this(file, start, end, null, null);
    }
    
    public ItemPointer(Object file, Location start, Location end, Definition definition, String zipFilePath) {
        this.file = file;
        this.start = start;
        this.end = end;
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
        int colLineBasedHash = (this.end.column + this.start.line + 7) * 3;
        if(this.file != null){
            return this.file.hashCode() + colLineBasedHash;
        }else{
            return colLineBasedHash;
        }
    }
}
