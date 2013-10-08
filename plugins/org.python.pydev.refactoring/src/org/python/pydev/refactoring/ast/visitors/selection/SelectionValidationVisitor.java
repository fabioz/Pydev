/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.visitors.selection;

import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.Yield;

public class SelectionValidationVisitor extends VisitorBase {
    private Class<?>[] invalidNode = new Class<?>[] { Break.class, ClassDef.class, Continue.class, FunctionDef.class,
            Pass.class, Return.class, Yield.class };

    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (node != null) {
            validateNode(node);
            node.traverse(this);
        }
    }

    private void validateNode(SimpleNode node) throws SelectionException {
        if (node instanceof ImportFrom) {
            if (AbstractVisitor.isWildImport((ImportFrom) node)) {
                //Wild import
                throw new SelectionException("Selection may not contain a wild import statement (Line "
                        + node.beginLine + "," + node.beginColumn + ")");
            }
        }
        for (Class<?> clazz : invalidNode) {
            if (clazz == node.getClass()) {
                throw new SelectionException(node);
            }
        }
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        // visitorbase will call traverse
        return null;
    }

}
