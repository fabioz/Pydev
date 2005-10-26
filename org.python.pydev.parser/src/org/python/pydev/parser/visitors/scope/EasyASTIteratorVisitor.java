/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.exprType;

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
public class EasyASTIteratorVisitor extends EasyAstIteratorBase{
    

    /** 
     * @see org.python.parser.ast.VisitorBase#visitImport(org.python.parser.ast.Import)
     */
    public Object visitImport(Import node) throws Exception {
        atomic(node);
        return super.visitImport(node);
    }

    /** 
     * @see org.python.parser.ast.VisitorBase#visitImportFrom(org.python.parser.ast.ImportFrom)
     */
    public Object visitImportFrom(ImportFrom node) throws Exception {
        atomic(node);
        return super.visitImportFrom(node);
    }
    

    /** 
     * @see org.python.parser.ast.VisitorBase#visitAssign(org.python.parser.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        exprType[] targets = node.targets;
        for (int i = 0; i < targets.length; i++) {
            exprType t = targets[i];
            
            if(t instanceof Name){
                //we are in the class declaration
                if(isInClassDecl()){
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
                }
            }
        }
        return super.visitAssign(node);
    }

    /**
     * Creates the iterator and transverses the passed root so that the results can be gotten.
     */
    public static EasyASTIteratorVisitor create(SimpleNode root){
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

}
