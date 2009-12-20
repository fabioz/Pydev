package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.exprType;

public class ExtraArg extends SimpleNode {
    public final NameTok tok;
    public final exprType typeDef;

    public ExtraArg(NameTok tok, int id) {
        this(tok, id, null);
    }

    public ExtraArg(NameTok tok, int id, exprType typeDef) {
        this.setId(id);
        this.tok = tok;
        this.typeDef = typeDef;
    }

}
