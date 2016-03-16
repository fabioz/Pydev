/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
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

    @Override
    public Image getImage(Object element) {
        Image image = null;
        ITreeNode node = (ITreeNode) element;
        image = cache.get(node.getImageName());

        return image;
    }

    @Override
    public String getText(Object element) {
        if (element instanceof ITreeNode) {
            return ((ITreeNode) element).getLabel();
        }

        return "";
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
        cache.dispose();
        cache = null;
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

}
