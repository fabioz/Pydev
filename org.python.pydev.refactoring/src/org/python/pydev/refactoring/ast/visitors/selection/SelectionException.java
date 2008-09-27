/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.visitors.selection;

import org.python.pydev.parser.jython.SimpleNode;

public class SelectionException extends Exception {

    private static final long serialVersionUID = 1L;

    private SimpleNode node;

    public SelectionException(SimpleNode node) {
        this.node = node;
    }

    @Override
    public String getMessage() {
        return "Selection may not contain a(n) " + node.getClass().getSimpleName() + " statement (Line " + node.beginLine + ","
                + node.beginColumn + ")";
    }
}
