/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.Document;
import org.python.pydev.parser.jython.SimpleNode;

public interface IChanges {

    /**
     * This method applies the change it describes and returns the resulting ast
     * @throws Throwable 
     */
    SimpleNode apply(SimpleNode initialAst, Document doc) throws Throwable;

}
