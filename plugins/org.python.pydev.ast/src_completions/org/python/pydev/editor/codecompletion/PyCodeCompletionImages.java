/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.IToken;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;

public class PyCodeCompletionImages {

    /**
     * Returns an image for the given type
     * @param type
     * @return
     */
    public static Image getImageForType(int type) {
        try {
            ImageCache imageCache = PydevPlugin.getImageCache();
            if (imageCache == null) {
                return null;
            }

            switch (type) {
                case IToken.TYPE_IMPORT:
                    return imageCache.get(UIConstants.COMPLETION_IMPORT_ICON);

                case IToken.TYPE_CLASS:
                    return imageCache.get(UIConstants.COMPLETION_CLASS_ICON);

                case IToken.TYPE_FUNCTION:
                    return imageCache.get(UIConstants.METHOD_ICON);

                case IToken.TYPE_ATTR:
                    return imageCache.get(UIConstants.PUBLIC_ATTR_ICON);

                case IToken.TYPE_BUILTIN:
                    return imageCache.get(UIConstants.BUILTINS_ICON);

                case IToken.TYPE_PARAM:
                case IToken.TYPE_LOCAL:
                case IToken.TYPE_OBJECT_FOUND_INTERFACE:
                case IToken.TYPE_IPYTHON:
                    return imageCache.get(UIConstants.COMPLETION_PARAMETERS_ICON);

                case IToken.TYPE_IPYTHON_MAGIC:
                    return imageCache.get(UIConstants.COMPLETION_IPYTHON_MAGIC);

                case IToken.TYPE_PACKAGE:
                    return imageCache.get(UIConstants.COMPLETION_PACKAGE_ICON);

                case IToken.TYPE_RELATIVE_IMPORT:
                    return imageCache.get(UIConstants.COMPLETION_RELATIVE_IMPORT_ICON);

                case IToken.TYPE_EPYDOC:
                    return imageCache.get(UIConstants.COMPLETION_EPYDOC);

                default:
                    return null;
            }

        } catch (Exception e) {
            Log.logInfo(e);
            return null;
        }
    }

}
