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

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * This class is used so that after traversing the AST, we have a simple structure for navigating
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
public class DefinitionsASTIteratorVisitor extends EasyASTIteratorVisitor {

    @Override
    public Object visitAssign(Assign node) throws Exception {
        return visitAssign(this, node);
    }

    public static Object visitAssign(EasyAstIteratorBase visitor, Assign node) throws Exception {
        return visitAssign(visitor, node, true);
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitAssign(org.python.pydev.parser.jython.ast.Assign)
     */
    public static Object visitAssign(EasyAstIteratorBase visitor, Assign node, boolean visitUnhandledAndTraverse)
            throws Exception {
        visitTargetsInAssign(visitor, node.targets);

        if (visitUnhandledAndTraverse) {
            Object ret = visitor.unhandled_node(node);
            visitor.traverse(node);
            return ret;
        } else {
            return null;
        }

    }

    /**
     * Given a visitor and the targets found in an assign, visit them to find class attributes / instance variables.
     * 
     * @param visitor the visitor
     * @param targets the expressions in the target
     */
    private static void visitTargetsInAssign(EasyAstIteratorBase visitor, exprType[] targets) {
        if (targets == null) {
            return;
        }
        for (int i = 0; i < targets.length; i++) {
            exprType t = targets[i];
            if (t instanceof Tuple) {
                Tuple tuple = (Tuple) t;
                visitTargetsInAssign(visitor, tuple.elts);
            }
            visitTargetInAssign(visitor, t);
        }
    }

    /**
     * Visit a single target found in an assign to create a class attributes / instance variables if possible. 
     * @param visitor the visitor
     * @param t the expression to visit
     */
    private static void visitTargetInAssign(EasyAstIteratorBase visitor, exprType t) {
        if (t instanceof Name) {
            //we are in the class declaration
            if (visitor.isInClassDecl() || visitor.isInGlobal()) {
                //add the attribute for the class
                visitor.atomic(t);
            }

        } else if (t instanceof Attribute) {

            //we are in a method from the class
            if (visitor.isInClassMethodDecl()) {
                Attribute a = (Attribute) t;
                if (a.value instanceof Name) {

                    //it is an instance variable attribute
                    Name n = (Name) a.value;
                    if (n.id.equals("self")) {
                        visitor.atomic(t);
                    }
                }

            } else if (visitor.isInClassDecl() || visitor.isInGlobal()) {
                //add the attribute for the class 
                visitor.atomic(t);
            }
        }
    }

    /**
     * Creates the iterator and traverses the passed root so that the results can be gotten.
     */
    public static DefinitionsASTIteratorVisitor create(SimpleNode root) {
        if (root == null) {
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

    /**
     * Creates the iterator and traverses the passed root so that the results can be gotten.
     */
    public static DefinitionsASTIteratorVisitor createForChildren(ClassDef root) {
        if (root == null) {
            return null;
        }
        DefinitionsASTIteratorVisitor visitor = new DefinitionsASTIteratorVisitor();
        try {
            stmtType[] body = root.body;
            if (body != null) {
                for (int i = 0; i < body.length; i++) {
                    if (body[i] != null) {
                        body[i].accept(visitor);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

}
