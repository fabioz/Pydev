/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jul 29, 2003
 */
package org.python.pydev.shared_ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.shared_core.structure.Tuple4;

/**
 * Caches images, releases all on dispose
 */
public class ImageCache {

    /**
     * Helper to decorate an image.
     * 
     * The only method that should be used is: drawDecoration
     */
    private static final class ImageDecorator extends CompositeImageDescriptor {
        private Point size;
        private ImageData base;
        private ImageData decoration;
        private int ox;
        private int oy;

        protected Point getSize() {
            return size;
        }

        protected void drawCompositeImage(int width, int height) {
            this.drawImage(base, 0, 0);
            this.drawImage(decoration, ox, oy);
        }

        public final ImageData drawDecoration(ImageData base, ImageData decoration, int ox, int oy) {
            this.size = new Point(base.width, base.height);
            this.base = base;
            this.decoration = decoration;
            this.ox = ox;
            this.oy = oy;
            return getImageData();
        }
    }

    private final Map<Object, Image> imageHash = new HashMap<Object, Image>(10);
    private final Map<Object, ImageDescriptor> descriptorHash = new HashMap<Object, ImageDescriptor>(10);
    private final ImageDecorator imageDecorator = new ImageDecorator();

    private final URL baseURL;
    private Image missing = null;
    private final Object lock = new Object();
    private final Object descriptorLock = new Object();

    public ImageCache(URL baseURL) {
        this.baseURL = baseURL;
    }

    public void dispose() {
        synchronized (lock) {
            Iterator<Image> e = imageHash.values().iterator();
            while (e.hasNext())
                ((Image) e.next()).dispose();
            if (missing != null) {
                missing.dispose();
            }
        }
    }

    /**
     * @param key - relative path to the plugin directory
     * @return the image
     */
    public Image get(String key) {
        synchronized (lock) {
            Image image = (Image) imageHash.get(key);
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
                } catch (Exception e) {
                    // If image is missing, create a default missing one
                    Log.log("ERROR: Missing image: " + key);
                    if (missing == null) {
                        desc = ImageDescriptor.getMissingImageDescriptor();
                        missing = desc.createImage();
                    }
                    image = missing;
                }
            }
            return image;
        }
    }

    public Image getImageDecorated(String key, String decoration) {
        return getImageDecorated(key, decoration, DECORATION_LOCATION_TOP_RIGHT);
    }

    public final static int DECORATION_LOCATION_TOP_RIGHT = 0;
    public final static int DECORATION_LOCATION_BOTTOM_RIGHT = 1;

    public Image getImageDecorated(String key, String decoration, int decorationLocation) {
        return getImageDecorated(key, decoration, decorationLocation, null, -1);
    }

    /**
     * @param key the key of the image that should be decorated (relative path to the plugin directory)
     * @param decoration the key of the image that should be decorated (relative path to the plugin directory)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Image getImageDecorated(String key, String decoration, int decorationLocation, String secondDecoration,
            int secondDecorationLocation) {
        synchronized (lock) {
            Object cacheKey = new Tuple4(key, decoration, decorationLocation, "imageDecoration");
            if (secondDecoration != null) {
                //Also add the second decoration to the cache key.
                cacheKey = new Tuple3(cacheKey, secondDecoration, secondDecorationLocation);
            }

            Image image = imageHash.get(cacheKey);
            if (image == null) {
                Display display = Display.getCurrent();

                //Note that changing the image data gotten here won't affect the original image.
                ImageData baseImageData = get(key).getImageData();
                image = decorateImage(decoration, decorationLocation, display, baseImageData);
                if (secondDecoration != null) {
                    image = decorateImage(secondDecoration, secondDecorationLocation, display, image.getImageData());
                }
                imageHash.put(cacheKey, image);

            }
            return image;
        }
    }

    private Image decorateImage(String decoration, int decorationLocation, Display display, ImageData baseImageData)
            throws AssertionError {
        Image image;
        ImageData decorationImageData = get(decoration).getImageData();
        ImageData imageData;
        switch (decorationLocation) {
            case DECORATION_LOCATION_TOP_RIGHT:
                imageData = imageDecorator.drawDecoration(baseImageData, decorationImageData, baseImageData.width
                        - decorationImageData.width, 0);
                break;

            case DECORATION_LOCATION_BOTTOM_RIGHT:
                imageData = imageDecorator.drawDecoration(baseImageData, decorationImageData, baseImageData.width
                        - decorationImageData.width, baseImageData.height - decorationImageData.height);
                break;

            default:
                throw new AssertionError("Decoration location not recognized: " + decorationLocation);
        }

        image = new Image(display, imageData);
        return image;
    }

    /**
     * @param key the key of the image that should be decorated (relative path to the plugin directory)
     * @param stringToAddToDecoration the string that should be drawn over the image
     */
    public Image getStringDecorated(String key, String stringToAddToDecoration) {
        synchronized (lock) {
            Tuple3<String, String, String> cacheKey = new Tuple3<String, String, String>(key, stringToAddToDecoration,
                    "stringDecoration");

            Image image = imageHash.get(cacheKey);
            if (image == null) {
                Display display = Display.getCurrent();
                image = new Image(display, get(key), SWT.IMAGE_COPY);
                imageHash.put(cacheKey, image); //put it there (even though it'll still be changed).

                GC gc = new GC(image);

                //		        Color color = new Color(display, 0, 0, 0);
                //		        Color color2 = new Color(display, 255, 255, 255);
                //		        gc.setForeground(color2); 
                //		        gc.setBackground(color2); 
                //		        gc.setFillRule(SWT.FILL_WINDING);
                //		        gc.fillRoundRectangle(2, 1, base-1, base, 2, 2);
                //		        gc.setForeground(color); 
                //		        gc.drawRoundRectangle(6, 0, base, base+1, 2, 2);
                //		        color2.dispose();
                //		        color.dispose();

                Color colorBackground = new Color(display, 255, 255, 255);
                Color colorForeground = new Color(display, 0, 83, 41);

                // get TextFont from preferences
                FontData fontData = FontUtils.getFontData(IFontUsage.IMAGECACHE, true);
                fontData.setStyle(SWT.BOLD);
                Font font = new Font(display, fontData);

                try {
                    gc.setForeground(colorForeground);
                    gc.setBackground(colorBackground);
                    gc.setTextAntialias(SWT.ON);
                    gc.setFont(font);
                    gc.drawText(stringToAddToDecoration, 5, 0, true);
                } catch (Exception e) {
                    Log.log(e);
                } finally {
                    colorBackground.dispose();
                    colorForeground.dispose();
                    font.dispose();
                    gc.dispose();
                }

            }
            return image;
        }
    }

    /**
     * like get, but returns ImageDescription instead of image
     */
    public ImageDescriptor getDescriptor(String key) {
        synchronized (descriptorLock) {
            if (!descriptorHash.containsKey(key)) {
                URL url;
                ImageDescriptor desc;
                try {
                    url = new URL(baseURL, key);
                    desc = ImageDescriptor.createFromURL(url);
                } catch (MalformedURLException e) {
                    Log.log("ERROR: Missing image: " + key);
                    desc = ImageDescriptor.getMissingImageDescriptor();
                }
                descriptorHash.put(key, desc);
                return desc;
            }
            return descriptorHash.get(key);
        }
    }
}
