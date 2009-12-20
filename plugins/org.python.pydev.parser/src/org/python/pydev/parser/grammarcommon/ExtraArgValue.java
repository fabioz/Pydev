package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.exprType;

public class ExtraArgValue extends SimpleNode {
    final public exprType value;
    final public int id;

    public ExtraArgValue(exprType value, int id) {
        this.value = value;
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
