/*
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.Call;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Name;

/**
 * This class defines how we should find attributes. 
 * 
 * Heuristics provided allow someone to find an attr inside a function definition (IN_INIT or IN_ANY)
 * or inside a method call (e.g. a method called properties.create(x=0) - that's what I use, so, that's specific).
 * Other uses may be customized later, once we know which other uses are done.
 * 
 * @author Fabio Zadrozny
 */
public class HeuristicFindAttrs extends AbstractVisitor {

    /**
     * @param where
     * @param how
     * @param methodCall
     */
    public HeuristicFindAttrs(int where, int how, String methodCall) {
        this.where = where;
        this.how = how;
        this.methodCall = methodCall;
    }
    
    public static final int WHITIN_METHOD_CALL = 0;
    public static final int WHITIN_INIT = 1;
    public static final int WHITIN_ANY = 2;
    
    public int where = -1;
    
    
    public static final int IN_ASSIGN = 0;
    public static final int IN_KEYWORDS = 1;

    public int how = -1;
    
    private boolean entryPointCorrect = false;
    
    private boolean inAssing = false;
    
    /**
     * This is the method that can be used to declare them (e.g. properties.create)
     * It's only used it it is a method call.
     */
    public String methodCall = "";

    /**
     * @see org.python.parser.ast.VisitorBase#unhandled_node(org.python.parser.SimpleNode)
     */
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    /**
     * @see org.python.parser.ast.VisitorBase#traverse(org.python.parser.SimpleNode)
     */
    public void traverse(SimpleNode node) throws Exception {
    }
    
    
    //ENTRY POINTS
    /**
     * @see org.python.parser.ast.VisitorBase#visitCall(org.python.parser.ast.Call)
     */
    public Object visitCall(Call node) throws Exception {
        if(entryPointCorrect == false && methodCall.length() > 0){
	        entryPointCorrect = true;
	        String[] c = methodCall.split("\\.");
	        
	        
	        
	        if (node.func instanceof Attribute){
		        Attribute func = (Attribute)node.func;
		        if(func.attr.equals(c[1])){
		        
			        if(func.value instanceof Name){
			            Name name = (Name) func.value;
			            if(name.id.equals(c[0])){
			                for (int i=0; i<node.keywords.length; i++){
			                    addToken(node.keywords[i]);
			                }
			            }
			        }
		        }
	        }
	        
	        entryPointCorrect = false;
        }
        return null;
    }

    /**
     * @see org.python.parser.ast.VisitorBase#visitFunctionDef(org.python.parser.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        
        if(entryPointCorrect == false){
	        entryPointCorrect = true;
	        
	        if(where == WHITIN_ANY){
	            node.traverse(this);
	        
	        } else if(where == WHITIN_INIT && node.name.equals("__init__")){
	            node.traverse(this);
	        }
	        entryPointCorrect = false;
        } 
        
        
        return null;
    }
    //END ENTRY POINTS
    
    
    
    /**
     * 
     */
    private void checkEntryPoint() {
        if(entryPointCorrect == false)
            throw new RuntimeException("Invalid entry point. ");
    }

    
    /**
     * Name should be whithin assign.
     * 
     * @see org.python.parser.ast.VisitorIF#visitAssign(org.python.parser.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        checkEntryPoint();
        if(how == IN_ASSIGN){
            inAssing = true;
            node.traverse(this);
            inAssing = false;
        }
        return null;
    }
    
    /**
     * @see org.python.parser.ast.VisitorBase#visitAttribute(org.python.parser.ast.Attribute)
     */
    public Object visitAttribute(Attribute node) throws Exception {
        if(how == IN_ASSIGN && inAssing){
            if(node.value instanceof Name){
                String id = ((Name)node.value).id;
                if(id != null && id.equals("self")){
                    addToken(node);
                }
            }
        }
        return null;
    }

}
