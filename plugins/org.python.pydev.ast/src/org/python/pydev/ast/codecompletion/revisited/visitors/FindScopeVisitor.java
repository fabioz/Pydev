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
package org.python.pydev.ast.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.model.ISimpleNode;
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
    public ILocalScope scope;

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

    private ISimpleNode lastDef;

    private IModule module;

    /**
     * Only for subclasses
     */
    protected FindScopeVisitor(IPythonNature nature, IModule module) {
        super(nature, module);
        scope = new LocalScope(nature, new FastStack<SimpleNode>(20), module);
        this.module = module;
    }

    /**
     * Constructor
     *
     * @param line in ast coords (starts at 1)
     * @param col in ast coords (starts at 1)
     */
    public FindScopeVisitor(int line, int col, IPythonNature nature, IModule module) {
        this(nature, module);
        this.line = line;
        this.col = col;
        this.module = module;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        //the line passed in starts at 1 and the lines for the visitor nodes start at 0
        if (!found && !(node instanceof Module)) {
            if (line <= node.beginLine) {
                //scope is locked at this time.
                onScopeFound(node);
            }
        } else {
            if (scope.getScopeEndLine() == -1 && line < node.beginLine && col >= node.beginColumn) {
                scope.setScopeEndLine(node.beginLine);
            }
        }
        return node;
    }

    /**
     * @param node
     */
    private void onScopeFound(SimpleNode node) {
        found = true;
        int original = scope.getIfMainLine();
        scope = new LocalScope(nature, this.stackScope.createCopy(), this.module);
        scope.setIfMainLine(original);
        scope.setFoundAtASTNode(node);
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
            this.lastDef = node;
            if (NodeUtils.isAfterNameEnd(node, line, col)) {
                stackScope.push(node);
                node.traverse(this);
                stackScope.pop();
            } else {
                unhandled_node(node); // Just handle it directly without adding to the stack.
            }
        }
        return null;
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        if (!found) {
            this.lastDef = node;
            if (NodeUtils.isAfterNameEnd(node, line, col)) {
                stackScope.push(node);
                node.traverse(this);
                stackScope.pop();
            } else {
                unhandled_node(node); // Just handle it directly without adding to the stack.
            }
        }
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        stackScope.push(node);
        super.visitModule(node);
        if (!found && col > 1) {
            // If it wasn't found, keep the last opened scope.
            if (this.lastDef != null) {
                SimpleNode n = (SimpleNode) this.lastDef;
                List<SimpleNode> lst = new ArrayList<>(3);
                while (n != null && !(n instanceof Module)) {
                    lst.add(n);
                    n = n.parent;
                }
                Collections.reverse(lst);
                for (SimpleNode n1 : lst) {
                    stackScope.push(n1);
                }
                onScopeFound(null);
            }
        }

        return null;
    }
}
