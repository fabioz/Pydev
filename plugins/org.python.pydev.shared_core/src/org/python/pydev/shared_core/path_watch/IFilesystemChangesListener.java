/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.path_watch;

import java.io.File;

/**
 * @author fabioz
 *
 * Note that modification changes will have added(File) called at this interface.
 */
public interface IFilesystemChangesListener {

    /**
     * @param file the file added. Note that we record to start listening for changes in a directory, and the 
     * file here may be either a child file (in which case the directory had a child added) or the actual directory,
     * in case it was previously removed or an overflow event occurred.
     */
    void added(File file);

    /**
     * @param file the file removed. Note that we record to start listening for changes in a directory, and the 
     * file here may be either a child file (in which case the directory had a child removed) or the actual directory,
     * in case the directory itself was removed or an overflow event occurred.
     */
    void removed(File file);

}
