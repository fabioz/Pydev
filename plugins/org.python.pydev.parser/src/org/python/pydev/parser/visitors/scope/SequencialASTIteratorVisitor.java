/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors.scope;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;

public class SequencialASTIteratorVisitor extends EasyAstIteratorBase {

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        atomic(node);
        super.unhandled_node(node);
        return null;
    }

    /**
     * Creates the iterator and traverses the passed root so that the results can be gotten.
     */
    public static SequencialASTIteratorVisitor create(SimpleNode root, boolean recursive) {
        SequencialASTIteratorVisitor visitor;
        if (recursive) {
            visitor = new SequencialASTIteratorVisitor();
        } else {
            visitor = new SequencialASTIteratorVisitor() {
                @Override
                public Object visitClassDef(ClassDef node) throws Exception {
                    if (node == root) {
                        return super.visitClassDef(node);
                    }
                    return null;
                }

                @Override
                public Object visitFunctionDef(FunctionDef node) throws Exception {
                    if (node == root) {
                        return super.visitFunctionDef(node);
                    }
                    return null;
                }
            };

        }
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

}
