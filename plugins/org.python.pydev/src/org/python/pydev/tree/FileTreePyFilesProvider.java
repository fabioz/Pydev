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
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.python.pydev.ast.listing_utils.PyFileListing;
import org.python.pydev.ast.listing_utils.PyFileListing.PyFileListingFilter;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.structure.LowMemoryArrayList;

/**
 * @author Fabio Zadrozny
 */
public class FileTreePyFilesProvider extends FileTreeContentProvider {

    @Override
    public Object[] getChildren(Object element) {
        LowMemoryArrayList<Object> lst = new LowMemoryArrayList<>();
        try {
            File file = (File) element;
            PyFileListingFilter filter = PyFileListing.getPyFilesFileFilter(true);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(file.toPath())) {
                for (Path path : stream) {
                    File subFile = path.toFile();
                    if (filter.accept(path, subFile, Files.isDirectory(path))) {
                        lst.add(subFile);
                    }
                }
            } catch (IOException e) {
                Log.log(e);
            }
        } catch (Exception e) {
            Log.log(e);
        }

        return lst.toArray(new File[0]);
    }

}
