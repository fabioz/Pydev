/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;

/**
 * @author Fabio Zadrozny
 */
public class SourceToken implements IToken{

    /**
     * @param node
     */
    public SourceToken(SimpleNode node, String rep, String doc) {
        this.ast = node;
    }
    private SimpleNode ast;
    private String representation;
    private String docStr;
    
 
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Token: "+representation;
    }
    
    /**
     * 
     * @return the completion type depending on the syntax tree.
     */
    public int getCompletionType(){
        if (ast instanceof ClassDef){
            return PyCodeCompletion.TYPE_CLASS; 
        
        }else if (ast instanceof FunctionDef){
            return PyCodeCompletion.TYPE_FUNCTION; 
        
        }else if (ast instanceof Name){
            return PyCodeCompletion.TYPE_ATTR;
            
        }else if (ast instanceof Import || ast instanceof ImportFrom){
            return PyCodeCompletion.TYPE_IMPORT; 
        }
        
        return  PyCodeCompletion.TYPE_UNKNOWN;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.IToken#getRepresentation()
     */
    public String getRepresentation() {
        return representation;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.IToken#getDocStr()
     */
    public String getDocStr() {
        return docStr;
    }
    
    public SimpleNode getAst(){
        return ast;
    }
    
}
