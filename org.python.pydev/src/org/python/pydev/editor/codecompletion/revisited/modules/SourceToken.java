/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.AbstractToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * @author Fabio Zadrozny
 */
public class SourceToken extends AbstractToken{

    private static final long serialVersionUID = 1L;
    
    /**
     * The AST that generated this SourceToken
     */
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

    /**
     * @return line starting at 1
     */
    public int getLineDefinition() {
        return NodeUtils.getLineDefinition(ast);
    }
    
    /**
     * @return col starting at 1
     */
    public int getColDefinition() {
        return NodeUtils.getColDefinition(ast);
    }

    int[] colLineEndToFirstDot;
    int[] colLineEndComplete;
    public int getLineEnd(boolean getOnlyToFirstDot){
    	if(getOnlyToFirstDot){
    		if(colLineEndToFirstDot == null){
    			colLineEndToFirstDot = NodeUtils.getColLineEnd(getAst(), getOnlyToFirstDot);
    		}
    		return colLineEndToFirstDot[0];
    		
    	}else{
    		if(colLineEndComplete == null){
    			colLineEndComplete = NodeUtils.getColLineEnd(getAst(), getOnlyToFirstDot);
    		}
    		return colLineEndComplete[0];
    	}
    }
    
    public int getColEnd(boolean getOnlyToFirstDot){
    	if(getOnlyToFirstDot){
    		if(colLineEndToFirstDot == null){
    			colLineEndToFirstDot = NodeUtils.getColLineEnd(getAst(), getOnlyToFirstDot);
    		}
    		return colLineEndToFirstDot[1];
    		
    	}else{
    		if(colLineEndComplete == null){
    			colLineEndComplete = NodeUtils.getColLineEnd(getAst(), getOnlyToFirstDot);
    		}
    		return colLineEndComplete[1];
    	}
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

    public boolean isImport() {
        SimpleNode ast = getAst();
        if(ast instanceof Import || ast instanceof ImportFrom){
            return true;
        }

        return false;
    }
    
    public boolean isImportFrom() {
    	return getAst() instanceof ImportFrom;
    }

    public boolean isWildImport() {
    	return AbstractVisitor.isWildImport(getAst());
    }
    /**
     * This representation may not be accurate depending on which tokens we are dealing with. 
     */
    public int[] getLineColEnd() {
    	if(ast instanceof NameTok || ast instanceof Name){
    		//those are the ones that we can be certain of...
    		return new int[]{getLineDefinition(), getColDefinition()+getRepresentation().length()};
    	}
    	throw new RuntimeException("Unable to get the lenght of the token:"+ast.getClass().getName());
    }

}
