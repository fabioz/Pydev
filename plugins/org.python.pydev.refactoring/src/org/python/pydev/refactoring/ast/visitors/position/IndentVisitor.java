/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.visitors.position;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.GeneratorExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.VisitorBase;

public class IndentVisitor extends VisitorBase {

    private static final int DEFAULT_INDENT = 4;

    private int indent;

    public IndentVisitor() {
        this.indent = DEFAULT_INDENT;
    }

    public int getIndent() {
        return indent;
    }

    private void handleDefault(SimpleNode node) {
        // For a node that is indented one level, beginColumn is 5 (assuming an
        // indentation of 4 spaces), so subtract 1.
        this.indent = node.beginColumn - 1;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        // ignore
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        handleDefault(node);
        return null;
    }

    public void visit(SimpleNode node) throws Exception {
        if (node != null) {
            node.accept(this);
        }
    }

    @Override
    public Object visitAssert(Assert node) throws Exception {
        handleDefault(node);
        this.indent -= 7;
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        visit(node.targets[0]);
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        visit(node.value);
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        visit(node.target);
        return null;
    }

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        visit(node.left);
        return null;
    }

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        visit(node.values[0]);
        return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        visit(node.func);
        return null;
    }

    @Override
    public Object visitCompare(Compare node) throws Exception {
        visit(node.left);
        return null;
    }

    @Override
    public Object visitDelete(Delete node) throws Exception {
        handleDefault(node);
        this.indent -= 4;
        return null;
    }

    @Override
    public Object visitExec(Exec node) throws Exception {
        visit(node);
        this.indent -= 5;
        return null;
    }

    @Override
    public Object visitExpr(Expr node) throws Exception {
        visit(node.value);
        return null;
    }

    @Override
    public Object visitGeneratorExp(GeneratorExp node) throws Exception {
        visit(node.elt);
        return null;
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        this.indent = node.beginColumn;
        this.indent -= "import ".length();
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        this.indent = node.beginColumn;
        this.indent -= "from ".length();
        return null;
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        handleDefault(node);
        this.indent -= 6;
        return null;
    }

    @Override
    public Object visitSuite(Suite node) throws Exception {
        visit(node.body[0]);
        return null;
    }

    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        handleDefault(node);
        return null;
    }

}
