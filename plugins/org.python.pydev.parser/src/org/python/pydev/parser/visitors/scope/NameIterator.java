/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 8, 2006
 */
package org.python.pydev.parser.visitors.scope;

import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;

/**
 * Iterator the passes the nodes getting the subclasses of Name and NameTok
 */
public class NameIterator implements Iterator<ASTEntry> {

    private ASTEntry next = null;
    private Iterator<ASTEntry> nodesIt;

    public NameIterator(List<ASTEntry> nodes) {
        this.nodesIt = nodes.iterator();
        setNext();
    }

    private void setNext() {
        while (nodesIt.hasNext()) {
            ASTEntry entry = nodesIt.next();
            if (entry.node instanceof Name || entry.node instanceof NameTok) {
                next = entry;
                return;
            }
        }
        next = null;
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public ASTEntry next() {
        ASTEntry n = next;
        setNext();
        return n;
    }

    @Override
    public void remove() {
        throw new RuntimeException("Not Impl");
    }

}
