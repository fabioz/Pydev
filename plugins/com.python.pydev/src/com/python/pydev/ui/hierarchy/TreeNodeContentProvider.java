/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.hierarchy;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

public class TreeNodeContentProvider implements ITreeContentProvider {

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public Object[] getChildren(Object parentElement) {
        if (parentElement == null) {
            return new Object[0];
        }
        @SuppressWarnings("rawtypes")
        TreeNode m = (TreeNode) parentElement;
        return m.children.toArray();
    }

    public Object getParent(Object element) {
        @SuppressWarnings("rawtypes")
        TreeNode m = (TreeNode) element;
        return m.parent;
    }

    public boolean hasChildren(Object element) {
        @SuppressWarnings("rawtypes")
        TreeNode m = (TreeNode) element;
        return m.children.size() > 0;
    }

}

class HierarchyLabelProvider extends LabelProvider implements IStyledLabelProvider {

    @Override
    public Image getImage(Object element) {
        if (element instanceof TreeNode) {
            @SuppressWarnings("rawtypes")
            TreeNode treeNode = (TreeNode) element;
            return treeNode.image;
        }
        return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
        if (element instanceof TreeNode) {
            @SuppressWarnings("rawtypes")
            TreeNode treeNode = (TreeNode) element;
            Object data = treeNode.data;
            if (data instanceof HierarchyNodeModel) {
                HierarchyNodeModel model = (HierarchyNodeModel) data;
                String spaces = "     ";
                if (model.moduleName != null && model.moduleName.trim().length() > 0) {
                    return model.name + spaces + "(" + model.moduleName + ")";
                }
                return model.name;
            }
            return data.toString();
        }
        return super.getText(element);
    }

    //not there on all versions of eclipse...
    public StyledString getStyledText(Object element) {
        if (element instanceof TreeNode) {
            @SuppressWarnings("rawtypes")
            TreeNode treeNode = (TreeNode) element;
            Object data = treeNode.data;
            if (data instanceof HierarchyNodeModel) {
                HierarchyNodeModel model = (HierarchyNodeModel) data;
                String spaces = "     ";
                StyledString styledString = new StyledString(model.name + spaces);
                if (model.moduleName != null && model.moduleName.trim().length() > 0) {
                    Styler styler = StyledString.createColorRegistryStyler(JFacePreferences.DECORATIONS_COLOR, null);
                    styledString.append("(" + model.moduleName + ")", styler);
                }
                return styledString;
            }
            return new StyledString(data.toString());
        }
        return new StyledString(element == null ? "" : element.toString());
    }

}
