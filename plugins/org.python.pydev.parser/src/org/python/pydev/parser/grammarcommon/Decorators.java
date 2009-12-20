package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.decoratorsType;

public class Decorators extends SimpleNode {
    
    public final decoratorsType[] exp;

    public Decorators(decoratorsType[] exp, int id) {
        this.exp = exp;
        this.setId(id);
    }
}
