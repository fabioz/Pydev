package org.python.pydev.editor.codecompletion;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class PyCodeCompletionImages {

    /**
     * Returns an image for the given type
     * @param type
     * @return
     */
    public static Image getImageForType(int type){
        try {
            ImageCache imageCache = PydevPlugin.getImageCache();
            if (imageCache == null)
                return null;
    
            switch (type) {
            case IPyCodeCompletion.TYPE_IMPORT:
                return imageCache.get(UIConstants.COMPLETION_IMPORT_ICON);
    
            case IPyCodeCompletion.TYPE_CLASS:
                return imageCache.get(UIConstants.COMPLETION_CLASS_ICON);
    
            case IPyCodeCompletion.TYPE_FUNCTION:
                return imageCache.get(UIConstants.PUBLIC_METHOD_ICON);
    
            case IPyCodeCompletion.TYPE_ATTR:
                return imageCache.get(UIConstants.PUBLIC_ATTR_ICON);
    
            case IPyCodeCompletion.TYPE_BUILTIN:
                return imageCache.get(UIConstants.BUILTINS_ICON);
    
            case IPyCodeCompletion.TYPE_PARAM:
                return imageCache.get(UIConstants.COMPLETION_PARAMETERS_ICON);
    
            case IPyCodeCompletion.TYPE_PACKAGE:
                return imageCache.get(UIConstants.COMPLETION_PACKAGE_ICON);
                
            case IPyCodeCompletion.TYPE_RELATIVE_IMPORT:
                return imageCache.get(UIConstants.COMPLETION_RELATIVE_IMPORT_ICON);
    
            default:
                return null;
            }
            
        } catch (Exception e) {
            PydevPlugin.log(e, false);
            return null;
        }
    }

}
