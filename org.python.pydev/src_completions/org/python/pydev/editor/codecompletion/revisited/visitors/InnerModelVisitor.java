/*
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;

/**
 * This class is used to visit the inner context of class or a function. 
 * 
 * @author Fabio Zadrozny
 */
public class InnerModelVisitor extends AbstractVisitor {

    /**
     * List that contains heuristics to find attributes.
     */
    private List attrsHeuristics = new ArrayList();

    public InnerModelVisitor(String moduleName, ICompletionState state){
    	this.moduleName = moduleName;
        attrsHeuristics.add(new HeuristicFindAttrs(HeuristicFindAttrs.WHITIN_METHOD_CALL, HeuristicFindAttrs.IN_KEYWORDS, "properties.create", moduleName, state));
        attrsHeuristics.add(new HeuristicFindAttrs(HeuristicFindAttrs.WHITIN_ANY	    , HeuristicFindAttrs.IN_ASSIGN  , "", moduleName, state));
    }
    
    /**
     * This should be changed as soon as we know what should we visit.
     */
    private static int VISITING_NOTHING = -1;
    
    /**
     * When visiting class, get attributes and methods
     */
    private static int VISITING_CLASS = 0;
    
    /**
     * Initially, we're visiting nothing.
     */
    private int visiting = VISITING_NOTHING;
    
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
     */
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    public Object visitClassDef(ClassDef node) throws Exception {
        if(visiting == VISITING_NOTHING){
            visiting = VISITING_CLASS;
	        node.traverse(this);
	        
        }else if(visiting == VISITING_CLASS){ 
            //that's a class within the class we're visiting
            addToken(node);
        }
        
        return null;
    }

    public Object visitFunctionDef(FunctionDef node) throws Exception {
        if(visiting == VISITING_CLASS){
            addToken(node);
            
            //iterate heuristics to find attributes
            for (Iterator iter = attrsHeuristics.iterator(); iter.hasNext();) {
                HeuristicFindAttrs element = (HeuristicFindAttrs) iter.next();
                element.visitFunctionDef(node);
                addElementTokens(element);
            }

        }
        return null;
    }
        
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        if(visiting == VISITING_CLASS){
            
            //iterate heuristics to find attributes
	        for (Iterator iter = attrsHeuristics.iterator(); iter.hasNext();) {
	            HeuristicFindAttrs element = (HeuristicFindAttrs) iter.next();
	            element.visitAssign(node);
	            addElementTokens(element);
	        }
        }
        return null;
    }
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitCall(org.python.pydev.parser.jython.ast.Call)
     */
    public Object visitCall(Call node) throws Exception {
        if(visiting == VISITING_CLASS){
            
            //iterate heuristics to find attributes
	        for (Iterator iter = attrsHeuristics.iterator(); iter.hasNext();) {
	            HeuristicFindAttrs element = (HeuristicFindAttrs) iter.next();
	            element.visitCall(node);
	            addElementTokens(element);
	        }
        }
        return null;
    }
    
    /**
     * @param element
     */
    private void addElementTokens(HeuristicFindAttrs element) {
        tokens.addAll(element.tokens);
        element.tokens.clear();
    }

    
}
