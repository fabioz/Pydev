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
import org.python.pydev.shared_core.string.StringUtils;

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
        if (node != null) {
            node.traverse(this);
        }
    }

}

public class SimpleNodeComparator {

    LineColComparator lineColComparator;

    public SimpleNodeComparator() {
        this(false);
    }

    public SimpleNodeComparator(boolean compareLineCol) {
        this(compareLineCol ? new RegularLineComparator() : null);
    }

    public static abstract class LineColComparator {

        public abstract void compareLineCol(SimpleNode node, SimpleNode node2) throws DifferException;

    }

    public static class RegularLineComparator extends LineColComparator {

        @Override
        public void compareLineCol(SimpleNode node, SimpleNode node2) throws DifferException {
            if (node.beginLine != node2.beginLine) {
                throw new DifferException(StringUtils.format("Nodes beginLine differ. (%s != %s) (%s -- %s)",
                        node.beginLine, node2.beginLine, node, node2));
            }
        }

    }

    public SimpleNodeComparator(LineColComparator lineColComparator) {
        this.lineColComparator = lineColComparator;
    }

    public void compare(SimpleNode original, SimpleNode newNode) throws Exception, DifferException {
        FlatVisitor flatVisitorOriginal = new FlatVisitor();
        flatVisitorOriginal.traverse(original);

        FlatVisitor flatVisitor = new FlatVisitor();
        flatVisitor.traverse(newNode);

        Iterator<SimpleNode> it = flatVisitorOriginal.visited.iterator();
        Iterator<SimpleNode> it2 = flatVisitor.visited.iterator();

        while (true) {
            if (!it.hasNext() && !it2.hasNext()) {
                break;
            }
            SimpleNode node = it.hasNext() ? it.next() : null;
            SimpleNode node2 = it2.hasNext() ? it2.next() : null;
            if (node == null || node2 == null) {
                throw new DifferException("Nodes differ. " + node + " != " + node2);
            }
            if (node.getClass() != node2.getClass()) {
                throw new DifferException("Nodes differ. " + node.getClass().getName() + " != "
                        + node2.getClass().getName());
            }
            if (lineColComparator != null) {
                lineColComparator.compareLineCol(node, node2);
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
