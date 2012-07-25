/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model.overridemethods;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;
import org.python.pydev.refactoring.core.model.tree.TreeNodeSimple;

public class ClassTreeNode extends TreeNodeSimple<IClassDefAdapter> {

    public ClassTreeNode(IClassDefAdapter adapter) {
        super(null, adapter);
    }

    @Override
    public Object[] getChildren() {
        List<ITreeNode> children = new ArrayList<ITreeNode>();
        for (FunctionDefAdapter function : this.adapter.getFunctionsInitFiltered()) {
            children.add(new FunctionTreeNode(this, function));
        }
        return children.toArray();
    }

    @Override
    public String getImageName() {
        return ITreeNode.NODE_CLASS;
    }

}
