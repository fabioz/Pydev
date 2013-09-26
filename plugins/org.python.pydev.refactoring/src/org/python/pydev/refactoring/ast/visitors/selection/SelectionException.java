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

import org.python.pydev.parser.jython.SimpleNode;

public class SelectionException extends Exception {

    private static final long serialVersionUID = 1L;

    private transient SimpleNode node;

    private String msg;

    public SelectionException(String msg) {
        this.msg = msg;
    }

    public SelectionException(SimpleNode node) {
        this.node = node;
    }

    @Override
    public String getMessage() {
        if (this.msg == null) {
            this.msg = "Selection may not contain a(n) " + node.getClass().getSimpleName() + " statement (Line "
                    + node.beginLine + "," + node.beginColumn + ")";
        }
        return this.msg;
    }
}
