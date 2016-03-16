/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package org.python.pydev.parser.visitors.scope;

import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.visitors.NodeUtils;

public class OutlineCreatorVisitor extends EasyASTIteratorWithChildrenVisitor {

    public static OutlineCreatorVisitor create(SimpleNode ast) {
        OutlineCreatorVisitor visitor = new OutlineCreatorVisitor();
        if (ast == null) {
            return visitor;
        }

        try {
            ast.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    private boolean isInAssign;

    @Override
    public void traverse(SimpleNode node) throws Exception {
        checkSpecials(node.specialsBefore);
        super.traverse(node);
        checkSpecials(node.specialsAfter);
    }

    @Override
    public void traverse(FunctionDef node) throws Exception {
        checkSpecials(node.specialsBefore);
        super.traverse(node);
        checkSpecials(node.specialsAfter);
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImport(org.python.pydev.parser.jython.ast.Import)
     */
    @Override
    public Object visitImport(Import node) throws Exception {
        atomic(node);
        return super.visitImport(node);
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImportFrom(org.python.pydev.parser.jython.ast.ImportFrom)
     */
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        atomic(node);
        return super.visitImportFrom(node);
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        isInAssign = true;
        try {
            DefinitionsASTIteratorVisitor.visitAssign(this, node, false);
        } finally {
            isInAssign = false;
        }
        traverse(node);

        return null;

    }

    @Override
    public Object visitIf(If node) throws Exception {
        if (NodeUtils.isIfMAinNode(node)) {
            atomic(node);
            return null;
        } else {
            return super.visitIf(node);
        }
    }

    @Override
    protected void doAddNode(ASTEntry entry) {
        SimpleNode node = entry.node;

        if (node instanceof commentType) {
            commentType type = (commentType) node;
            if (type.beginColumn == 1) {
                entry.parent = null; //top-level
            } else {

                //try to match it to some other indentation already set.
                ASTEntryWithChildren lastAdded = null;
                if (nodes != null && nodes.size() > 0) {
                    lastAdded = (ASTEntryWithChildren) nodes.get(nodes.size() - 1);
                }

                while (lastAdded != null) {
                    if (lastAdded.node == null) {
                        break;
                    }

                    //if it is equal to the indentation of this node, it's parent is the same, if it is higher
                    //it is a child or a child's child...
                    if (lastAdded.node.beginColumn == node.beginColumn) {
                        entry.parent = lastAdded.parent;
                        break;

                    } else if (node.beginColumn > lastAdded.node.beginColumn) {
                        //it's higher, so, check the last children of lastAdded for a possible parent...
                        entry.parent = lastAdded;
                        List<ASTEntryWithChildren> children = lastAdded.children;
                        if (children != null && children.size() > 0) {
                            lastAdded = children.get(children.size() - 1);
                        } else {
                            break;
                        }

                    } else {
                        //it's less, so, the parent is already set...
                        break;
                    }
                }
            }
        }

        super.doAddNode(entry);
    }

    private void checkSpecials(List<Object> specials) {
        if (specials == null || isInAssign) {
            return;
        }
        for (Object object : specials) {
            if (object instanceof commentType) {
                commentType type = (commentType) object;
                String trimmed = type.id.trim();

                if (trimmed.startsWith("#---") || trimmed.endsWith("---")) {
                    atomic(type);
                }
            }
        }
    }

}
