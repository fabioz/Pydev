/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors;

import java.util.List;

import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.VisitorBase;

public class FindLastLineVisitor extends VisitorBase {

    private SimpleNode lastNode;
    private ISpecialStr lastSpecialStr;

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        this.lastNode = node;
        check(this.lastNode.specialsBefore);
        check(this.lastNode.specialsAfter);
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        check(node.specialsBefore);
        if (node.attr != null)
            node.attr.accept(this);
        if (node.value != null)
            node.value.accept(this);
        check(node.specialsAfter);
        return null;
    }

    private void check(List<Object> specials) {
        if (specials == null) {
            return;
        }
        for (Object obj : specials) {
            if (obj instanceof ISpecialStr) {
                if (lastSpecialStr == null || lastSpecialStr.getBeginLine() <= ((ISpecialStr) obj).getBeginLine()) {
                    lastSpecialStr = (ISpecialStr) obj;
                }
            }
        }
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        if (node.module != null) {
            unhandled_node(node.module);
            node.module.accept(this);
        }

        if (node.names != null) {
            for (int i = 0; i < node.names.length; i++) {
                if (node.names[i] != null) {
                    unhandled_node(node.names[i]);
                    node.names[i].accept(this);
                }
            }
        }
        unhandled_node(node);
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    public SimpleNode getLastNode() {
        return lastNode;
    }

    public ISpecialStr getLastSpecialStr() {
        return lastSpecialStr;
    }

}
