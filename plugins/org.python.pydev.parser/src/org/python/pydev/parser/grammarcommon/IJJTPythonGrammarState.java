package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;

public interface IJJTPythonGrammarState {

    void pushNodePos(int beginLine, int beginColumn);

    SimpleNode peekNode();

    SimpleNode setNodePos();

    SimpleNode getLastOpened();

}
