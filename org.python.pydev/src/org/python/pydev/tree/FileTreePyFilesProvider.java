/*
 * Created on Oct 13, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.tree;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Fabio Zadrozny
 */
public class FileTreePyFilesProvider extends FileTreeContentProvider {

    public Object[] getChildren(Object element) {
        Object[] kids = ((File) element).listFiles(new FileFilter(){

            public boolean accept(File pathname) {
                return pathname.isDirectory() || pathname.toString().endsWith(".py");
            }
            
        });
        return kids == null ? new Object[0] : kids;
    }

}
