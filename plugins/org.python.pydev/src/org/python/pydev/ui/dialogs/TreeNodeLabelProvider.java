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
package org.python.pydev.ui.dialogs;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_core.structure.TreeNode;

@SuppressWarnings("rawtypes")
public class TreeNodeLabelProvider extends BaseLabelProvider implements ILabelProvider {

    @Override
    public Image getImage(Object element) {
        return null;
    }

    @Override
    public String getText(Object element) {
        TreeNode n = (TreeNode) element;
        Object data = n.getData();
        if (data == null) {
            return "null";
        }
        return data.toString();
    }

}
