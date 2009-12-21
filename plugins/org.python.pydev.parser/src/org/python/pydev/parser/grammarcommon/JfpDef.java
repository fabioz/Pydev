package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;

public class JfpDef extends SimpleNode {
    public final Name nameNode;
    public final exprType typeDef;

    public JfpDef(Name node, exprType typeDef) {
        this.nameNode = node;
        this.typeDef = typeDef;
    }
}
