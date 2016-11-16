package com.python.pydev.analysis.visitors;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorBase;

public class FixLinesVisitor extends VisitorBase {

    private final int startInternalStrLineOffset;
    private final int startInternalStrColOffset;

    public FixLinesVisitor(int startInternalStrLineOffset, int startInternalStrColOffset) {
        this.startInternalStrLineOffset = startInternalStrLineOffset;
        this.startInternalStrColOffset = startInternalStrColOffset;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (node != null) {
            node.traverse(this);
        }
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        if (node.beginLine == 1) {
            node.beginColumn += this.startInternalStrColOffset;
        }
        node.beginLine += this.startInternalStrLineOffset;
        return null;
    }

}