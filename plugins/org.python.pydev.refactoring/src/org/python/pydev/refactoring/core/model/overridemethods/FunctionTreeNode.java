/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model.overridemethods;

import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;
import org.python.pydev.refactoring.core.model.tree.TreeNodeSimple;

public class FunctionTreeNode extends TreeNodeSimple<FunctionDefAdapter> {

    private static final String CLOSEBRACKET = ")";

    private static final String OPENBRACKET = "(";

    public FunctionTreeNode(ITreeNode parent, FunctionDefAdapter adapter) {
        super(parent, adapter);
    }

    @Override
    public String getImageName() {
        return ITreeNode.NODE_METHOD;
    }

    @Override
    public String getLabel() {
        return adapter.getName() + OPENBRACKET + adapter.getSignature() + CLOSEBRACKET;
    }

}
