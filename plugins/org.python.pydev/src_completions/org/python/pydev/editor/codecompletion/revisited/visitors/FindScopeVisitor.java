/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 20, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * @author Fabio Zadrozny
 */
public class FindScopeVisitor extends AbstractVisitor {

    /**
     * Stack of classes / methods representing the scope.
     */
    protected FastStack<SimpleNode> stackScope = new FastStack<SimpleNode>(20);

    /**
     * This is the scope.
     */
    public ILocalScope scope = new LocalScope(new FastStack<SimpleNode>(20));

    /**
     * Variable to mark if we found scope.
     */
    protected boolean found = false;

    /**
     * line to find
     */
    private int line;

    /**
     * column to find
     */
    private int col;

    /**
     * Only for subclasses
     */
    protected FindScopeVisitor() {

    }

    /**
     * Constructor
     *
     * @param line in ast coords (starts at 1)
     * @param col in ast coords (starts at 1)
     */
    public FindScopeVisitor(int line, int col) {
        this.line = line;
        this.col = col;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        //the line passed in starts at 1 and the lines for the visitor nodes start at 0
        if (!found && !(node instanceof Module || node instanceof Pass)) {
            if (line <= node.beginLine) {
                //scope is locked at this time.
                found = true;
                int original = scope.getIfMainLine();
                scope = new LocalScope(this.stackScope.createCopy());
                scope.setIfMainLine(original);
                scope.setFoundAtASTNode(node);
            }
        } else {
            if (scope.getScopeEndLine() == -1 && line < node.beginLine && col >= node.beginColumn) {
                scope.setScopeEndLine(node.beginLine);
            }
        }
        return node;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitIf(org.python.pydev.parser.jython.ast.If)
     */
    @Override
    public Object visitIf(If node) throws Exception {
        checkIfMainNode(node);
        return super.visitIf(node);
    }

    /**
     * Checks if we found an 'if' main node
     */
    protected void checkIfMainNode(If node) {
        boolean isIfMainNode = NodeUtils.isIfMAinNode(node);
        if (isIfMainNode) {
            scope.setIfMainLine(node.beginLine);
        }
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitClassDef(org.python.pydev.parser.jython.ast.ClassDef)
     */
    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        if (!found) {
            stackScope.push(node);
            node.traverse(this);
            stackScope.pop();
        }
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        if (!found) {
            stackScope.push(node);
            node.traverse(this);
            stackScope.pop();
        }
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        stackScope.push(node);
        return super.visitModule(node);
    }
}
