/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/07/2005
 */
package com.python.pydev.fastparser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;

public class MemoVisitor extends VisitorBase {

    List<SimpleNode> visited = new ArrayList<SimpleNode>();

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        if (node instanceof Pass) {
            return null;
        }

        visited.add(node);
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        visited.add(node);
        exprType[] args = node.args.args;
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (args[i] instanceof Tuple) {
                    Tuple t = (Tuple) args[i];
                    for (int j = 0; j < t.elts.length; j++) {
                        t.elts[j].accept(this);
                    }
                } else {
                    args[i].accept(this);
                }
            }
        }
        return null;
    }

    public int size() {
        return visited.size();
    }

    @Override
    public boolean equals(Object obj) {

        MemoVisitor other = (MemoVisitor) obj;
        Iterator<SimpleNode> iter1 = other.visited.iterator();

        for (Iterator<SimpleNode> iter = visited.iterator(); iter.hasNext();) {
            SimpleNode n = (SimpleNode) iter.next();
            SimpleNode n1 = null;
            try {
                n1 = (SimpleNode) iter1.next();
            } catch (NoSuchElementException e) {
                throw new RuntimeException("Just received " + n, e);
            }

            if (n instanceof Expr && n1 instanceof Expr) {
                continue;
            }
            print(n.getClass());
            if (n.getClass().equals(n1.getClass()) == false) {
                print("n.getClass() != n1.getClass() " + n.getClass() + " != " + n1.getClass());
                return false;
            }
            //            if(n.beginColumn != n1.beginColumn){
            //                print("n = "+n+" n1 = "+n1);
            //                print("n = "+NodeUtils.getFullRepresentationString(n)+" n1 = "+NodeUtils.getFullRepresentationString(n1));
            //                print("n.beginColumn != n1.beginColumn "+ n.beginColumn +" != "+ n1.beginColumn);
            //                return false;
            //            }
            //            if(n.beginLine != n1.beginLine){
            //                print("n.beginLine != n1.beginLine "+ n.beginLine +" != "+ n1.beginLine);
            //                return false;
            //            }

            String s1 = NodeUtils.getFullRepresentationString(n);
            String s2 = NodeUtils.getFullRepresentationString(n1);
            if ((s1 == null && s2 != null) || (s1 != null && s2 == null)) {
                print("(s1 == null && s2 != null) || (s1 != null && s2 == null)");
                return false;
            }

            if (s1.equals(s2.replaceAll("\r", "")) == false) {
                print("s1 != s2 \n-->" + s1 + "<--\n!=\n-->" + s2 + "<--");
                return false;
            }
        }

        return true;
    }

    private void print(Object string) {
        //        System.out.println(string);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<SimpleNode> iter = visited.iterator(); iter.hasNext();) {
            buffer.append(iter.next().toString());
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
