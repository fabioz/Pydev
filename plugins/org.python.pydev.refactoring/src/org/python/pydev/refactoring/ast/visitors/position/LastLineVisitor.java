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

package org.python.pydev.refactoring.ast.visitors.position;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * A node's last body statement isn't always the last line. We have to traverse the statement's AST node in many cases: e.g. a nested class,
 * any control statement, etc.
 * 
 * @author Ueli Kistler
 * 
 */
public class LastLineVisitor extends VisitorBase {

    private int lastLine;

    public LastLineVisitor() {
        lastLine = 0;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (node != null) {
            node.traverse(this);
        }
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        int lineEnd = NodeUtils.getLineEnd(node);
        if (lineEnd > lastLine) {
            lastLine = lineEnd;
        }
        return null;
    }

    public int getLastLine() {
        return lastLine;
    }

}
