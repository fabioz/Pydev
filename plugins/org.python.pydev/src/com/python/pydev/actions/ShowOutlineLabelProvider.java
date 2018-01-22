/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.outline.ParsedItem;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_ui.ImageCache;

public final class ShowOutlineLabelProvider extends LabelProvider implements IStyledLabelProvider {

    @Override
    public Image getImage(Object element) {
        SimpleNode n = null;
        if (element instanceof DataAndImageTreeNode) {
            @SuppressWarnings("rawtypes")
            DataAndImageTreeNode treeNode = (DataAndImageTreeNode) element;
            element = treeNode.data;
        }
        if (element instanceof OutlineEntry) {
            n = ((OutlineEntry) element).node;
        }

        if (element instanceof ASTEntry) {
            n = ((ASTEntry) element).node;
        }
        if (n != null) {
            ImageCache imageCache = PydevPlugin.getImageCache();
            if (imageCache == null) {
                return null;
            }

            return ParsedItem.getImageForNode(imageCache, n, null);
        }
        return null;
    }

    @Override
    public String getText(Object element) {
        if (element instanceof DataAndImageTreeNode) {
            @SuppressWarnings("rawtypes")
            DataAndImageTreeNode treeNode = (DataAndImageTreeNode) element;
            element = treeNode.data;
        }
        if (element instanceof OutlineEntry) {
            OutlineEntry entry = (OutlineEntry) element;
            String start = NodeUtils.getFullRepresentationString(entry.node);
            if (entry.model != null) {
                FastStringBuffer suffix = new FastStringBuffer("  (", entry.model.name.length() + 50)
                        .append(entry.model.name);
                if (entry.model.moduleName != null && entry.model.moduleName.length() > 0) {
                    suffix.append(" - ").append(entry.model.moduleName);
                }
                suffix.append(')');

                return start + suffix.toString();
            }
            return start;
        }
        if (element instanceof ASTEntry) {
            return NodeUtils.getFullRepresentationString(((ASTEntry) element).node);
        }
        return element.toString();
    }

    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof DataAndImageTreeNode) {
            @SuppressWarnings("rawtypes")
            DataAndImageTreeNode treeNode = (DataAndImageTreeNode) element;
            element = treeNode.data;
        }
        if (element instanceof OutlineEntry) {
            OutlineEntry entry = (OutlineEntry) element;
            String start = NodeUtils.getFullRepresentationString(entry.node);
            if (entry.model != null) {
                FastStringBuffer suffix = new FastStringBuffer("    (", entry.model.name.length() + 50)
                        .append(entry.model.name);
                if (entry.model.moduleName != null && entry.model.moduleName.length() > 0) {
                    suffix.append(" - ").append(entry.model.moduleName);
                }
                suffix.append(')');

                return new StyledString(start).append(suffix.toString(), StyledString.QUALIFIER_STYLER);

            } else if (entry.parentClass != null) {
                FastStringBuffer suffix = new FastStringBuffer("    (", entry.parentClass.length() + 4).append(
                        entry.parentClass).append(')');

                return new StyledString(start).append(suffix.toString(), StyledString.QUALIFIER_STYLER);

            }
            return new StyledString(start);
        }
        if (element instanceof ASTEntry) {
            return new StyledString(NodeUtils.getFullRepresentationString(((ASTEntry) element).node));
        }
        return new StyledString(element.toString());
    }
}