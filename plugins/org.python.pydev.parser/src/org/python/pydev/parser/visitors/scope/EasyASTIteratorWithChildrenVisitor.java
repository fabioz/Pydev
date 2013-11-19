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

import java.util.ArrayList;

/**
 * The ASTEntries in this visitor should know about which context (parent) they're in.
 * 
 * The definition of what's a context (e.g.: should an if generate a new context or only a class/method?)
 * is up to the actual visitor.
 *
 * @author Fabio
 */
public abstract class EasyASTIteratorWithChildrenVisitor extends EasyAstIteratorBase {

    /**
     * Overridden because we deal only with the nodes with children in this iterator
     * 
     * @see org.python.pydev.parser.visitors.scope.EasyAstIteratorBase#createEntry()
     */
    @Override
    protected ASTEntry createEntry() {
        ASTEntry entry;
        if (parents.size() > 0) {
            entry = new ASTEntryWithChildren((ASTEntryWithChildren) parents.peek());
        } else {
            entry = new ASTEntryWithChildren(null);
        }
        return entry;
    }

    /**
     * This implementation only adds it to the flattened list (nodes) if there is no parent.
     * Otherwise (if there is a parent), this implementation will add it to the parents children.
     * 
     * @see org.python.pydev.parser.visitors.scope.EasyAstIteratorBase#doAddNode(org.python.pydev.parser.visitors.scope.ASTEntry)
     */
    @Override
    protected void doAddNode(ASTEntry entry) {
        if (entry.parent == null) {
            super.doAddNode(entry);
        } else {
            ASTEntryWithChildren parent = (ASTEntryWithChildren) entry.parent;
            if (parent.children == null) {
                parent.children = new ArrayList<ASTEntryWithChildren>();
            }
            parent.children.add((ASTEntryWithChildren) entry);
        }
    }
}
