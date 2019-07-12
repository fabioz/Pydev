package org.python.pydev.shared_core.image;

public interface IImageCache {

    public final static int DECORATION_LOCATION_TOP_RIGHT = 0;
    public final static int DECORATION_LOCATION_BOTTOM_RIGHT = 1;

    /**
     * @param key - relative path to the plugin directory
     * @return the image
     */
    IImageHandle get(String key);

    IImageHandle getImageDecorated(String key, String decoration);

    IImageHandle getImageDecorated(String key, String decoration, int decorationLocation);

    IImageDescriptor getDescriptor(String projectIcon);

    IImageHandle getImageDecorated(String key, String decoration, int decorationLocation,
            String secondDecoration, int secondDecorationLocation);

    IImageHandle getStringDecorated(String key, String stringToAddToDecoration);

}