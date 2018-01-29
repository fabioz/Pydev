package org.python.pydev.editor.codecompletion.proposals;

import org.python.pydev.core.IInfo;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;

public class AnalysisImages {

    private static final Object lock = new Object();
    public static IImageHandle autoImportClassWithImportType;
    public static IImageHandle autoImportMethodWithImportType;
    public static IImageHandle autoImportAttributeWithImportType;
    public static IImageHandle autoImportModImportType;

    public static IImageHandle getImageForAutoImportTypeInfo(IInfo info) {
        return getImageForAutoImportTypeInfo(info.getType());
    }

    public static IImageHandle getImageForAutoImportTypeInfo(int infoType) {
        switch (infoType) {
            case IInfo.CLASS_WITH_IMPORT_TYPE:
                if (autoImportClassWithImportType == null) {
                    synchronized (lock) {
                        IImageCache imageCache = SharedUiPlugin.getImageCache();
                        autoImportClassWithImportType = imageCache.getImageDecorated(UIConstants.CLASS_ICON,
                                UIConstants.CTX_INSENSITIVE_DECORATION_ICON,
                                IImageCache.DECORATION_LOCATION_BOTTOM_RIGHT);
                    }
                }
                return autoImportClassWithImportType;

            case IInfo.METHOD_WITH_IMPORT_TYPE:
                if (autoImportMethodWithImportType == null) {
                    synchronized (lock) {
                        IImageCache imageCache = SharedUiPlugin.getImageCache();
                        autoImportMethodWithImportType = imageCache.getImageDecorated(UIConstants.METHOD_ICON,
                                UIConstants.CTX_INSENSITIVE_DECORATION_ICON,
                                IImageCache.DECORATION_LOCATION_BOTTOM_RIGHT);
                    }
                }
                return autoImportMethodWithImportType;

            case IInfo.ATTRIBUTE_WITH_IMPORT_TYPE:
                if (autoImportAttributeWithImportType == null) {
                    synchronized (lock) {
                        IImageCache imageCache = SharedUiPlugin.getImageCache();
                        autoImportAttributeWithImportType = imageCache.getImageDecorated(UIConstants.PUBLIC_ATTR_ICON,
                                UIConstants.CTX_INSENSITIVE_DECORATION_ICON,
                                IImageCache.DECORATION_LOCATION_BOTTOM_RIGHT);
                    }
                }
                return autoImportAttributeWithImportType;

            case IInfo.MOD_IMPORT_TYPE:
                if (autoImportModImportType == null) {
                    synchronized (lock) {
                        IImageCache imageCache = SharedUiPlugin.getImageCache();
                        autoImportModImportType = imageCache.getImageDecorated(UIConstants.FOLDER_PACKAGE_ICON,
                                UIConstants.CTX_INSENSITIVE_DECORATION_ICON,
                                IImageCache.DECORATION_LOCATION_BOTTOM_RIGHT);
                    }
                }
                return autoImportModImportType;

            case IInfo.USE_PACKAGE_ICON:
                IImageCache imageCache = SharedUiPlugin.getImageCache();
                return imageCache.get(UIConstants.COMPLETION_PACKAGE_ICON);

            default:
                throw new RuntimeException("Undefined type.");

        }

    }

    public static IImageHandle classWithImportType;
    public static IImageHandle methodWithImportType;
    public static IImageHandle attributeWithImportType;
    public static IImageHandle modImportType;

    public static IImageHandle getImageForTypeInfo(IInfo info) {
        switch (info.getType()) {
            case IInfo.CLASS_WITH_IMPORT_TYPE:
                if (classWithImportType == null) {
                    synchronized (lock) {
                        IImageCache imageCache = SharedUiPlugin.getImageCache();
                        classWithImportType = imageCache.get(UIConstants.CLASS_ICON);
                    }
                }
                return classWithImportType;

            case IInfo.METHOD_WITH_IMPORT_TYPE:
                if (methodWithImportType == null) {
                    synchronized (lock) {
                        IImageCache imageCache = SharedUiPlugin.getImageCache();
                        methodWithImportType = imageCache.get(UIConstants.METHOD_ICON);
                    }
                }
                return methodWithImportType;

            case IInfo.ATTRIBUTE_WITH_IMPORT_TYPE:
                if (attributeWithImportType == null) {
                    synchronized (lock) {
                        IImageCache imageCache = SharedUiPlugin.getImageCache();
                        attributeWithImportType = imageCache.get(UIConstants.PUBLIC_ATTR_ICON);
                    }
                }
                return attributeWithImportType;

            case IInfo.MOD_IMPORT_TYPE:
                if (modImportType == null) {
                    synchronized (lock) {
                        IImageCache imageCache = SharedUiPlugin.getImageCache();
                        modImportType = imageCache.get(UIConstants.FOLDER_PACKAGE_ICON);
                    }
                }
                return modImportType;
            default:
                throw new RuntimeException("Undefined type.");

        }
    }

}
