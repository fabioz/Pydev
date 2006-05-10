package com.python.pydev.analysis.scopeanalysis;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.EasyAstIteratorBase;

/**
 * This class will be able to check for the names of the attribute and return those entries.
 * It will not check for attributes that start with 'self'.
 */
public class AttributeReferencesVisitor extends EasyAstIteratorBase{

	private boolean inAttr = false;
	
	protected Object unhandled_node(SimpleNode node) throws Exception {
		if(inAttr){
			if(node instanceof Name || node instanceof NameTok){
				atomic(node);
			}
		}
			
    	return super.unhandled_node(node);
    }
	
	@Override
	public Object visitAttribute(Attribute node) throws Exception {
		inAttr = true;
		String fullRepresentationString = NodeUtils.getFullRepresentationString(node);
		Object ret = null;
		
		if(!fullRepresentationString.startsWith("self")){
			ret = super.visitAttribute(node);
		}
		
		inAttr = false;
		return ret;
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
