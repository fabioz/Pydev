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
