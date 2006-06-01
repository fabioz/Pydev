/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * This class should be used to do changes in the ast.
 *  
 * @author Fabio
 */
public class PyASTChanger {

    private Tuple<SimpleNode, Throwable> tup;

    public PyASTChanger(Document doc) {
        Tuple<SimpleNode, Throwable> ret = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, -1));
        this.tup = ret;
    }

    public SimpleNode getAST() {
        return this.tup.o1;
    }

}
