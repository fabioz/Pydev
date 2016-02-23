/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors.scope;

import java.util.Iterator;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;

/**
 * @author Hussain Bohra
 */
public class EasyASTIteratorWithLoop extends EasyAstIteratorBase {
    /**
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFor(org.python.pydev.parser.jython.ast.For)
     */
    @Override
    public Object visitFor(For node) throws Exception {
        return createASTNode(node);
    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorWhile#visitFor(org.python.pydev.parser.jython.ast.While)
     */
    @Override
    public Object visitWhile(While node) throws Exception {
        return createASTNode(node);

    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorWhile#visitFor(org.python.pydev.parser.jython.ast.TryExcept)
     */
    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        return createASTNode(node);

    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorWhile#visitFor(org.python.pydev.parser.jython.ast.TryFinally)
     */
    @Override
    public Object visitWith(With node) throws Exception {
        return createASTNode(node);

    }

    /**
     * @see org.python.pydev.parser.jython.ast.VisitorWhile#visitFor(org.python.pydev.parser.jython.ast.TryFinally)
     */
    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        return createASTNode(node);

    }

    private Object createASTNode(SimpleNode node) throws Exception {
        ASTEntry entry = before(node);
        parents.push(entry);
        traverse(node);
        parents.pop();
        after(entry);

        return null;
    }

    /**
     * 
     * @return an iterator for For, While,TryExcept, TryFinally and With
     *         definitions
     */
    public Iterator<ASTEntry> getIterators() {
        return getIterator(new Class[] { For.class, While.class, TryExcept.class, TryFinally.class, With.class });
    }

    /**
     * Creates the iterator and traverses the passed root so that the results
     * can be gotten.
     */
    public static EasyASTIteratorWithLoop create(SimpleNode root) {
        EasyASTIteratorWithLoop visitor = new EasyASTIteratorWithLoop();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

}
