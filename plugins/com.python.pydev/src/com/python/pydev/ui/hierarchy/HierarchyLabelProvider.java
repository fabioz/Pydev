package com.python.pydev.ui.hierarchy;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;

class HierarchyLabelProvider extends LabelProvider implements IStyledLabelProvider {

    @Override
    public Image getImage(Object element) {
        if (element instanceof DataAndImageTreeNode) {
            @SuppressWarnings("rawtypes")
            DataAndImageTreeNode treeNode = (DataAndImageTreeNode) element;
            return treeNode.image;
        }
        return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
        if (element instanceof DataAndImageTreeNode) {
            @SuppressWarnings("rawtypes")
            DataAndImageTreeNode treeNode = (DataAndImageTreeNode) element;
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
        if (element instanceof DataAndImageTreeNode) {
            @SuppressWarnings("rawtypes")
            DataAndImageTreeNode treeNode = (DataAndImageTreeNode) element;
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