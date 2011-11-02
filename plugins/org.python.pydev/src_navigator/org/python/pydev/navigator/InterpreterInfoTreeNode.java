/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import org.python.pydev.core.structure.TreeNode;
import org.python.pydev.navigator.elements.ISortedElement;

public class InterpreterInfoTreeNode<X> extends TreeNode<X> implements ISortedElement{
    
    public InterpreterInfoTreeNode(Object parent, X data) {
        super(parent, data);
    }

    public int getRank() {
        return ISortedElement.RANK_LIBS;
    }

}
