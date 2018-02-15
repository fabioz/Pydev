/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.shared_ui.bundle;

import org.osgi.framework.Bundle;
import org.python.pydev.core.CoreBundleInfo;
import org.python.pydev.shared_ui.ImageCache;

/**
 * @author Fabio Zadrozny
 */
public class BundleInfo extends CoreBundleInfo implements IBundleInfo {

    public BundleInfo(Bundle bundle) {
        super(bundle);
    }

    private ImageCache imageCache;

    /**
     * @see org.python.pydev.shared_ui.bundle.IBundleInfo#getImageCache()
     */
    @Override
    public ImageCache getImageCache() {
        if (imageCache == null) {
            imageCache = new ImageCache(bundle.getEntry("/"));
        }
        return imageCache;
    }

}
