/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;

public class CodeFoldingVisitor extends EasyASTIteratorWithChildrenVisitor {

    /**
     * Creates the iterator and traverses the passed root so that the results can be gotten.
     */
    public static CodeFoldingVisitor create(SimpleNode root) {
        CodeFoldingVisitor visitor = new CodeFoldingVisitor();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    @Override
    public Object visitIf(If node) throws Exception {
        ASTEntry entry = before(node);
        parents.push(entry);
        traverse(node);
        after(entry);
        parents.pop();
        return null;
    }

    @Override
    protected void doAddNode(ASTEntry entry) {
        ASTEntry parent = entry.parent;
        if (entry.node instanceof If) {
            If entryIf = (If) entry.node;

            //treat elifs
            if (parent != null && parent.node instanceof If) {
                If parentIf = (If) parent.node;
                if (parentIf.orelse != null && parentIf.orelse.body != null && parentIf.orelse.body.length > 0
                        && parentIf.orelse.body[0] == entryIf) {
                    parent.endLine = entry.node.beginLine - 1;
                    if (entry.parent != null) {
                        entry.parent = entry.parent.parent;
                    }
                    super.doAddNode(entry);
                    return;
                }
            }

        }
        super.doAddNode(entry);
    }

    @Override
    protected void after(ASTEntry entry) {
        super.after(entry);

        //if we just added a node, we have to check if it's an If that has an ending else...
        if (entry.node instanceof If) {
            If entryIf = (If) entry.node;
            checkElse(entryIf, entry);
        }
    }

    /**
     * Check if the passed if has an else... If it has, generate a 'fake' If entry for it (so that it's gotten later)
     */
    private void checkElse(If entryIf, ASTEntry parentIf) {
        //treat elses
        if (entryIf.orelse != null && entryIf.orelse.body != null && entryIf.orelse.body.length > 0) {
            stmtType firstOrElseStmt = entryIf.orelse.body[0];

            if (!(firstOrElseStmt instanceof If) && firstOrElseStmt != null) {
                If generatedIf = new If(new BoolOp(BoolOp.And, new exprType[0]), new stmtType[0], new Suite(
                        new stmtType[0]));

                generatedIf.beginLine = firstOrElseStmt.beginLine - 1;
                generatedIf.beginColumn = 1;

                ASTEntry generatedEntry = createEntry();
                generatedEntry.endLine = parentIf.endLine;
                parentIf.endLine = generatedIf.beginLine - 1;
                generatedEntry.node = generatedIf;

                if (generatedEntry.parent != null) {

                    generatedEntry.parent = generatedEntry.parent.parent;
                }

                //actually go on and add the entry...
                super.doAddNode(generatedEntry);
            }
        }
    }

    @Override
    public Object visitFor(For node) throws Exception {
        return defaultVisit(node);
    }

    @Override
    public Object visitWhile(While node) throws Exception {
        return defaultVisit(node);
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        return defaultVisit(node);
    }

    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        return defaultVisit(node);
    }

    @Override
    public Object visitWith(With node) throws Exception {
        return defaultVisit(node);
    }

    //not all methods have bodies... (some have 'atomic' adds)
    @Override
    public Object visitImport(Import node) throws Exception {
        atomic(node);
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        atomic(node);
        return null;
    }

    @Override
    public Object visitStr(Str node) throws Exception {
        atomic(node);
        return null;
    }

    @Override
    protected ASTEntry atomic(SimpleNode node) {
        try {
            unhandled_node(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return super.atomic(node);
    }

    private Object defaultVisit(SimpleNode node) throws Exception {
        unhandled_node(node);
        ASTEntry entry = before(node);
        parents.push(entry);
        traverse(node);
        after(entry);
        parents.pop();
        return null;
    }

    /**
     * Overriden so that we consider the children when iterating (and don't get only the roots)
     * because we're interested in having a flat list in this case, and not actually the hierachical info.
     */
    @Override
    public List<ASTEntry> getAsList(Class... classes) {
        List<ASTEntry> newList = new ArrayList<ASTEntry>();
        for (Iterator<ASTEntry> iter = nodes.iterator(); iter.hasNext();) {
            ASTEntryWithChildren entry = (ASTEntryWithChildren) iter.next();
            checkEntry(newList, entry, classes);
        }
        return newList;
    }

    private void checkEntry(List<ASTEntry> newList, ASTEntryWithChildren entry, Class... classes) {
        if (isFromClass(entry.node, classes)) {
            newList.add(entry);
        }
        if (entry.children != null) {
            for (ASTEntry child : entry.children) {
                checkEntry(newList, (ASTEntryWithChildren) child, classes);
            }
        }
    }

}
