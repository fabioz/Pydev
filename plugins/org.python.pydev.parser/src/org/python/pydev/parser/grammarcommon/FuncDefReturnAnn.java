package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;

public class FuncDefReturnAnn extends SimpleNode {
    public final SimpleNode node;

    public FuncDefReturnAnn(SimpleNode node) {
        this.node = node;
    }
}
