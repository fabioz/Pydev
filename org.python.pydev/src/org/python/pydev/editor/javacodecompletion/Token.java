/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import org.python.parser.SimpleNode;

/**
 * @author Fabio Zadrozny
 */
public class Token {

    /**
     * @param node
     */
    public Token(SimpleNode node) {
        this.ast = node;
    }
    public SimpleNode ast;
    public String docStr;
    
 
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Token: "+this.ast.getClass().getName();
    }
}
