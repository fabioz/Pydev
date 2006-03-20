/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import java.util.Iterator;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.exprType;

/**
 * This class is used so that after transversing the AST, we have a simple structure for navigating
 * upon its nodes;
 *
 * This structure should provide:
 * - Imports
 * - Classes (and attributes)
 * - Methods
 * 
 * 
 * 
 * Note: it does not only provide global information, but also inner information, such as methods from a class.
 * 
 * @author Fabio
 */
public class DefinitionsASTIteratorVisitor extends EasyASTIteratorVisitor{
    
    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        exprType[] targets = node.targets;
        for (int i = 0; i < targets.length; i++) {
            exprType t = targets[i];
            
            if(t instanceof Name){
                //we are in the class declaration
                if(isInClassDecl() || isInGlobal()){
                    //add the attribute for the class
                    atomic(t);
                }
                
            }else if(t instanceof Attribute){
                
                //we are in a method from the class
                if(isInClassMethodDecl()){
                    Attribute a = (Attribute) t;
                    if(a.value instanceof Name){
                        
                        //it is an instance variable attribute
                        Name n = (Name) a.value;
                        if (n.id.equals("self")){
		                    atomic(t);
                        }
                    }
                    
                }else if(isInClassDecl() || isInGlobal()){
                    //add the attribute for the class 
                    atomic(t);
                }
            }
        }
//        return VisitorBase.visitAssign(node);
        Object ret = unhandled_node(node);
        traverse(node);
        return ret;

    }

    /**
     * Creates the iterator and transverses the passed root so that the results can be gotten.
     */
    public static DefinitionsASTIteratorVisitor create(SimpleNode root){
        if(root == null){
            return null;
        }
        DefinitionsASTIteratorVisitor visitor = new DefinitionsASTIteratorVisitor();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    public Iterator<ASTEntry> getOutline() {
        return getIterator(new Class[]{ClassDef.class, FunctionDef.class, Attribute.class, Name.class});
    }

}
