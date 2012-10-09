/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model.constructorfield;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;
import org.python.pydev.refactoring.core.model.tree.TreeNodeSimple;

public class TreeNodeClassField extends TreeNodeSimple<IClassDefAdapter> {

    public TreeNodeClassField(IClassDefAdapter adapter) {
        super(null, adapter);
    }

    @Override
    public Object[] getChildren() {
        List<ITreeNode> children = new ArrayList<ITreeNode>();
        for (SimpleAdapter attribute : this.adapter.getAttributes()) {
            children.add(new TreeNodeField(this, attribute));
        }
        return children.toArray();
    }

    @Override
    public String getImageName() {
        return ITreeNode.NODE_CLASS;
    }
}
