/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import org.python.pydev.parser.jython.SimpleNode;

public class SimpleAdapter extends AbstractNodeAdapter<SimpleNode> {

    public SimpleAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, SimpleNode node, String endLineDelim) {
        super(module, parent, node, endLineDelim);
    }

}
