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

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.shared_ui.ImageCache;

/**
 * @author Fabio Zadrozny
 */
public interface IBundleInfo {

    /**
     * Should return a file from a relative path.
     * 
     * @param relative
     * @return
     * @throws CoreException
     */
    File getRelativePath(IPath relative) throws CoreException;

    String getPluginID();

    ImageCache getImageCache();

}
