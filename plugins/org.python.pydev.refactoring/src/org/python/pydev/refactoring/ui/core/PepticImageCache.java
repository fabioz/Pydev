/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.refactoring.PepticPlugin;
import org.python.pydev.refactoring.messages.Messages;

public class PepticImageCache {
    private Map<String, Image> imageHash = new HashMap<String, Image>();

    public void dispose() {
        for (Image image : imageHash.values()) {
            image.dispose();
        }
    }

    public Image get(String key) {
        Image image = imageHash.get(key);
        if (image == null) {
            ImageDescriptor desc;

            desc = getDescriptor(key);
            image = desc.createImage();
            imageHash.put(key, image);
        }
        return image;
    }

    private ImageDescriptor getDescriptor(String key) {
        return PepticPlugin.imageDescriptorFromPlugin(PepticPlugin.PLUGIN_ID, Messages.imagePath + key);
    }
}
