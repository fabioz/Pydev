/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.tree;

import java.io.File;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class FileTreeLabelProvider extends LabelProvider {

    ImageCache imageCache;

    /**
     * 
     */
    public FileTreeLabelProvider() {
        imageCache = new ImageCache(PydevPlugin.getDefault().getBundle().getEntry("/"));
    }

    public String getText(Object element) {
        return ((File) element).getName();
    }

    public Image getImage(Object element) {
        if (((File) element).isDirectory()) {
            return imageCache.get(UIConstants.FOLDER_ICON);
        } else {
            return imageCache.get(UIConstants.FILE_ICON);
        }
    }
}