/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.structure;

import java.util.List;

import junit.framework.TestCase;

public class TreeNodeTest extends TestCase {

    public void testTreeNode() {
        TreeNode<Integer> root = new TreeNode<Integer>(null, 0);
        TreeNode<Integer> c1 = new TreeNode<Integer>(root, 1);
        TreeNode<Integer> c2 = new TreeNode<Integer>(c1, 2);
        TreeNode<Integer> c3 = new TreeNode<Integer>(c1, 3);
        List<TreeNode<Integer>> flattened = root.flatten();
        assertEquals(flattened.size(), 3);
    }

}
