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
 * @author Fabio Zadrozny
 */
public class PyPreviousMethod extends PyMethodNavigation {

	// me is the last node w
    public ASTEntry getSelect(SimpleNode ast, int line) {
        EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(ast);
        Iterator<ASTEntry> classesAndMethodsIterator = visitor.getClassesAndMethodsIterator();
        ASTEntry last = null;
        
        while(classesAndMethodsIterator.hasNext()){
            ASTEntry entry = classesAndMethodsIterator.next();
            if(entry.node.beginLine-1 < line ){
                last = entry;
            }
        }
        return last;
    }

    @Override
    protected boolean goToEndOfFile() {
        return false;
    }

    @Override
    protected boolean goToStartOfFile() {
        return true;
    }
}
