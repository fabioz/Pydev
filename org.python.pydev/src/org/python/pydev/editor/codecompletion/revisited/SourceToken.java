/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

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
public class SourceToken extends AbstractToken{

    /**
     * @param node
     */
    public SourceToken(SimpleNode node, String rep, String doc, String parentPackage) {
        super(rep, doc, parentPackage, getType(node));
        this.ast = node;
    }
    private SimpleNode ast;
    
    /**
     * 
     * @return the completion type depending on the syntax tree.
     */
    public static int getType(SimpleNode ast){
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

    
    public SimpleNode getAst(){
        return ast;
    }
    
}
