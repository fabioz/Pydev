package org.python.pydev.tree;

import java.io.File;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.ImageCache;
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