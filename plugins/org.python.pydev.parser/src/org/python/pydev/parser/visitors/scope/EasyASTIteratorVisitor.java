/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import java.util.Iterator;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
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
public class EasyASTIteratorVisitor extends EasyAstIteratorBase {

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImport(org.python.pydev.parser.jython.ast.Import)
     */
    @Override
    public Object visitImport(Import node) throws Exception {
        atomic(node);
        return super.visitImport(node);
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImportFrom(org.python.pydev.parser.jython.ast.ImportFrom)
     */
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        atomic(node);
        return super.visitImportFrom(node);
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    @Override
    public Object visitAssign(Assign node) throws Exception {
        exprType[] targets = node.targets;
        for (int i = 0; i < targets.length; i++) {
            exprType t = targets[i];

            if (t instanceof Name) {
                //we are in the class declaration
                if (isInClassDecl()) {
                    //add the attribute for the class
                    atomic(t);
                }

            } else if (t instanceof Attribute) {

                //we are in a method from the class
                if (isInClassMethodDecl()) {
                    Attribute a = (Attribute) t;
                    if (a.value instanceof Name) {

                        //it is an instance variable attribute
                        Name n = (Name) a.value;
                        if (n.id.equals("self")) {
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
    public static EasyASTIteratorVisitor create(SimpleNode root) {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    public static Iterator<ASTEntry> createClassIterator(SimpleNode ast) {
        EasyASTIteratorVisitor visitor = create(ast);
        return visitor.getClassesIterator();
    }

}
