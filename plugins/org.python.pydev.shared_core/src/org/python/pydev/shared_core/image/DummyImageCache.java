package org.python.pydev.shared_core.image;

public class DummyImageCache implements IImageCache {

    @Override
    public IImageHandle get(String key) {
        return null;
    }

    @Override
    public IImageHandle getImageDecorated(String key, String decoration) {
        return null;
    }

    @Override
    public IImageHandle getImageDecorated(String key, String decoration, int decorationLocation) {
        return null;
    }

    @Override
    public IImageDescriptor getDescriptor(String projectIcon) {
        return null;
    }

    @Override
    public IImageHandle getImageDecorated(String key, String decoration, int decorationLocation,
            String secondDecoration, int secondDecorationLocation) {
        return new IImageHandle() {

            @Override
            public Object getImageData() {
                return null;
            }

            @Override
            public Object getImage() {
                return null;
            }
        };
    }

    @Override
    public IImageHandle getStringDecorated(String key, String stringToAddToDecoration) {
        return null;
    }

}
