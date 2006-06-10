/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import java.util.Iterator;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

/**
 * One-trick pony, finds the next method.
 */
public class PyNextMethod extends PyMethodNavigation{

	/**
	 * Gets the next method/class definition
     * @param line is in doc coords
	 */
	public ASTEntry getSelect(SimpleNode ast, int line) {
        EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(ast);
        Iterator<ASTEntry> classesAndMethodsIterator = visitor.getClassesAndMethodsIterator();
        while(classesAndMethodsIterator.hasNext()){
            ASTEntry entry = classesAndMethodsIterator.next();
            if(entry.node.beginLine-1 > line ){
                return entry;
            }
        }
        return null;
	}

    @Override
    protected boolean goToEndOfFile() {
        return true;
    }

    @Override
    protected boolean goToStartOfFile() {
        return false;
    }
}
