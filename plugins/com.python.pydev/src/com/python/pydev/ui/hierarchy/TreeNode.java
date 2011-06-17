/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.hierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

public class TreeNode {

    public final Image image;
    public final Object data;
    public final List<TreeNode> children = new ArrayList<TreeNode>();
    public final TreeNode parent;
    
    public TreeNode(TreeNode parent, Object data, Image image) {
        this.parent = parent;
        this.data = data;
        if(parent != null){
            parent.children.add(this);
        }
        this.image = image;
    }
    

}
