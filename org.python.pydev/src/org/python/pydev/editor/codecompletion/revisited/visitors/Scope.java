/*
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.Iterator;
import java.util.Stack;

import org.python.parser.SimpleNode;

/**
 * @author Fabio Zadrozny
 */
public class Scope {

    public Stack scope = new Stack();
    
    public Scope(Stack scope){
        this.scope.addAll(scope);
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Scope)) {
            return false;
        }
        
        Scope s = (Scope) obj;
        
        if(this.scope.size() != s.scope.size()){
            return false;
        }
        
        return checkIfScopesMatch(s);
    }
    
    /**
     * Checks if this scope is an outer scope of the scope passed as a param (s).
     * Or if it is the same scope. 
     * 
     * @param s
     * @return
     */
    public boolean isOuterOrSameScope(Scope s){
        if(this.scope.size() > s.scope.size()){
            return false;
        }
 
        return checkIfScopesMatch(s);
    }

    /**
     * @param s
     * @return
     */
    private boolean checkIfScopesMatch(Scope s) {
        Iterator otIt = s.scope.iterator();
        
        for (Iterator iter = this.scope.iterator(); iter.hasNext();) {
            SimpleNode element = (SimpleNode) iter.next();
            SimpleNode otElement = (SimpleNode) otIt.next();
            
            if(element.beginColumn != otElement.beginColumn)
                return false;
            
            if(element.beginLine != otElement.beginLine)
                return false;
            
            if(! element.getClass().equals(otElement.getClass()))
                return false;
            
            if(! AbstractVisitor.getFullRepresentationString(element).equals( AbstractVisitor.getFullRepresentationString(otElement)))
                return false;
            
        }
        return true;
    }
}
