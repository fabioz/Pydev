/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;

public class SimpleAdapter extends AbstractNodeAdapter<SimpleNode> {

    public SimpleAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, SimpleNode node, AdapterPrefs adapterPrefs) {
        super(module, parent, node, adapterPrefs);
    }

}
