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

    public Object[] getChildren(Object element) {
        Object[] kids = ((File) element).listFiles(PyFileListing.getPyFilesFileFilter(true));
        return kids == null ? new Object[0] : kids;
    }

}
