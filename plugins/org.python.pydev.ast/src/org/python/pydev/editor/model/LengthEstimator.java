/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 8, 2004
 */
package org.python.pydev.editor.model;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.Ellipsis;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Expression;
import org.python.pydev.parser.jython.ast.ExtSlice;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Interactive;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Repr;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Slice;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.Yield;

/**
 * LengthEstimator estimates a lenght of a node in characters.
 * 
 * We need this to properly determine length of Nodes in the model.
 * Jython's parser only gives us the start of the node.
 * The estimates in this file are heuristic.
 */
public class LengthEstimator extends VisitorBase {

    int length = 0;

    int getLength() {
        return length;
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    @Override
    public Object visitName(Name node) throws Exception {
        length += node.id.length();
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        length += ((NameTok) node.attr).id.length() + 1; // +1 for '.'
        node.traverse(this);
        return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        LengthEstimator e2 = new LengthEstimator();
        node.traverse(e2);
        length += e2.getLength();
        return null;
    }

    @Override
    public Object visitAssert(Assert node) throws Exception {
        //        System.out.println("lenVisitAssert:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        //        System.out.println("lenVisitAssign:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        //        System.out.println("lenVisitAugAssign:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        //        System.out.println("lenVisitBinOp:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        //        System.out.println("lenVisitBoolOp:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitBreak(Break node) throws Exception {
        //        System.out.println("lenVisitBreak:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        //        System.out.println("lenVisitClassDef:" + node.name);
        return null;
    }

    @Override
    public Object visitCompare(Compare node) throws Exception {
        //        System.out.println("lenVisitCompare:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitContinue(Continue node) throws Exception {
        //        System.out.println("lenVisitContinue:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitDelete(Delete node) throws Exception {
        //        System.out.println("lenVisitDelete:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitDict(Dict node) throws Exception {
        //        System.out.println("lenVisitDict:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitEllipsis(Ellipsis node) throws Exception {
        //        System.out.println("lenVisitEllipsis:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitExec(Exec node) throws Exception {
        //        System.out.println("lenVisitExec:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitExpr(Expr node) throws Exception {
        //        System.out.println("lenVisitExpr:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitExpression(Expression node) throws Exception {
        //        System.out.println("lenVisitExpression:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitExtSlice(ExtSlice node) throws Exception {
        //        System.out.println("lenVisitExtSlice:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitFor(For node) throws Exception {
        //        System.out.println("lenVisitFor:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        //        System.out.println("lenVisitFunctionDef:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        //        System.out.println("lenVisitGlobal:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitIf(If node) throws Exception {
        //        System.out.println("lenVisitIf:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        //        System.out.println("lenVisitImport:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        //        System.out.println("lenVisitImportFrom:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitIndex(Index node) throws Exception {
        //        System.out.println("lenVisitIndex:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitInteractive(Interactive node) throws Exception {
        //        System.out.println("lenVisitInteractive:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        //        System.out.println("lenVisitLambda:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitList(List node) throws Exception {
        //        System.out.println("lenVisitList:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        //        System.out.println("lenVisitListComp:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        //        System.out.println("lenVisitModule:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitNum(Num node) throws Exception {
        //        System.out.println("lenVisitNum:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        //        System.out.println("lenVisitPass:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        //        System.out.println("lenVisitPrint:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitRaise(Raise node) throws Exception {
        //        System.out.println("lenVisitRaise:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitRepr(Repr node) throws Exception {
        //        System.out.println("lenVisitRepr:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitReturn(Return node) throws Exception {
        //        System.out.println("lenVisitReturn:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitSlice(Slice node) throws Exception {
        //        System.out.println("lenVisitSlice:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitStr(Str node) throws Exception {
        //        System.out.println("lenVisitStr:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        //        System.out.println("lenVisitSubscript:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitSuite(Suite node) throws Exception {
        //        System.out.println("lenVisitSuite:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        //        System.out.println("lenVisitTryExcept:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        //        System.out.println("lenVisitTryFinally:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        //        System.out.println("lenVisitTuple:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        //        System.out.println("lenVisitUnaryOp:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitWhile(While node) throws Exception {
        //        System.out.println("lenVisitWhile:" + node.toString(""));
        return null;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        //        System.out.println("lenVisitYield:" + node.toString(""));
        return null;
    }
}
