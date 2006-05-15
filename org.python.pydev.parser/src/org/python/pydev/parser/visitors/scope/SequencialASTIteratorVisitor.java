package org.python.pydev.parser.visitors.scope;

import org.python.pydev.parser.jython.SimpleNode;

public class SequencialASTIteratorVisitor extends EasyAstIteratorBase{

    protected Object unhandled_node(SimpleNode node) throws Exception {
    	atomic(node);
    	super.unhandled_node(node);
    	return null;
    }
    
    /**
     * Creates the iterator and transverses the passed root so that the results can be gotten.
     */
    public static SequencialASTIteratorVisitor create(SimpleNode root){
        SequencialASTIteratorVisitor visitor = new SequencialASTIteratorVisitor();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }
    

}
