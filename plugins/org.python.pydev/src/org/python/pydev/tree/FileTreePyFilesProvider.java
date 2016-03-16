/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 13, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.tree;

import java.io.File;

import org.python.pydev.utils.PyFileListing;

/**
 * @author Fabio Zadrozny
 */
public class FileTreePyFilesProvider extends FileTreeContentProvider {

    @Override
    public Object[] getChildren(Object element) {
        Object[] kids = ((File) element).listFiles(PyFileListing.getPyFilesFileFilter(true));
        return kids == null ? new Object[0] : kids;
    }

}
