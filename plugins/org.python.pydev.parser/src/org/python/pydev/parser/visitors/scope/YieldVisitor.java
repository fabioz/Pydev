/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors.scope;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.stmtType;

public class YieldVisitor extends VisitorBase {

    public static List<Yield> findYields(FunctionDef functionDef) {
        YieldVisitor visitor = new YieldVisitor();
        if (functionDef == null) {
            return visitor.ret;
        }
        stmtType[] body = functionDef.body;
        if (body == null) {
            return visitor.ret;
        }

        try {
            int len = body.length;
            for (int i = 0; i < len; i++) {
                stmtType b = body[i];
                if (b != null) {
                    b.accept(visitor);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor.ret;
    }

    private ArrayList<Yield> ret = new ArrayList<Yield>(3); //Start considering 3 yields.

    @Override
    public Object visitYield(Yield node) throws Exception {
        ret.add(node);
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        return null;
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

}
