/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.core;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.refactoring.core.model.tree.ITreeNode;

public class TreeLabelProvider implements ILabelProvider {

    private PepticImageCache cache;

    public TreeLabelProvider() {
        cache = new PepticImageCache();
    }

    public Image getImage(Object element) {
        Image image = null;
        ITreeNode node = (ITreeNode) element;
        image = cache.get(node.getImageName());

        return image;
    }

    public String getText(Object element) {
        if (element instanceof ITreeNode) {
            return ((ITreeNode) element).getLabel();
        }

        return "";
    }

    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
        cache.dispose();
        cache = null;
    }

    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }

}
