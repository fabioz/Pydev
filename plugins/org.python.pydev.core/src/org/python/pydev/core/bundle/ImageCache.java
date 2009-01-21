/*
 * Author: atotic
 * Created: Jul 29, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.core.bundle;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Caches images, releases all on dispose
 */
public class ImageCache {
    
    
    private Map<String, Image> imageHash = new HashMap<String, Image>(10);
    private URL baseURL; 
    private Image missing = null;
    
    public ImageCache(URL baseURL) {
        this.baseURL = baseURL;
    }
    
    
    protected ImageCache() {
    }


    public void dispose() {
        Iterator e = imageHash.values().iterator();
        while (e.hasNext())
             ((Image) e.next()).dispose();
        if (missing != null) {
            missing.dispose();
        }
    }

    /**
     * @param key - relative path to the plugin directory
     * @return the image
     */
    public Image get(String key) {
        Image image = (Image)imageHash.get(key);
        if (image == null) {
            ImageDescriptor desc;
            try {
                desc = getDescriptor(key);
                image = desc.createImage();
                imageHash.put(key, image);
            } catch (NoClassDefFoundError e) {
                //we're in tests...
                return null;
            } catch (UnsatisfiedLinkError e) {
                //we're in tests...
                return null;
            } catch (MalformedURLException e) {
                // If image is missing, create a default missing one
                System.err.println("ERROR: Missing image: " + key);
                if (missing == null) {
                    desc =  ImageDescriptor.getMissingImageDescriptor();
                    missing = desc.createImage();
                }
                image = missing;
            }            
        }
        return image;
    }

    /**
     * like get, but returns ImageDescription instead of image
     */
    public ImageDescriptor getDescriptor(String key) throws MalformedURLException {
        URL url = new URL(baseURL, key);
        return ImageDescriptor.createFromURL(url);
    }
}
