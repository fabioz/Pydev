/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.refactoring.ast.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.Await;
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
import org.python.pydev.parser.jython.ast.Expression;
import org.python.pydev.parser.jython.ast.ExtSlice;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.GeneratorExp;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Interactive;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NonLocal;
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
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.VisitorIF;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.WithItem;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author fabioz
 *
 */
public class FindDuplicatesVisitor implements VisitorIF {

    private final exprType expression;
    private final ITextSelection selection;
    private final List<Tuple<ITextSelection, SimpleNode>> duplicates = new ArrayList<Tuple<ITextSelection, SimpleNode>>();

    private final IDocument doc;
    private final PySelection ps;
    private final char[] selectedText;

    private SimpleNode lastFound = null;
    private ParsingUtils parsingUtils;

    public FindDuplicatesVisitor(ITextSelection selection, exprType expression, IDocument doc) {
        this.selection = selection;
        this.expression = expression;
        this.doc = doc;
        this.ps = new PySelection(this.doc, selection);
        FastStringBuffer buf = new FastStringBuffer(ps.getSelectedText(), 0);
        ParsingUtils.removeCommentsAndWhitespaces(buf);
        buf.replaceAll("\\", ""); //Remove all the \\ 
        selectedText = buf.toCharArray();
        parsingUtils = ParsingUtils.create(this.doc);
    }

    protected boolean unhandled_node(SimpleNode node) throws Exception {
        this.addLastFound(node);
        if (node.equals(expression)) {
            lastFound = node;
            return false;
        }
        return true;
    }

    public void finish() {
        addLastFound(null);
    }

    private int getLineDefinition(SimpleNode ast2) {
        while (ast2 instanceof Attribute || ast2 instanceof Call) {
            if (ast2 instanceof Attribute) {
                ast2 = ((Attribute) ast2).value;
            } else {
                Call c = (Call) ast2;
                if (c.func != null) {
                    ast2 = c.func;
                } else {
                    break;
                }
            }
        }
        return ast2.beginLine;
    }

    private void addLastFound(SimpleNode nextNode) {
        if (lastFound != null) {
            int offset = ps.getAbsoluteCursorOffset(getLineDefinition(lastFound) - 1,
                    NodeUtils.getColDefinition(lastFound) - 1);

            //OK, we have the start point, let's calculate the match based on the document
            //(with the next node as a boundary if it was provided)

            int maxOffset = doc.getLength();
            if (nextNode != null) {
                int nextTokenOffset = ps.getAbsoluteCursorOffset(NodeUtils.getLineDefinition(nextNode) - 1,
                        NodeUtils.getColDefinition(nextNode) - 1) + 1;
                if (nextTokenOffset < maxOffset) {
                    maxOffset = nextTokenOffset;
                }
            }

            int j = 0;
            int len = 0;
            for (int i = offset; i < maxOffset && j < selectedText.length; i++, len++) {
                char c;
                try {
                    c = doc.getChar(i);
                } catch (BadLocationException e) {
                    break;
                }
                //Let's see if we have the match
                char c1 = selectedText[j];
                if (c == c1) {
                    j++;
                } else {
                    if (c == '#') {
                        int start = i;
                        i = parsingUtils.eatComments(null, i);
                        len += i - start;
                    } else if (!Character.isWhitespace(c) && c != '\\') {
                        //We removed comments and whitespaces from the original, so, we can ignore it 
                        //here too, but if we found some other char, it's NOT a match...
                        break;
                    }
                }
            }

            if (j == selectedText.length) {

                if (!ps.intersects(offset, len)) {
                    ITextSelection sel = new TextSelection(this.doc, offset, len);
                    duplicates.add(new Tuple<ITextSelection, SimpleNode>(sel, lastFound));
                }
            }
            lastFound = null;
        }
    }

    /**
     * Visit each of the children one by one.
     * @args node The node whose children will be visited.
     */
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    /**
     * @return
     */
    public List<Tuple<ITextSelection, SimpleNode>> getDuplicates() {
        return duplicates;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitInteractive(Interactive node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitExpression(Expression node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitSuite(Suite node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitWithItem(WithItem node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitReturn(Return node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitDelete(Delete node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitFor(For node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitWhile(While node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitIf(If node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitWith(With node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitRaise(Raise node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitAssert(Assert node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitExec(Exec node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitGlobal(Global node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitNonLocal(NonLocal node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitExpr(Expr node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitBreak(Break node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitContinue(Continue node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitIfExp(IfExp node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitDict(Dict node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitSet(Set node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitSetComp(SetComp node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitDictComp(DictComp node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitGeneratorExp(GeneratorExp node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitAwait(Await node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitCompare(Compare node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitRepr(Repr node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitNum(Num node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitStr(Str node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitStrJoin(StrJoin node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitStarred(Starred node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitName(Name node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitList(org.python.pydev.parser.jython.ast.List node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitTuple(org.python.pydev.parser.jython.ast.Tuple node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitEllipsis(Ellipsis node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitSlice(Slice node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitExtSlice(ExtSlice node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitIndex(Index node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

    @Override
    public Object visitComprehension(Comprehension node) throws Exception {
        boolean ret = unhandled_node(node);
        if (ret) {
            traverse(node);
        }
        return null;
    }

}
