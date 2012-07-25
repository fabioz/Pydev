package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.Node;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;

public abstract class AbstractJJTPythonGrammarState {

    public ITreeBuilder builder;

    public void openNodeScope(Node jjtn000) {

    }

    public void clearNodeScope(Node jjtn000) {

    }

    public SimpleNode popNode() {

        return null;
    }

    public void closeNodeScope(Node jjtn000, boolean b) throws ParseException {

    }

    public void closeNodeScope(Node jjtn005, int i) throws ParseException {

    }

    public int nodeArity() {

        return 0;
    }

    public SimpleNode getLastOpened() {

        return null;
    }

    public SimpleNode peekNode(int i) {

        return null;
    }

    public SimpleNode peekNode() {

        return null;
    }

}
