/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 3, 2006
 */
package org.python.pydev.parser.visitors.comparator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.visitors.NodeUtils;

class FlatVisitor extends VisitorBase {

    List<SimpleNode> visited = new ArrayList<SimpleNode>();

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        visited.add(node);
        //System.out.println("adding:"+node.getClass().getName());
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

}

public class SimpleNodeComparator {

    public void compare(SimpleNode original, SimpleNode newNode) throws Exception, DifferException {
        FlatVisitor flatVisitorOriginal = new FlatVisitor();
        flatVisitorOriginal.traverse(original);

        FlatVisitor flatVisitor = new FlatVisitor();
        flatVisitor.traverse(original);

        Iterator<SimpleNode> it = flatVisitorOriginal.visited.iterator();
        Iterator<SimpleNode> it2 = flatVisitor.visited.iterator();

        while (it.hasNext() && it2.hasNext()) {
            SimpleNode node = it.next();
            SimpleNode node2 = it2.next();
            if (node.getClass() != node2.getClass()) {
                throw new DifferException("Nodes differ. " + node.getClass().getName() + " != "
                        + node2.getClass().getName());
            }
            String s1 = NodeUtils.getFullRepresentationString(node);
            String s2 = NodeUtils.getFullRepresentationString(node2);
            if ((s1 == null && s2 != null) || (s1 != null && s2 == null)) {
                throw new DifferException("Nodes differ. (s1 == null && s2 != null) || (s1 != null && s2 == null)");
            }
            if (s1 == s2) { //null
                continue;
            }
            if (s1.equals(s2.replaceAll("\r", "")) == false) {
                throw new DifferException("Nodes differ. s1 != s2 \n-->" + s1 + "<--\n!=\n-->" + s2 + "<--");
            }
        }
    }

}
