/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.keywordType;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.AbstractToken;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * @author Fabio Zadrozny
 */
public class SourceToken extends AbstractToken{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private SimpleNode ast;

    /**
     * @param node
     */
    public SourceToken(SimpleNode node, String rep, String args, String doc, String parentPackage) {
        super(rep, doc, args, parentPackage, getType(node));
        this.ast = node;
    }

    /**
     * @param node
     */
    public SourceToken(SimpleNode node, String rep, String args, String doc, String parentPackage, int type) {
        super(rep, doc, args, parentPackage, type);
        this.ast = node;
    }

    /**
     * @param node
     */
    public SourceToken(SimpleNode node, String rep, String doc, String args, String parentPackage, String originalRep) {
        super(rep, doc, args, parentPackage, getType(node), originalRep);
        this.ast = node;
    }
    
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

        }else if (ast instanceof keywordType){
            return PyCodeCompletion.TYPE_ATTR; 
        
        }else if (ast instanceof Attribute){
            return PyCodeCompletion.TYPE_ATTR; 
        }
        
        return  PyCodeCompletion.TYPE_UNKNOWN;
    }

    
    public SimpleNode getAst(){
        return ast;
    }

    public int getLineDefinition() {
        return NodeUtils.getLineDefinition(ast);
    }
    
    public int getColDefinition() {
        return NodeUtils.getColDefinition(ast);
    }

    int[] colLineEnd;
    public int getLineEnd(){
        if(colLineEnd == null){
            colLineEnd = NodeUtils.getColLineEnd(getAst());
        }
        return colLineEnd[0];
    }
    
    public int getColEnd(){
        if(colLineEnd == null){
            colLineEnd = NodeUtils.getColLineEnd(getAst());
        }
        return colLineEnd[1];
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof SourceToken))
            return false;
        
        SourceToken s = (SourceToken) obj;
        
        if(!s.getRepresentation().equals(getRepresentation()))
            return false;
        if(s.getLineDefinition() != getLineDefinition())
            return false;
        if(s.getColDefinition() != getColDefinition())
            return false;
        
        return true;
    }
    
    @Override
    public int hashCode() {
        return 7*getLineDefinition()*getColDefinition();
    }
}
