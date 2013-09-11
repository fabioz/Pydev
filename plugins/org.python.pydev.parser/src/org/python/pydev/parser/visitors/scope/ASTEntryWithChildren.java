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

public class ASTEntryWithChildren extends ASTEntry {

    public List<ASTEntryWithChildren> children;

    public ASTEntryWithChildren(ASTEntryWithChildren parent) {
        super(parent);
    }

    public ASTEntryWithChildren(ASTEntryWithChildren parent, SimpleNode node) {
        super(parent, node);
    }

    public ASTEntryWithChildren[] getChildren() {
        if (children == null) {
            return null;
        }
        return children.toArray(new ASTEntryWithChildren[children.size()]);
    }

    public ASTEntryWithChildren getParent() {
        return (ASTEntryWithChildren) this.parent;
    }

}
