/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

/**
 * @author Fabio Zadrozny
 */
public class Definition {

    /**
     * Line of the definition.
     */
    public int line;
    
    /**
     * Column of the definition.
     */
    public int col;

    public Definition(){
        
    }

    public Definition(int line, int col){
        this.line = line;
        this.col = col;
    }

}
