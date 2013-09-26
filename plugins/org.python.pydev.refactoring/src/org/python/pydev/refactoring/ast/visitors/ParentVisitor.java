/******************************************************************************
* Copyright (C) 2007-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.ast.visitors;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.shared_core.structure.FastStack;

public class ParentVisitor extends VisitorBase {
    protected FastStack<SimpleNode> stack = new FastStack<SimpleNode>(20);

    public ParentVisitor() {
        stack.push(null);
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.parent = stack.peek();
        stack.push(node);
        node.traverse(this);
        stack.pop();
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }
}
