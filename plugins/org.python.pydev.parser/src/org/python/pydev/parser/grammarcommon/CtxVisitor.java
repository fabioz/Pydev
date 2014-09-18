/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Starred;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.shared_core.string.StringUtils;

public final class CtxVisitor extends Visitor {

    private int ctx;
    private JJTPythonGrammarState stack;

    public CtxVisitor(JJTPythonGrammarState stack) {
        this.stack = stack;
    }

    public void setParam(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Param;
        visit(node);
    }

    public void setKwOnlyParam(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.KwOnlyParam;
        visit(node);
    }

    public void setStore(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Store;
        visit(node);
    }

    public void setStore(SimpleNode[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) {
            setStore(nodes[i]);
        }
    }

    public void setDelete(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Del;
        visit(node);
    }

    public void setDelete(SimpleNode[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) {
            setDelete(nodes[i]);
        }
    }

    public void setAugStore(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.AugStore;
        visit(node);
    }

    @Override
    public Object visitName(Name node) throws Exception {
        if (ctx == expr_contextType.Store && node.reserved) {
            String msg = StringUtils.format("Cannot assign value to %s (because it's a keyword)", node.id);
            this.stack.getGrammar().addAndReport(new ParseException(msg, node), msg);
        } else {
            node.ctx = ctx;
        }
        return null;
    }

    @Override
    public Object visitStarred(Starred node) throws Exception {
        node.ctx = ctx;
        traverse(node);
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    @Override
    public Object visitList(List node) throws Exception {
        if (ctx == expr_contextType.AugStore) {
            String msg = "Augmented assign to list not possible";
            this.stack.getGrammar().addAndReport(new ParseException(msg, node), msg);
        } else {
            node.ctx = ctx;
        }
        traverse(node);
        return null;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        if (ctx == expr_contextType.AugStore) {
            String msg = "Augmented assign to tuple not possible";
            this.stack.getGrammar().addAndReport(new ParseException(msg, node), msg);
        } else {
            node.ctx = ctx;
        }
        traverse(node);
        return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        throw new ParseException("can't assign to function call", node);
    }

    public Object visitListComp(Call node) throws Exception {
        throw new ParseException("can't assign to list comprehension call", node);
    }

    @Override
    public Object unhandled_node(SimpleNode node) throws Exception {
        throw new ParseException("can't assign to operator:" + node, node);
    }
}
