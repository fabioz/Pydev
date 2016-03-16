/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import org.python.pydev.navigator.elements.ISortedElement;
import org.python.pydev.shared_core.structure.TreeNode;

public class InterpreterInfoTreeNode<X> extends TreeNode<X> implements ISortedElement {

    public InterpreterInfoTreeNode(Object parent, X data) {
        super(parent, data);
    }

    @Override
    public int getRank() {
        return ISortedElement.RANK_LIBS;
    }

}
