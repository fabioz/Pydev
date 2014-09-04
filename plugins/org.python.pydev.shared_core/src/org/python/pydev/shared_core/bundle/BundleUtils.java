/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.bundle;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;

public class BundleUtils {

    public static File getRelative(IPath relative, Bundle bundle) {
        try {
            URL bundleURL = FileLocator.find(bundle, relative, null);
            URL fileURL;
            fileURL = FileLocator.toFileURL(bundleURL);
            File f = new File(fileURL.getPath());

            return f;
        } catch (Exception e) {
            throw new RuntimeException("Can't find relative path:" + relative + " within:" + bundle, e);
        }
    }

}
