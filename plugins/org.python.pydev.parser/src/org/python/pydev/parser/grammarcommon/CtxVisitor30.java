package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Call;

public class CtxVisitor30 extends CtxVisitor {

    public CtxVisitor30(JJTPythonGrammarState stack) {
        super(stack);
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        //Case:
        // (1, *range(3))
        //throw new ParseException("can't assign to function call", node);
        return null;
    }

    @Override
    public Object visitListComp(org.python.pydev.parser.jython.ast.ListComp node) throws Exception {
        //Case:
        // (1, *range(3))
        //throw new ParseException("can't assign to list comprehension call", node);
        return null;
    }

    @Override
    public Object unhandled_node(SimpleNode node) throws Exception {
        // Any other 'starred' case.
        return null;
    }
}
