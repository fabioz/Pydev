package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;

public final class NullTreeBuilder implements ITreeBuilder {

    private final Name nameConstant = new Name("", Name.Load, false);

    public SimpleNode closeNode(SimpleNode sn, int num) throws Exception {
        return null;
    }

    public SimpleNode openNode(int id) {
        switch (id) {

            case ITreeConstants.JJTNAME:
            case ITreeConstants.JJTDOTTED_NAME:
                //If null is returned here, we may have an NPE.
                return nameConstant;

        }
        return null;
    }

    public SimpleNode getLastOpened() {
        return null;
    }

}
