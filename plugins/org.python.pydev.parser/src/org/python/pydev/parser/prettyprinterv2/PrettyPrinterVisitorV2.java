/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.python.pydev.core.IGrammarVersionProvider;
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
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.ListComp;
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
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * statements that 'need' to be on a new line:
 * print
 * del
 * pass
 * flow
 * import
 * global
 * exec
 * assert
 * 
 * 
 * flow:
 * return
 * yield
 * raise
 * 
 * 
 * compound:
 * if
 * while
 * for
 * try
 * func
 * class
 * 
 * @author Fabio
 */
public final class PrettyPrinterVisitorV2 extends PrettyPrinterUtilsV2 {

    private int tupleNeedsParens;

    public PrettyPrinterVisitorV2(IPrettyPrinterPrefs prefs, PrettyPrinterDocV2 doc) {
        super(prefs, doc);
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        beforeNode(node);

        int id = 0;
        java.util.List<ILinePart> recordChanges = null;
        org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> lowerAndHigher = null;

        for (int i = 0; i < node.targets.length; i++) {
            exprType target = node.targets[i];
            if (i >= 1) { //more than one assign
                doc.add(lowerAndHigher.o2.getLine(), lowerAndHigher.o2.getBeginCol(),
                        this.prefs.getAssignPunctuation(), node);
            }
            id = this.doc.pushRecordChanges();
            target.accept(this);
            recordChanges = this.doc.popRecordChanges(id);
            lowerAndHigher = doc.getLowerAndHigerFound(recordChanges);
        }
        doc.add(lowerAndHigher.o2.getLine(), lowerAndHigher.o2.getBeginCol(), this.prefs.getAssignPunctuation(), node);

        node.value.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        beforeNode(node);
        int id = this.doc.pushRecordChanges();
        node.target.accept(this);
        org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> lowerAndHigerFound = this.doc
                .getLowerAndHigerFound(this.doc
                        .popRecordChanges(id));
        ILinePart lastPart = lowerAndHigerFound.o2;
        doc.add(lastPart.getLine(), lastPart.getBeginCol(), this.prefs.getAugOperatorMapping(node.op), node);
        node.value.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        beforeNode(node);
        int id = this.doc.pushRecordChanges();
        this.pushTupleNeedsParens();
        node.left.accept(this);
        org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> lowerAndHigerFound = this.doc
                .getLowerAndHigerFound(this.doc
                        .popRecordChanges(id));
        ILinePart lastPart = lowerAndHigerFound.o2;
        doc.add(lastPart.getLine(), lastPart.getBeginCol(), this.prefs.getOperatorMapping(node.op), node);
        node.right.accept(this);
        this.popTupleNeedsParens();
        afterNode(node);
        return null;
    }

    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, this.prefs.getUnaryopOperatorMapping(node.op), node);
        node.operand.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        beforeNode(node);
        for (int i = 0; i < node.values.length - 1; i++) {

            int id = doc.pushRecordChanges();
            node.values[i].accept(this);
            java.util.List<ILinePart> changes = doc.popRecordChanges(id);
            org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> lowerAndHigher = doc
                    .getLowerAndHigerFound(changes);
            ILinePart lastPart = lowerAndHigher.o2;
            doc.add(lastPart.getLine(), lastPart.getBeginCol(), this.prefs.getBoolOperatorMapping(node.op), lastNode);

        }

        node.values[node.values.length - 1].accept(this);

        afterNode(node);
        return null;
    }

    @Override
    public Object visitCompare(Compare node) throws Exception {
        beforeNode(node);
        this.pushTupleNeedsParens();
        int id = this.doc.pushRecordChanges();
        node.left.accept(this);
        java.util.List<ILinePart> recordChanges = this.doc.popRecordChanges(id);
        org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> lowerAndHigher = doc
                .getLowerAndHigerFound(recordChanges);

        for (int i = 0; i < node.comparators.length; i++) {
            ILinePart lastPart = lowerAndHigher.o2; //higher
            doc.add(lastPart.getLine(), lastPart.getBeginCol(), this.prefs.getCmpOp(node.ops[i]), lastNode);

            id = this.doc.pushRecordChanges();
            node.comparators[i].accept(this);
            recordChanges = this.doc.popRecordChanges(id);
            lowerAndHigher = doc.getLowerAndHigerFound(recordChanges);
        }
        this.popTupleNeedsParens();
        afterNode(node);
        return null;
    }

    @Override
    public Object visitEllipsis(Ellipsis node) throws Exception {
        int id = this.doc.pushRecordChanges();
        beforeNode(node);
        java.util.List<ILinePart> changes = this.doc.popRecordChanges(id);
        org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> lowerAndHigerFound = this.doc
                .getLowerAndHigerFound(changes);
        if (lowerAndHigerFound != null) {
            this.doc.add(lowerAndHigerFound.o2.getLine(), lowerAndHigerFound.o2.getBeginCol(), "...", node);
        } else {
            this.doc.add(node.beginLine, node.beginColumn, "...", node);
        }
        afterNode(node);
        return null;
    }

    @Override
    public Object visitDict(Dict node) throws Exception {
        beforeNode(node);
        exprType[] keys = node.keys;
        exprType[] values = node.values;
        doc.addRequire("{", node);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                doc.addRequire(",", lastNode);
            }
            this.pushTupleNeedsParens();
            keys[i].accept(this);
            this.popTupleNeedsParens();

            doc.addRequire(":", lastNode);

            this.pushTupleNeedsParens();
            values[i].accept(this);
            this.popTupleNeedsParens();
        }
        doc.addRequire("}", lastNode);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        beforeNode(node);
        if (node.elts != null && node.elts.length > 0) {
            if (tupleNeedsParens > 0) {
                doc.addRequire("(", node);
            }
            int id = doc.pushRecordChanges();

            //tuple inside tuple
            this.pushTupleNeedsParens();
            visitCommaSeparated(node.elts, node.endsWithComma);
            this.popTupleNeedsParens();

            // Note: guaranteed to be sorted!
            java.util.List<ILinePart> changes = doc.popRecordChanges(id);

            //Ok, treat the following case: if we added a comment, we have a new line, in which case the tuple
            //MUST have parens.
            if (tupleNeedsParens == 0) {
                int len = changes.size() - 1; //If the last is a comment, it's Ok (so -1).
                for (int i = 0; i < len; i++) {
                    ILinePart iLinePart = changes.get(i);
                    if (iLinePart.getToken() instanceof commentType) {
                        tupleNeedsParens = 1;
                        doc.addRequireBefore("(", changes.get(0));
                        break;
                    }
                }
            }

            if (tupleNeedsParens > 0) {
                doc.addRequire(")", lastNode);
            }
        } else {
            doc.addRequire("(", node);
            doc.addRequire(")", node);
        }
        afterNode(node);
        return null;
    }

    private void visitCommaSeparated(exprType[] elts, boolean requireEndWithCommaSingleElement) throws Exception {
        if (elts != null) {
            for (int i = 0; i < elts.length; i++) {
                if (elts[i] != null) {
                    if (i > 0) {
                        doc.addRequire(",", lastNode);
                    }
                    elts[i].accept(this);
                }
            }
            if (elts.length == 1 && requireEndWithCommaSingleElement) {
                doc.addRequire(",", lastNode);
            }
        }
    }

    @Override
    public Object visitList(List node) throws Exception {
        beforeNode(node);
        this.pushTupleNeedsParens();
        doc.addRequire("[", node);
        visitCommaSeparated(node.elts, false);
        doc.addRequire("]", lastNode);
        this.popTupleNeedsParens();
        afterNode(node);
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        beforeNode(node);
        switch (node.ctx) {
            case ListComp.ListCtx:
                doc.addRequire("[", node);
                break;
            case ListComp.TupleCtx:
                doc.addRequire("(", node);
                break;
        }

        int id = this.doc.pushRecordChanges();
        this.pushTupleNeedsParens();
        node.elt.accept(this);
        this.popTupleNeedsParens();
        for (SimpleNode c : node.generators) {
            doc.addRequire(" for ", lastNode);
            c.accept(this);
        }
        java.util.List<ILinePart> recordedChanges = this.doc.popRecordChanges(id);
        this.doc.replaceRecorded(recordedChanges, "for", " for ", "if", " if ");

        switch (node.ctx) {
            case ListComp.ListCtx:
                doc.addRequire("]", lastNode);
                break;
            case ListComp.TupleCtx:
                doc.addRequire(")", lastNode);
                break;
        }

        afterNode(node);
        return null;
    }

    @Override
    public Object visitSetComp(SetComp node) throws Exception {
        beforeNode(node);
        doc.addRequire("{", node);
        int id = doc.pushRecordChanges();
        node.elt.accept(this);
        doc.addRequire(" for ", lastNode);
        for (comprehensionType c : node.generators) {
            c.accept(this);
        }
        doc.addRequire("}", lastNode);
        doc.replaceRecorded(doc.popRecordChanges(id), "for", " for ", "if", " if ");
        afterNode(node);
        return null;
    }

    @Override
    public Object visitDictComp(DictComp node) throws Exception {
        beforeNode(node);
        int id = doc.pushRecordChanges();
        doc.addRequire("{", node);
        node.key.accept(this);
        doc.addRequire(":", lastNode);
        node.value.accept(this);
        doc.addRequire(" for ", lastNode);
        for (comprehensionType c : node.generators) {
            c.accept(this);
        }
        doc.replaceRecorded(doc.popRecordChanges(id), "for", " for ", "if", " if ");
        doc.addRequire("}", lastNode);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitSet(Set node) throws Exception {
        beforeNode(node);
        doc.addRequire("{", node.elts[0]);
        visitCommaSeparated(node.elts, false);
        doc.addRequire("}", lastNode);
        afterNode(node);
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
        beforeNode(node);
        node.target.accept(this);
        doc.addRequire("in", lastNode);
        this.pushTupleNeedsParens();
        node.iter.accept(this);
        this.popTupleNeedsParens();
        for (SimpleNode s : reverseNodeArray(node.ifs)) {
            doc.addRequire(" if ", lastNode);
            s.accept(this);
        }
        afterNode(node);
        return null;
    }

    @Override
    public Object visitWhile(While node) throws Exception {
        startStatementPart();
        beforeNode(node);
        doc.addRequire("while", node);
        node.test.accept(this);
        doc.addRequire(":", lastNode);
        this.doc.addRequireIndent(":", lastNode);
        endStatementPart(node);
        for (SimpleNode n : node.body) {
            n.accept(this);
        }
        endSuiteWithOrElse(node.orelse);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitWith(With node) throws Exception {
        startStatementPart();
        beforeNode(node);
        doc.addRequire("with", node);
        if (node.with_item != null) {
            for (int i = 0; i < node.with_item.length; i++) {
                if (i > 0) {
                    doc.addRequire(",", lastNode);
                }
                WithItem withItem = (WithItem) node.with_item[i];
                withItem.accept(this);
            }
        }

        doc.addRequire(":", lastNode);
        this.doc.addRequireIndent(":", lastNode);
        endStatementPart(node);

        if (node.body != null) {
            node.body.accept(this);
        }
        dedent();

        afterNode(node);
        return null;
    }

    @Override
    public Object visitWithItem(WithItem node) throws Exception {
        beforeNode(node);

        node.context_expr.accept(this);

        exprType optional = node.optional_vars;
        if (optional != null && lastNode != null) {
            doc.addRequire("as", lastNode);
            optional.accept(this);
        }

        afterNode(node);
        return null;
    }

    @Override
    public Object visitFor(For node) throws Exception {
        //for a in b: xxx else: yyy
        int id = doc.pushRecordChanges();

        //a
        startStatementPart();
        doc.addRequire("for ", node); //Make the require with the final version of the "for " string.
        beforeNode(node);

        node.target.accept(this);

        doc.replaceRecorded(doc.popRecordChanges(id), "for", "for ");

        //in b
        doc.addRequire("in", lastNode);
        node.iter.accept(this);
        doc.addRequire(":", lastNode);
        this.doc.addRequireIndent(":", lastNode);

        endStatementPart(node);

        for (SimpleNode n : node.body) {
            n.accept(this);
        }
        suiteType orelse = node.orelse;
        endSuiteWithOrElse(orelse);

        afterNode(node);
        return null;
    }

    public void pushTupleNeedsParens() {
        this.tupleNeedsParens += 1;
    }

    public void popTupleNeedsParens() {
        this.tupleNeedsParens -= 1;
    }

    private void endSuiteWithOrElse(suiteType orelse) throws Exception {
        if (orelse == null) {
            dedent(this.prefs.getLinesAfterSuite());
        } else {
            dedent();
            visitOrElsePart(orelse, "else");
        }
    }

    @Override
    public Object visitRepr(Repr node) throws Exception {
        int id = doc.pushRecordChanges();
        Object ret = super.visitRepr(node);
        java.util.List<ILinePart> changes = doc.popRecordChanges(id);
        org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> lowerAndHigerFound = doc
                .getLowerAndHigerFound(changes);
        doc.addBefore(lowerAndHigerFound.o1.getLine(), lowerAndHigerFound.o1.getBeginCol(), "`", node);
        doc.add(lowerAndHigerFound.o2.getLine(), lowerAndHigerFound.o2.getBeginCol(), "`", node);
        return ret;
    }

    @Override
    public Object visitReturn(Return node) throws Exception {
        int id = doc.pushRecordChanges();

        beforeNode(node);
        String replaceToken;
        if (node.value != null) {
            replaceToken = "return ";
        } else {
            replaceToken = "return";
        }
        doc.addRequire(replaceToken, node);
        node.traverse(this);

        java.util.List<ILinePart> changes = doc.popRecordChanges(id);

        doc.replaceRecorded(changes, "return", replaceToken);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitDelete(Delete node) throws Exception {

        beforeNode(node);
        doc.addRequire("del", node);
        node.traverse(this);

        afterNode(node);
        return null;
    }

    public void visitTryPart(SimpleNode node, stmtType[] body) throws Exception {
        //try:
        beforeNode(node);

        boolean indent = true;

        if (node instanceof TryFinally) {
            indent = false;
            if (node.specialsBefore != null && node.specialsBefore.size() > 0) {
                for (Object o : node.specialsBefore) {
                    if (o.toString().equals("try")) {
                        indent = true;
                        break;
                    }
                }
            }

            if (!indent) {
                //We didn't find a try there...

                //The only reason we don't indent is when we have a try..finally containing a try..except, in which
                //case, that AST is actually representing a try..except..finally (so, it's only valid if we have a body with 1 element
                //and that element is an except).
                if (body == null || body.length != 1 || !(body[0] instanceof TryExcept)) {
                    indent = true;
                }
            }
        }

        if (indent) {
            doc.addRequire("try", node);
            doc.addRequire(":", node);
            doc.addRequireIndent(":", node);
        }
        if (body != null) {
            for (stmtType st : body) {
                st.accept(this);
            }
        }
        if (indent) {
            dedent();
        }
        afterNode(node);

    }

    public void visitOrElsePart(suiteType orelse, String expectedToken) throws Exception {
        visitOrElsePart(orelse, expectedToken, this.prefs.getLinesAfterSuite());
    }

    public void visitOrElsePart(suiteType orelse, String expectedToken, int linesAfterSuite) throws Exception {

        if (orelse != null) {
            startStatementPart();
            beforeNode(orelse);
            doc.addRequire(expectedToken, orelse);
            doc.addRequire(":", orelse);
            doc.addRequireIndent(":", orelse);
            endStatementPart(orelse);
            for (stmtType st : ((Suite) orelse).body) {
                st.accept(this);
            }
            dedent(linesAfterSuite);
            afterNode(orelse);
        }

    }

    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        visitTryPart(node, node.body);
        visitOrElsePart(node.finalbody, "finally");
        return null;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        visitTryPart(node, node.body);
        for (excepthandlerType h : node.handlers) {

            startStatementPart();
            beforeNode(h);
            doc.addRequire("except", lastNode);
            this.pushTupleNeedsParens();
            if (h.type != null || h.name != null) {
                doc.addRequire(" ", lastNode);
            }
            if (h.type != null) {
                h.type.accept(this);
            }
            if (h.name != null) {

                if (h.type != null) {
                    int grammarVersion = this.prefs.getGrammarVersion();
                    if (grammarVersion < IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6) {
                        doc.addRequire(",", lastNode);

                    } else if (grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6
                            || grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
                        doc.addRequireOneOf(lastNode, "as", ",");

                    } else { // Python 3.0 or greater
                        doc.addRequire("as", lastNode);
                    }
                }
                h.name.accept(this);
            }
            afterNode(h);
            popTupleNeedsParens();
            this.doc.addRequire(":", lastNode);
            this.doc.addRequireIndent(":", lastNode);
            endStatementPart(lastNode);

            for (stmtType st : h.body) {
                st.accept(this);
            }
            dedent();
        }
        visitOrElsePart(node.orelse, "else");
        return null;
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        beforeNode(node);

        doc.add(node.beginLine, node.beginColumn, "print ", node);

        if (node.dest != null) {
            doc.add(node.beginLine, node.beginColumn, ">> ", node);
            node.dest.accept(this);
        }

        if (node.values != null) {
            for (int i = 0; i < node.values.length; i++) {
                if (i > 0 || node.dest != null) {
                    doc.addRequire(",", lastNode);
                }
                exprType value = node.values[i];
                if (value != null) {
                    value.accept(this);
                }
            }
        }

        afterNode(node);
        return null;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        beforeNode(node);
        doc.addRequire("yield", node);
        if (node.yield_from) {
            doc.addRequire("from", node);
        }
        node.traverse(this);

        afterNode(node);
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        beforeNode(node);
        int id = doc.pushRecordChanges();
        node.value.accept(this);
        java.util.List<ILinePart> recordChanges = doc.popRecordChanges(id);
        org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> lowerAndHigerFound = doc
                .getLowerAndHigerFound(recordChanges);

        doc.add(lowerAndHigerFound.o2.getLine(), lowerAndHigerFound.o2.getBeginCol(), ".", node.value);
        node.attr.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        beforeNode(node);
        node.func.accept(this);
        doc.addRequire("(", lastNode);
        this.pushTupleNeedsParens();
        handleArguments(node.args, node.keywords, node.starargs, node.kwargs);
        this.popTupleNeedsParens();
        doc.addRequire(")", lastNode);

        afterNode(node);
        return null;
    }

    @Override
    public Object visitStarred(Starred node) throws Exception {
        unhandled_node(node);
        beforeNode(node);
        doc.addRequire("*", node);
        node.traverse(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitAssert(Assert node) throws Exception {
        unhandled_node(node);
        beforeNode(node);
        doc.addRequire("assert", node);
        if (node.test != null) {
            node.test.accept(this);
        }
        if (node.msg != null) {
            doc.addRequire(",", lastNode);
            this.pushTupleNeedsParens();
            node.msg.accept(this);
            this.popTupleNeedsParens();
        }

        afterNode(node);
        return null;
    }

    @Override
    public Object visitStrJoin(StrJoin node) throws Exception {
        unhandled_node(node);
        beforeNode(node);
        exprType last = null;
        if (node.strs != null) {
            for (int i = 0; i < node.strs.length; i++) {
                exprType str = node.strs[i];
                if (str != null) {
                    if (last != null && last.beginLine != str.beginLine) {
                        this.doc.addRequire("\\", last);
                    }
                    str.accept(this);
                    //                    if(last == null){
                    //                        doc.addIndent(str); //Only add an indent after the 1st string
                    //                    }
                    last = str;
                }
            }
        }
        if (last != null) {
            //            doc.getLine(last.beginLine).dedent(0);
        }
        afterNode(node);
        return null;
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        beforeNode(node);
        doc.addRequire("global", node);

        if (node.names != null) {
            for (int i = 0; i < node.names.length; i++) {
                if (i > 0) {
                    doc.addRequire(",", lastNode);
                }
                if (node.names[i] != null) {
                    node.names[i].accept(this);
                }
            }
        }
        if (node.value != null) {
            doc.addRequire("=", lastNode);
            node.value.accept(this);
        }

        afterNode(node);
        return null;
    }

    @Override
    public Object visitExec(Exec node) throws Exception {
        int id = doc.pushRecordChanges();
        beforeNode(node);

        doc.addRequire("exec ", node);

        if (node.body != null) {
            node.body.accept(this);
        }

        if (node.globals != null || node.locals != null) {
            doc.addRequire("in", lastNode);
        }

        if (node.globals != null) {
            node.globals.accept(this);
        }

        if (node.locals != null) {
            if (node.globals != null) {
                doc.addRequire(",", lastNode);
            }
            node.locals.accept(this);
        }

        doc.replaceRecorded(doc.popRecordChanges(id), "exec", "exec ");
        afterNode(node);
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

        beforeNode(node);
        doc.add(node.name.beginLine, node.beginColumn, "class", node);
        node.name.accept(this);

        int id = this.doc.pushRecordChanges();
        this.pushTupleNeedsParens();
        handleArguments(node.bases, node.keywords, node.starargs, node.kwargs);
        this.popTupleNeedsParens();
        java.util.List<ILinePart> changes = this.doc.popRecordChanges(id);
        if (changes.size() > 0) {
            org.python.pydev.shared_core.structure.Tuple<ILinePart, ILinePart> found = this.doc.getLowerAndHigerFound(
                    changes, true);
            if (found != null) {
                this.doc.addRequireBefore("(", found.o1);
                this.doc.addRequire(")", lastNode);
            }
        }
        this.doc.addRequire(":", lastNode);
        this.doc.addRequireIndent(":", lastNode);

        for (SimpleNode n : node.body) {
            n.accept(this);
        }

        dedent(this.prefs.getLinesAfterClass());
        afterNode(node);
        return null;
    }

    public boolean isFilled(SimpleNode[] nodes) {
        return (nodes != null) && (nodes.length > 0);
    }

    private void handleDecorator(decoratorsType node) throws Exception {
        beforeNode(node);
        doc.addRequire("@", node);
        if (node.func != null) {
            node.func.accept(this);
        }

        this.pushTupleNeedsParens();
        if (node.isCall) {
            doc.addRequire("(", lastNode);
        }
        if ((node.args != null && node.args.length > 0) || (node.keywords != null && node.keywords.length > 0)
                || node.starargs != null || node.kwargs != null) {
            handleArguments(reverseNodeArray(node.args), reverseNodeArray(node.keywords), node.starargs, node.kwargs);
        }
        if (node.isCall) {
            doc.addRequire(")", lastNode);
        }
        this.popTupleNeedsParens();
        afterNode(node);
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

        beforeNodeWithoutSettintgLastNode(completeArgs);
        boolean foundBefore = false;
        for (int i = 0; i < argsLen; i++) {
            foundBefore = true;
            if (i > 0) {
                doc.addRequire(",", lastNode);
            }
            exprType argName = args[i];

            //this is something as >>var:int=10<<
            //handle argument
            argName.accept(this);

            //handle annotation
            if (anns != null) {
                exprType ann = anns[i];
                if (ann != null) {
                    doc.addRequire(":", lastNode);
                    ann.accept(this); //right after the '='
                }
            }

            //handle defaults
            if (i >= diff) {
                exprType defaulArgValue = d[i - diff];
                if (defaulArgValue != null) {
                    doc.addRequire("=", lastNode);
                    defaulArgValue.accept(this);
                }
            }

        }

        //varargs
        if (completeArgs.vararg != null) {
            if (lastNode == null) {
                lastNode = completeArgs.vararg;
            }
            if (foundBefore) {
                doc.addRequire(",", lastNode);
            }
            doc.addRequire("*", lastNode);
            foundBefore = true;
            completeArgs.vararg.accept(this);
            if (completeArgs.varargannotation != null) {
                doc.addRequire(":", lastNode);
                completeArgs.varargannotation.accept(this);
            }

        } else {
            if (completeArgs.kwonlyargs != null && completeArgs.kwonlyargs.length > 0
                    && completeArgs.kwonlyargs[0] != null) {
                //we must add a '*,' to print it if we have a keyword arg after the varargs but don't really have an expression for it

                if (foundBefore) {
                    doc.addRequire(",", lastNode);
                }
                doc.addRequire("*", completeArgs.kwonlyargs[0]);
                doc.addRequire(",", completeArgs.kwonlyargs[0]);
                foundBefore = false; //comma is already there
            }
        }

        //keyword only arguments (after varargs)
        if (completeArgs.kwonlyargs != null) {
            for (int i = 0; i < completeArgs.kwonlyargs.length; i++) {
                if (foundBefore) {
                    doc.addRequire(",", lastNode);
                }
                foundBefore = true;

                exprType kwonlyarg = completeArgs.kwonlyargs[i];
                if (kwonlyarg != null) {

                    kwonlyarg.accept(this);

                    if (completeArgs.kwonlyargannotation != null && completeArgs.kwonlyargannotation[i] != null) {
                        doc.addRequire(":", lastNode);
                        completeArgs.kwonlyargannotation[i].accept(this);
                    }
                    if (completeArgs.kw_defaults != null && completeArgs.kw_defaults[i] != null) {
                        doc.addRequire("=", lastNode);
                        completeArgs.kw_defaults[i].accept(this);
                    }
                }
            }
        }

        //keyword arguments
        if (completeArgs.kwarg != null) {
            if (lastNode == null) {
                lastNode = completeArgs.kwarg;
            }
            if (foundBefore) {
                doc.addRequire(",", lastNode);
            }
            doc.addRequire("**", lastNode);

            completeArgs.kwarg.accept(this);
            if (completeArgs.kwargannotation != null) {
                doc.addRequire(":", lastNode);
                completeArgs.kwargannotation.accept(this);
            }
        }

        afterNode(completeArgs);

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
        beforeNode(node);
        doc.add(node.name.beginLine, node.beginColumn, "def", node);
        node.name.accept(this);

        doc.addRequire("(", lastNode);
        if (node.args != null) {
            this.pushTupleNeedsParens();
            handleArguments(node.args);
            this.popTupleNeedsParens();
        }

        // 'def' NAME parameters ['->' test] ':' suite
        // parameters: '(' [typedargslist] ')'
        doc.addRequire(")", lastNode);

        // this is the "['->' test]"
        if (node.returns != null) {
            doc.addRequire("->", lastNode);
            node.returns.accept(this);
        }

        doc.addRequire(":", lastNode);
        doc.addRequireIndent(":", lastNode);

        if (node.body != null) {
            int length = node.body.length;
            for (int i = 0; i < length; i++) {
                if (node.body[i] != null) {
                    node.body[i].accept(this);
                }
            }
        }

        dedent(this.prefs.getLinesAfterMethod());
        afterNode(node);
        return null;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        return handleSimpleNode(node, "pass");
    }

    @Override
    public Object visitBreak(Break node) throws Exception {
        return handleSimpleNode(node, "break");
    }

    @Override
    public Object visitContinue(Continue node) throws Exception {
        return handleSimpleNode(node, "continue");
    }

    private Object handleSimpleNode(SimpleNode node, String checkTokenAdded) throws Exception {
        beforeNode(node);
        node.traverse(this);
        doc.addRequire(checkTokenAdded, lastNode);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitIfExp(IfExp node) throws Exception {
        //we have to change the order a bit...
        int id = this.doc.pushRecordChanges();

        beforeNode(node);
        node.body.accept(this);

        int id2 = 0;
        if (node.orelse != null) {
            id2 = this.doc.pushRecordChanges();
        }

        SimpleNode lastBeforeIf = lastNode;
        this.doc.addRequire(" if ", lastBeforeIf);
        node.test.accept(this);

        java.util.List<ILinePart> recordedChanges = this.doc.popRecordChanges(id);
        this.doc.replaceRecorded(recordedChanges, "if", " if ");

        if (node.orelse != null) {
            this.doc.addRequire(" else ", lastNode);
            recordedChanges = this.doc.popRecordChanges(id2);
            this.doc.replaceRecorded(recordedChanges, "else", " else ");
            node.orelse.accept(this);
        }

        afterNode(node);

        return null;
    }

    @Override
    public Object visitName(Name node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, node.id, node);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, node.id, node);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitNum(Num node) throws Exception {
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, node.num, node);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        beforeNode(node);

        if (node.value != null) {
            node.value.accept(this);
        }

        doc.addRequire("[", lastNode);
        if (node.slice != null) {
            node.slice.accept(this);
        }

        doc.addRequire("]", lastNode);

        afterNode(node);
        return null;
    }

    @Override
    public Object visitSlice(Slice node) throws Exception {
        beforeNode(node);

        if (node.lower != null) {
            node.lower.accept(this);
            doc.addRequire(":", lastNode);
        }

        if (node.upper != null) {
            if (node.lower == null) {
                doc.addRequire(":", lastNode);
            }
            node.upper.accept(this);
        }

        if (node.step != null) {
            doc.addRequire(":", lastNode);
            node.step.accept(this);
        }

        if (node.lower == null && node.upper == null && node.step == null) {
            doc.addRequire(":", lastNode);
        }

        afterNode(node);
        return null;
    }

    @Override
    public Object visitStr(Str node) throws Exception {
        unhandled_node(node);
        beforeNode(node);
        doc.add(node.beginLine, node.beginColumn, NodeUtils.getStringToPrint(node), node);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        int id = this.doc.pushRecordChanges();
        beforeNode(node);
        this.doc.addRequire("import ", node);

        for (int i = 0; i < node.names.length; i++) {
            if (i > 0) {
                this.doc.addRequire(",", lastNode);
            }
            aliasType alias = node.names[i];
            handleAlias(alias);
        }

        afterNode(node);

        this.doc.replaceRecorded(this.doc.popRecordChanges(id), "import", "import ");
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        int id = this.doc.pushRecordChanges();

        boolean emptyModule = node.module == null || ((NameTok) node.module).id == null
                || ((NameTok) node.module).id.equals("");

        SimpleNode bindFromTo = emptyModule ? node.module : node;

        beforeNode(node);
        LinePartRequireMark mark = this.doc.addRequire("from ", bindFromTo);

        if (node.level > 0) {
            String s = new FastStringBuffer(node.level).appendN('.', node.level).toString();
            doc.addRequireAfter(s, mark);
        }

        if (!emptyModule) {
            node.module.accept(this); //no need to add an empty module
        } else {
            //empty
            beforeNode(node.module);
            afterNode(node.module);
        }

        this.doc.addRequire(" import ", lastNode);
        if (node.names.length == 0) {
            this.doc.addRequire("*", lastNode);
        } else {
            for (int i = 0; i < node.names.length; i++) {
                aliasType alias = node.names[i];
                if (i > 0) {
                    doc.addRequire(",", lastNode);
                }
                handleAlias(alias);
            }
        }

        afterNode(node);

        java.util.List<ILinePart> recordChanges = this.doc.popRecordChanges(id);
        this.doc.replaceRecorded(recordChanges, "import", " import ", "from", "from ");

        return null;
    }

    private void handleAlias(aliasType alias) throws Exception {
        beforeNode(alias);
        if (alias.name != null) {
            alias.name.accept(this);
        }

        if (alias.asname != null) {
            doc.addRequire("as", lastNode);
            alias.asname.accept(this);
        }
        afterNode(alias);
    }

    @Override
    public Object visitRaise(Raise node) throws Exception {
        int id = this.doc.pushRecordChanges();
        beforeNode(node);
        this.pushTupleNeedsParens();
        if (node.type != null) {
            this.doc.addRequire("raise ", node);
        } else {
            this.doc.addRequire("raise", node);
        }
        if (node.type != null) {
            node.type.accept(this);
        }

        if (node.inst != null) {
            this.doc.addRequire(",", lastNode);
            node.inst.accept(this);
        }

        if (node.tback != null) {
            this.doc.addRequire(",", lastNode);
            node.tback.accept(this);
        }

        if (node.cause != null) {
            this.doc.addRequire(" from ", lastNode);
            node.cause.accept(this);
        }

        java.util.List<ILinePart> recordChanges = this.doc.popRecordChanges(id);
        if (node.type != null) {
            this.doc.replaceRecorded(recordChanges, "raise", "raise ", "from", " from ");
        }
        this.popTupleNeedsParens();
        afterNode(node);
        return null;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        beforeNode(node);

        argumentsType arguments = node.args;
        String str;
        if (arguments == null || arguments.args == null || arguments.args.length == 0) {
            str = "lambda";
        } else {
            str = "lambda ";
        }

        doc.add(node.beginLine, node.beginColumn, str, node);
        if (node.args != null) {
            this.handleArguments(node.args);
        }

        doc.addRequire(":", lastNode);
        if (node.body != null) {
            node.body.accept(this);
        }

        afterNode(node);
        return null;
    }

    private void handleArguments(SimpleNode[] args, SimpleNode[] keywords, exprType starargs, exprType kwargs)
            throws Exception, IOException {
        boolean foundBefore = false;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (foundBefore) {
                    doc.addRequire(",", lastNode);
                }
                if (args[i] != null) {
                    args[i].accept(this);
                    foundBefore = true;
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
                if (foundBefore) {
                    doc.addRequire(",", lastNode);
                }
                foundBefore = true;
                handleKeyword(keyword);
            }
        }

        if (starargs != null) {
            if (foundBefore) {
                doc.addRequire(",", lastNode);
            }
            doc.addRequire("*", lastNode);
            starargs.accept(this);
            foundBefore = true;
        }

        for (keywordType keyword : keywordsLater) {
            if (foundBefore) {
                doc.addRequire(",", lastNode);
            }
            foundBefore = true;
            handleKeyword(keyword);
        }

        if (kwargs != null) {
            if (foundBefore) {
                doc.addRequire(",", lastNode);
            }
            doc.addRequire("**", lastNode);
            kwargs.accept(this);
        }
    }

    private void handleKeyword(keywordType keyword) throws Exception, IOException {
        beforeNode(keyword);
        if (keyword.arg != null) {
            keyword.arg.accept(this);
        }
        doc.addRequire("=", lastNode);
        if (keyword.value != null) {
            keyword.value.accept(this);
        }

        afterNode(keyword);
    }

    @Override
    public Object visitIf(If node) throws Exception {
        visitIfPart(null, node, false);
        return null;
    }

    private void visitIfPart(suiteType orelse, If node, boolean isElif) throws Exception {
        startStatementPart();
        if (orelse != null) {
            beforeNode(orelse);
        }
        beforeNode(node);
        if (isElif) {
            doc.addRequire("elif", node);
        } else {
            doc.addRequire("if", node);
        }
        this.pushTupleNeedsParens();
        node.test.accept(this);
        this.popTupleNeedsParens();
        endStatementPart(node);

        doc.addRequire(":", lastNode);
        doc.addRequireIndent(":", lastNode);

        //write the body and dedent
        for (SimpleNode n : node.body) {
            n.accept(this);
        }

        dedent();
        afterNode(node);
        if (orelse != null) {
            afterNode(orelse);
        }

        if (node.orelse != null && node.orelse.body != null && node.orelse.body.length > 0) {
            if (node.orelse.body.length == 1 && node.orelse.body[0] instanceof If) {
                If if1 = (If) node.orelse.body[0];
                if (if1.test == null) {
                    visitOrElsePart(node.orelse, "else", 0);
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
                        doc.addRequire("else", if1);
                        doc.addRequire(":", if1);
                        indent(if1, true);
                        visitIfPart(node.orelse, if1, false);
                        dedent();
                    } else {
                        visitIfPart(node.orelse, if1, true);
                    }
                }

            } else {
                visitOrElsePart(node.orelse, "else", 0);
            }
        }
    }

    @Override
    public String toString() {
        return "PrettyPrinterVisitorV2{\n" + this.doc + "\n}";
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
