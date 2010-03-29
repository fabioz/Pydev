package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;

public interface IJJTPythonGrammarState {

    SimpleNode peekNode();

    SimpleNode getLastOpened();

}
