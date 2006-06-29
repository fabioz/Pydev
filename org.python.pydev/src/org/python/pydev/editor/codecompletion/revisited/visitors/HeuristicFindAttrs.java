/*
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.exprType;

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
    public HeuristicFindAttrs(int where, int how, String methodCall, String moduleName) {
        this.where = where;
        this.how = how;
        this.methodCall = methodCall;
        this.moduleName = moduleName;
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
    private boolean inFuncDef = false;
    
    /**
     * This is the method that can be used to declare them (e.g. properties.create)
     * It's only used it it is a method call.
     */
    public String methodCall = "";

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
    }
    
    
    
    //ENTRY POINTS
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitCall(org.python.pydev.parser.jython.ast.Call)
     */
    public Object visitCall(Call node) throws Exception {
        if(entryPointCorrect == false && methodCall.length() > 0){
	        entryPointCorrect = true;
	        String[] c = FullRepIterable.dotSplit(methodCall);
	        
	        
	        
	        if (node.func instanceof Attribute){
		        Attribute func = (Attribute)node.func;
		        if(((NameTok)func.attr).id.equals(c[1])){
		        
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
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        
        if(entryPointCorrect == false){
	        entryPointCorrect = true;
	        inFuncDef = true;
	        
	        if(where == WHITIN_ANY){
	            node.traverse(this);
	        
	        } else if(where == WHITIN_INIT && node.name.equals("__init__")){
	            node.traverse(this);
	        }
	        entryPointCorrect = false;
	        inFuncDef = false;
        } 
        
        
        return null;
    }
    //END ENTRY POINTS
    
    
    
    
    /**
     * Name should be whithin assign.
     * 
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        if(how == IN_ASSIGN){
            inAssing = true;
            
            for (int i = 0; i < node.targets.length; i++) {
                if(node.targets[i] instanceof Attribute){
                    visitAttribute((Attribute)node.targets[i]);
                    
                }else if(node.targets[i] instanceof Name && inFuncDef == false){
                    String id = ((Name)node.targets[i]).id;
                    if(id != null){
                        addToken(node.targets[i]);
                    }
                    
                }else if(node.targets[i] instanceof Tuple && inFuncDef == false){
                	//that's for finding the definition: a,b,c = range(3) inside a class definition
                	Tuple tuple = (Tuple) node.targets[i];
                	for(exprType t :tuple.elts){
                		if(t instanceof Name){
                			String id = ((Name)t).id;
                			if(id != null){
                				addToken(t);
                			}
                		}
                	}
                	
                }
            }
            
            inAssing = false;
        }
        return null;
    }
    
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAttribute(org.python.pydev.parser.jython.ast.Attribute)
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
    

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorIF#visitIf(org.python.pydev.parser.jython.ast.If)
     */
    public Object visitIf(If node) throws Exception {
        node.traverse(this);
        return null;
    }



}
