/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors.scope;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterUtilsV2;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * This visitor will try to get the node where the extracted local should actually be added.
 */
public class GetNodeForExtractLocalVisitor extends VisitorBase {

    protected final FastStack<FastStack<SimpleNode>> contextStack = new FastStack<FastStack<SimpleNode>>(10);
    private int initialExtractLocalLine;
    private boolean keepGoing = true;
    private stmtType lastStmt;

    public GetNodeForExtractLocalVisitor(int initialExtractLocalLine) {
        this.initialExtractLocalLine = initialExtractLocalLine;
        contextStack.push(new FastStack<SimpleNode>(10)); //start with a stack.
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (!keepGoing) {
            return;
        }
        boolean multiLineStmt = false;

        if (node instanceof stmtType) {
            multiLineStmt = PrettyPrinterUtilsV2.isMultiLineStmt((stmtType) node);
            if (multiLineStmt) {
                contextStack.push(new FastStack<SimpleNode>(10));
            }
            lastStmt = (stmtType) node;
            node.traverse(this);
            if (multiLineStmt && keepGoing) {
                contextStack.pop();
            }

        } else {
            FastStack<SimpleNode> peek = contextStack.peek();
            boolean inExpr = peek.size() != 0;
            if (!inExpr && node instanceof exprType) {
                peek.push(node);
            }
            node.traverse(this);
            if (!inExpr && node instanceof exprType && keepGoing) {
                peek.pop();
            }
        }
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        if (node instanceof stmtType) {
            return null;
        }
        if (node.beginLine >= initialExtractLocalLine) {
            keepGoing = false;
        }
        return null;
    }

    public SimpleNode getLastInContextBeforePassedLine() {
        FastStack<SimpleNode> peeked = contextStack.peek();
        if (peeked.size() > 0) {
            SimpleNode expr = peeked.peek();
            if (lastStmt != null) {
                if (expr.beginLine < lastStmt.beginLine) {
                    return expr;
                } else {
                    return lastStmt;
                }
            }

        }
        return lastStmt;
    }

}
