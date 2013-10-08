/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.actions;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

/**
 * @author fabioz
 *
 */
public class OutlineEntry {

    public final SimpleNode node;
    public final String parentClass;
    public final HierarchyNodeModel model;

    public OutlineEntry(ASTEntry entry) {
        this(entry, null);
    }

    public OutlineEntry(ASTEntry entry, HierarchyNodeModel model) {
        this.node = entry.node;
        this.model = model;

        String p = null;
        if (!(this.node instanceof ClassDef)) {
            while (entry.parent != null) {
                if (entry.parent.node instanceof ClassDef) {
                    ClassDef classDef = (ClassDef) entry.parent.node;
                    p = NodeUtils.getRepresentationString(classDef);
                    break;
                }
                entry = entry.parent;
            }
        }
        parentClass = p;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof OutlineEntry))
            return false;
        OutlineEntry other = (OutlineEntry) obj;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        return true;
    }

}
