/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Ellipsis;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Index;
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
import org.python.pydev.parser.jython.ast.Set;
import org.python.pydev.parser.jython.ast.SetComp;
import org.python.pydev.parser.jython.ast.Slice;
import org.python.pydev.parser.jython.ast.Starred;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.StrJoin;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.WithItem;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;

public class MakeAstValidForPrettyPrintingVisitor extends VisitorBase {

    int currentLine = 0;
    int currentCol = 0;
    private int isInMultiLine;

    private void nextLine() {
        nextLine(false);
    }

    private void nextLine(boolean force) {
        //on a multi-line statement, we don't need to add tokens in new lines.
        if (force || isInMultiLine == 0) {
            currentLine += 1;
            currentCol = 0;
        }
    }

    private void nextCol() {
        currentCol += 1;
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        throw new RuntimeException("Unhandled: " + node);
    }

    @Override
    public Object visitIndex(Index node) throws Exception {
        fixNode(node);
        traverse(node);
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        fixNode(node);
        traverse(node);
        fixAfterNode(node);
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    protected void fixAfterNode(SimpleNode node) {
        if (node.specialsAfter != null) {
            for (Object o : node.specialsAfter) {
                if (o instanceof commentType) {
                    fixNode((SimpleNode) o);
                    nextLine();
                }
            }
        }
    }

    protected void fixNode(SimpleNode node) {
        if (node instanceof stmtType) {
            nextLine();
        }
        if (node.specialsBefore != null) {
            for (Object o : node.specialsBefore) {
                if (o instanceof commentType) {
                    fixNode((SimpleNode) o);
                    nextLine();
                }
            }
        }

        if (node.beginLine < currentLine) {
            node.beginLine = currentLine;
            node.beginColumn = currentCol;

        } else if (node.beginLine == currentLine && node.beginColumn < currentCol) {
            node.beginColumn = currentCol;

        } else {
            currentLine = node.beginLine;
            currentCol = node.beginColumn;
        }

        if (node instanceof stmtType) {
            nextCol();
        }
    }

    public static void makeValid(SimpleNode node) throws Exception {
        node.accept(new MakeAstValidForPrettyPrintingVisitor());
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        fixNode(node);

        for (int i = 0; i < node.targets.length; i++) {
            exprType target = node.targets[i];
            target.accept(this);
            nextCol();
        }

        node.value.accept(this);
        nextCol();
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        fixNode(node);
        node.target.accept(this);
        nextCol();
        node.value.accept(this);
        nextCol();
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        fixNode(node);
        node.left.accept(this);
        nextCol();
        node.right.accept(this);
        nextCol();
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        fixNode(node);
        node.operand.accept(this);
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        fixNode(node);
        for (int i = 0; i < node.values.length; i++) {
            node.values[i].accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitCompare(Compare node) throws Exception {
        fixNode(node);
        node.left.accept(this);

        for (int i = 0; i < node.comparators.length; i++) {
            node.comparators[i].accept(this);
        }
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitEllipsis(Ellipsis node) throws Exception {
        fixNode(node);
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitDict(Dict node) throws Exception {
        fixNode(node);
        this.pushInMultiline();
        exprType[] keys = node.keys;
        exprType[] values = node.values;
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {

            }
            keys[i].accept(this);

            values[i].accept(this);

        }
        this.popInMultiline();
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        fixNode(node);
        this.pushInMultiline();
        if (node.elts != null && node.elts.length > 0) {
            //tuple inside tuple
            visitCommaSeparated(node.elts, node.endsWithComma);

        }
        this.popInMultiline();
        fixAfterNode(node);
        return null;
    }

    private void pushInMultiline() {
        this.isInMultiLine += 1;
    }

    private void popInMultiline() {
        this.isInMultiLine -= 1;
    }

    private void visitCommaSeparated(exprType[] elts, boolean requireEndWithCommaSingleElement) throws Exception {
        if (elts != null) {
            for (int i = 0; i < elts.length; i++) {
                if (elts[i] != null) {
                    elts[i].accept(this);
                }
            }
        }
    }

    @Override
    public Object visitList(List node) throws Exception {
        fixNode(node);
        this.pushInMultiline();
        visitCommaSeparated(node.elts, false);
        this.popInMultiline();
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        fixNode(node);

        node.elt.accept(this);

        for (SimpleNode c : node.generators) {
            c.accept(this);
        }
        fixAfterNode(node);

        return null;
    }

    @Override
    public Object visitSetComp(SetComp node) throws Exception {
        fixNode(node);

        node.elt.accept(this);
        for (comprehensionType c : node.generators) {
            c.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitDictComp(DictComp node) throws Exception {
        fixNode(node);

        node.key.accept(this);

        node.value.accept(this);
        for (comprehensionType c : node.generators) {
            c.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitSet(Set node) throws Exception {
        fixNode(node);
        visitCommaSeparated(node.elts, false);
        fixAfterNode(node);
        return null;
    }

    private SimpleNode[] reverseNodeArray(SimpleNode[] expressions) {
        java.util.List<SimpleNode> ifs = new ArrayList<SimpleNode>(Arrays.asList(expressions));
        Collections.reverse(ifs);
        SimpleNode[] ifsInOrder = ifs.toArray(new SimpleNode[0]);
        return ifsInOrder;
    }

    @Override
    public Object visitComprehension(Comprehension node) throws Exception {
        fixNode(node);
        node.target.accept(this);

        node.iter.accept(this);

        for (SimpleNode s : reverseNodeArray(node.ifs)) {
            s.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitWhile(While node) throws Exception {

        fixNode(node);
        node.test.accept(this);

        if (node.body == null || node.body.length == 0) {
            node.body = new stmtType[] { new Pass() };
        }

        for (SimpleNode n : node.body) {
            n.accept(this);
        }
        endSuiteWithOrElse(node.orelse);

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitSuite(Suite node) throws Exception {
        fixNode(node);
        for (SimpleNode n : node.body) {
            n.accept(this);
        }
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitWith(With node) throws Exception {

        fixNode(node);
        for (SimpleNode n : node.with_item) {
            n.accept(this);
        }

        if (node.body != null) {
            node.body.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitWithItem(WithItem node) throws Exception {
        fixNode(node);

        traverse(node);

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitFor(For node) throws Exception {
        //for a in b: xxx else: yyy

        //a

        fixNode(node);

        node.target.accept(this);

        //in b
        node.iter.accept(this);

        if (node.body == null || node.body.length == 0) {
            node.body = new stmtType[] { new Pass() };
        }

        for (SimpleNode n : node.body) {
            n.accept(this);
        }
        suiteType orelse = node.orelse;
        endSuiteWithOrElse(orelse);

        fixAfterNode(node);
        return null;
    }

    private void endSuiteWithOrElse(suiteType orelse) throws Exception {
        if (orelse != null) {
            visitOrElsePart(orelse, "else");
        }
    }

    @Override
    public Object visitRepr(Repr node) throws Exception {
        fixNode(node);
        traverse(node);
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitReturn(Return node) throws Exception {
        fixNode(node);
        node.traverse(this);
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitDelete(Delete node) throws Exception {
        fixNode(node);
        node.traverse(this);
        fixAfterNode(node);
        return null;
    }

    public void visitTryPart(SimpleNode node, stmtType[] body) throws Exception {
        //try:
        fixNode(node);

        for (stmtType st : body) {
            st.accept(this);
        }

        fixAfterNode(node);
    }

    public void visitOrElsePart(suiteType orelse, String expectedToken) throws Exception {

        if (orelse != null) {
            fixNode(orelse);
            for (stmtType st : ((Suite) orelse).body) {
                st.accept(this);
            }
            fixAfterNode(orelse);
        }

    }

    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        if (node.body == null || node.body.length == 0) {
            node.body = new stmtType[] { new Pass() };
        }

        visitTryPart(node, node.body);
        visitOrElsePart(node.finalbody, "finally");
        return null;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        if (node.body == null || node.body.length == 0) {
            node.body = new stmtType[] { new Pass() };
        }

        visitTryPart(node, node.body);
        for (excepthandlerType h : node.handlers) {

            fixNode(h);

            if (h.type != null) {
                h.type.accept(this);
            }
            if (h.name != null) {
                h.name.accept(this);
            }

            if (h.body == null || h.body.length == 0) {
                h.body = new stmtType[] { new Pass() };
            }

            for (stmtType st : h.body) {
                st.accept(this);
            }
            fixAfterNode(h);

        }
        visitOrElsePart(node.orelse, "else");
        return null;
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        fixNode(node);

        if (node.dest != null) {
            node.dest.accept(this);
        }

        if (node.values != null) {
            for (int i = 0; i < node.values.length; i++) {
                if (i > 0 || node.dest != null) {

                }
                exprType value = node.values[i];
                if (value != null) {
                    value.accept(this);
                }
            }
        }
        fixAfterNode(node);

        return null;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {

        fixNode(node);
        node.traverse(this);
        fixAfterNode(node);

        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        fixNode(node);

        node.value.accept(this);
        node.attr.accept(this);

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        fixNode(node);
        node.func.accept(this);
        this.pushInMultiline();
        handleArguments(node.args, node.keywords, node.starargs, node.kwargs);
        this.popInMultiline();
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitStarred(Starred node) throws Exception {
        fixNode(node);
        node.traverse(this);

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitAssert(Assert node) throws Exception {
        fixNode(node);
        if (node.test != null) {
            node.test.accept(this);
        }
        if (node.msg != null) {
            node.msg.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitStrJoin(StrJoin node) throws Exception {

        fixNode(node);
        if (node.strs != null) {
            for (int i = 0; i < node.strs.length; i++) {
                exprType str = node.strs[i];
                if (str != null) {
                    str.accept(this);
                    nextLine(true);
                }
            }
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        fixNode(node);

        if (node.names != null) {
            for (int i = 0; i < node.names.length; i++) {
                if (i > 0) {

                }
                if (node.names[i] != null) {
                    node.names[i].accept(this);
                }
            }
        }
        if (node.value != null) {

            node.value.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitExec(Exec node) throws Exception {
        fixNode(node);

        if (node.body != null) {
            node.body.accept(this);
        }

        if (node.globals != null) {
            node.globals.accept(this);
        }

        if (node.locals != null) {
            node.locals.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        if (node.decs != null) {
            for (decoratorsType n : node.decs) {
                if (n != null) {
                    handleDecorator(n);
                }
            }
        }

        fixNode(node);
        node.name.accept(this);

        handleArguments(node.bases, node.keywords, node.starargs, node.kwargs);

        if (node.body == null || node.body.length == 0) {
            node.body = new stmtType[] { new Pass() };
        }

        for (SimpleNode n : node.body) {
            n.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    public boolean isFilled(SimpleNode[] nodes) {
        return (nodes != null) && (nodes.length > 0);
    }

    private void handleDecorator(decoratorsType node) throws Exception {
        fixNode(node);
        if (node.func != null) {
            node.func.accept(this);
        }

        if ((node.args != null && node.args.length > 0) || (node.keywords != null && node.keywords.length > 0)
                || node.starargs != null || node.kwargs != null) {
            handleArguments(reverseNodeArray(node.args), reverseNodeArray(node.keywords), node.starargs, node.kwargs);
        }
        fixAfterNode(node);

    }

    /**
    * Prints the arguments.
    */
    protected void handleArguments(argumentsType completeArgs) throws Exception {
        exprType[] args = completeArgs.args;
        exprType[] d = completeArgs.defaults;
        exprType[] anns = completeArgs.annotation;
        int argsLen = args == null ? 0 : args.length;
        int defaultsLen = d == null ? 0 : d.length;
        int diff = argsLen - defaultsLen;

        fixNode(completeArgs);
        for (int i = 0; i < argsLen; i++) {
            exprType argName = args[i];

            //this is something as >>var:int=10<<
            //handle argument
            argName.accept(this);

            //handle annotation
            if (anns != null) {
                exprType ann = anns[i];
                if (ann != null) {

                    ann.accept(this); //right after the '='
                }
            }

            //handle defaults
            if (i >= diff) {
                exprType defaulArgValue = d[i - diff];
                if (defaulArgValue != null) {

                    defaulArgValue.accept(this);
                }
            }

        }

        //varargs
        if (completeArgs.vararg != null) {
            completeArgs.vararg.accept(this);
            if (completeArgs.varargannotation != null) {

                completeArgs.varargannotation.accept(this);
            }

        }

        //keyword only arguments (after varargs)
        if (completeArgs.kwonlyargs != null) {
            for (int i = 0; i < completeArgs.kwonlyargs.length; i++) {
                exprType kwonlyarg = completeArgs.kwonlyargs[i];
                if (kwonlyarg != null) {

                    kwonlyarg.accept(this);

                    if (completeArgs.kwonlyargannotation != null && completeArgs.kwonlyargannotation[i] != null) {

                        completeArgs.kwonlyargannotation[i].accept(this);
                    }
                    if (completeArgs.kw_defaults != null && completeArgs.kw_defaults[i] != null) {

                        completeArgs.kw_defaults[i].accept(this);
                    }
                }
            }
        }

        //keyword arguments
        if (completeArgs.kwarg != null) {

            completeArgs.kwarg.accept(this);
            if (completeArgs.kwargannotation != null) {

                completeArgs.kwargannotation.accept(this);
            }
        }

        fixAfterNode(completeArgs);
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        if (node.decs != null) {
            for (decoratorsType n : node.decs) {
                if (n != null) {
                    handleDecorator(n);
                }
            }
        }
        fixNode(node);
        node.name.accept(this);

        if (node.args != null) {

            handleArguments(node.args);

        }

        // 'def' NAME parameters ['->' test] ':' suite
        // parameters: '(' [typedargslist] ')'

        // this is the "['->' test]"
        if (node.returns != null) {
            node.returns.accept(this);
        }

        if (node.body == null || node.body.length == 0) {
            node.body = new stmtType[] { new Pass() };
        }

        int length = node.body.length;
        for (int i = 0; i < length; i++) {
            if (node.body[i] != null) {
                node.body[i].accept(this);
            }
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        return handleSimpleNode(node);
    }

    @Override
    public Object visitBreak(Break node) throws Exception {
        return handleSimpleNode(node);
    }

    @Override
    public Object visitContinue(Continue node) throws Exception {
        return handleSimpleNode(node);
    }

    private Object handleSimpleNode(SimpleNode node) throws Exception {
        fixNode(node);
        node.traverse(this);
        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitIfExp(IfExp node) throws Exception {
        //we have to change the order a bit...

        fixNode(node);
        node.body.accept(this);

        node.test.accept(this);

        if (node.orelse != null) {
            node.orelse.accept(this);
        }
        fixAfterNode(node);

        return null;
    }

    @Override
    public Object visitName(Name node) throws Exception {
        return handleSimpleNode(node);
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        return handleSimpleNode(node);
    }

    @Override
    public Object visitNum(Num node) throws Exception {
        return handleSimpleNode(node);
    }

    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        fixNode(node);

        if (node.value != null) {
            node.value.accept(this);
        }

        if (node.slice != null) {
            node.slice.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitSlice(Slice node) throws Exception {
        fixNode(node);

        if (node.lower != null) {
            node.lower.accept(this);

        }

        if (node.upper != null) {
            node.upper.accept(this);
        }

        if (node.step != null) {

            node.step.accept(this);
        }

        fixAfterNode(node);

        return null;
    }

    @Override
    public Object visitStr(Str node) throws Exception {
        return handleSimpleNode(node);
    }

    @Override
    public Object visitImport(Import node) throws Exception {

        fixNode(node);

        for (int i = 0; i < node.names.length; i++) {
            if (i > 0) {

            }
            aliasType alias = node.names[i];
            handleAlias(alias);

        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {

        fixNode(node);

        if (node.module != null) {
            node.module.accept(this);
        }

        for (int i = 0; i < node.names.length; i++) {
            aliasType alias = node.names[i];
            handleAlias(alias);
        }
        fixAfterNode(node);

        return null;
    }

    private void handleAlias(aliasType alias) throws Exception {
        fixNode(alias);
        if (alias.name != null) {
            alias.name.accept(this);
        }

        if (alias.asname != null) {
            alias.asname.accept(this);
        }
        fixAfterNode(alias);
    }

    @Override
    public Object visitRaise(Raise node) throws Exception {
        fixNode(node);

        if (node.type != null) {
            node.type.accept(this);
        }

        if (node.inst != null) {

            node.inst.accept(this);
        }

        if (node.tback != null) {

            node.tback.accept(this);
        }

        if (node.cause != null) {
            node.cause.accept(this);
        }
        fixAfterNode(node);

        return null;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        fixNode(node);

        handleArguments(node.args);

        if (node.body != null) {
            node.body.accept(this);
        }

        fixAfterNode(node);
        return null;
    }

    @Override
    public Object visitExpr(Expr node) throws Exception {
        handleSimpleNode(node);
        return null;
    }

    private void handleArguments(SimpleNode[] args, SimpleNode[] keywords, exprType starargs, exprType kwargs)
            throws Exception, IOException {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    args[i].accept(this);
                }
            }
        }

        java.util.List<keywordType> keywordsLater = new ArrayList<keywordType>();
        if (keywords != null) {
            for (int i = 0; i < keywords.length; i++) {
                keywordType keyword = (keywordType) keywords[i];
                if (keyword == null) {
                    continue;
                }
                if (keyword.afterstarargs) {
                    keywordsLater.add(keyword);
                    continue; //this one won't be added right now
                }
                handleKeyword(keyword);
            }
        }

        if (starargs != null) {
            starargs.accept(this);
        }

        for (keywordType keyword : keywordsLater) {
            handleKeyword(keyword);
        }

        if (kwargs != null) {
            kwargs.accept(this);
        }
    }

    private void handleKeyword(keywordType keyword) throws Exception, IOException {
        fixNode(keyword);
        if (keyword.arg != null) {
            keyword.arg.accept(this);
        }

        if (keyword.value != null) {
            keyword.value.accept(this);
        }
        fixAfterNode(keyword);
    }

    @Override
    public Object visitIf(If node) throws Exception {
        visitIfPart(null, node, false);
        return null;
    }

    private void visitIfPart(suiteType orelse, If node, boolean isElif) throws Exception {

        if (orelse != null) {
            fixNode(orelse);
        }
        fixNode(node);

        node.test.accept(this);

        if (node.body == null || node.body.length == 0) {
            node.body = new stmtType[] { new Pass() };
        }

        //write the body and dedent
        for (SimpleNode n : node.body) {
            n.accept(this);
        }

        if (orelse != null) {
            fixAfterNode(orelse);
        }

        if (node.orelse != null && ((Suite) node.orelse).body != null && ((Suite) node.orelse).body.length > 0) {
            stmtType[] body = ((Suite) node.orelse).body;
            if (body.length == 1 && body[0] instanceof If) {
                If if1 = (If) body[0];
                if (if1.test == null) {
                    visitOrElsePart(node.orelse, "else");
                } else {
                    boolean foundIf = false;
                    if (if1.specialsBefore != null) {
                        for (Object o : if1.specialsBefore) {
                            if (o.toString().equals("if")) {
                                foundIf = true;
                                break;
                            }
                        }
                    }
                    if (foundIf) {

                        visitIfPart(node.orelse, if1, false);

                    } else {
                        visitIfPart(node.orelse, if1, true);
                    }
                }

            } else {
                visitOrElsePart(node.orelse, "else");
            }
        }

        fixAfterNode(node);

    }

    /**
     * This should be the entry point for any node, as it properly handles nodes that aren't usually handled.
     */
    protected SimpleNode visitNode(SimpleNode node) throws Exception {
        if (node == null) {
            return null;
        }

        if (node instanceof decoratorsType) {
            handleDecorator((decoratorsType) node);
        } else if (node instanceof keywordType) {
            handleKeyword((keywordType) node);
        } else if (node instanceof argumentsType) {
            handleArguments((argumentsType) node);
        } else if (node instanceof aliasType) {
            handleAlias((aliasType) node);
        } else {
            node.accept(this);
        }

        return null;
    }

}
