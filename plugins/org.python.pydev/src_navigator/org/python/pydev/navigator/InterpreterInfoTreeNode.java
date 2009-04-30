package org.python.pydev.navigator;

import org.python.pydev.core.structure.TreeNode;
import org.python.pydev.navigator.elements.ISortedElement;

public class InterpreterInfoTreeNode<X> extends TreeNode<X> implements ISortedElement{
    
    public InterpreterInfoTreeNode(InterpreterInfoTreeNode<X> parent, X data) {
        super(parent, data);
    }

    public int getRank() {
        return ISortedElement.RANK_LIBS;
    }

}
