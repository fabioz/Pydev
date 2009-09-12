/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model.constructorfield;

import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;
import org.python.pydev.refactoring.core.model.tree.TreeNodeSimple;

public class TreeNodeField extends TreeNodeSimple<SimpleAdapter> {

    public TreeNodeField(ITreeNode parent, SimpleAdapter adapter) {
        super(parent, adapter);
    }

    @Override
    public String getImageName() {
        return ITreeNode.NODE_ATTRIBUTE;
    }

}
