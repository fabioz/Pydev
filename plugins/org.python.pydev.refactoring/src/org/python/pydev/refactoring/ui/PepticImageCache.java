/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.refactoring.PepticPlugin;
import org.python.pydev.refactoring.messages.Messages;

public class PepticImageCache {

    private Map<String, Image> imageHash = new HashMap<String, Image>(10);

    private Image missing = null;

    public void dispose() {
        for(Image image:imageHash.values()){
            image.dispose();
        }
        if(missing != null){
            missing.dispose();
        }
    }

    public Image get(String key) {
        Image image = imageHash.get(key);
        if(image == null){
            ImageDescriptor desc;
            /* FIXME: Why catching these exceptions */
            try{
                desc = getDescriptor(key);
                image = desc.createImage();
                imageHash.put(key, image);
            }catch(NoClassDefFoundError e){
                return null;
            }catch(UnsatisfiedLinkError e){
                return null;
            }
        }
        return image;
    }

    public ImageDescriptor getDescriptor(String key) {
        return PepticPlugin.imageDescriptorFromPlugin(PepticPlugin.PLUGIN_ID, Messages.imagePath + key);
    }

}
