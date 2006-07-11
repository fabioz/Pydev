package com.python.pydev.analysis.scopeanalysis;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.scope.EasyAstIteratorBase;

/**
 * This class will be able to check for the names of the attribute and return those entries.
 * It will not check for attributes that start with 'self'.
 */
public class AttributeReferencesVisitor extends EasyAstIteratorBase{

	private int inAttr = 0;
	
	protected Object unhandled_node(SimpleNode node) throws Exception {
        //System.out.println("unhandled_node:"+node);
		if(inAttr > 0 || isInClassDecl()){
			if(node instanceof Name || node instanceof NameTok){
				atomic(node);
			}
		}
			
    	return super.unhandled_node(node);
    }
	
	@Override
	public Object visitAttribute(Attribute node) throws Exception {
	    Object ret = null;
		inAttr += 1;
        try{
    		
    		ret = super.visitAttribute(node);
        }finally{    		
            inAttr -= 1;
        }
		return ret;
	}
    
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        atomic(node.name);
        return super.visitFunctionDef(node);
    }
    
    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        atomic(node.name);
        return super.visitClassDef(node);
    }

	

    /**
     * Creates the iterator and transverses the passed root so that the results can be gotten.
     */
    public static AttributeReferencesVisitor create(SimpleNode root){
        AttributeReferencesVisitor visitor = new AttributeReferencesVisitor();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }
    
}
