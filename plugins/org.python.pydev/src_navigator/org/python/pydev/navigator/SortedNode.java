package org.python.pydev.navigator;

import org.python.pydev.core.structure.TreeNode;
import org.python.pydev.navigator.elements.ISortedElement;

public class SortedNode<X> extends TreeNode<X> implements ISortedElement{
    
    public SortedNode(SortedNode<X> parent, X data) {
        super(parent, data);
    }

    public int getRank() {
        return ISortedElement.RANK_LIBS;
    }

}
