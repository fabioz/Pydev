/******************************************************************************
* Copyright (C) 2007-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.utils;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

public class DirectoryTraverser implements Iterable<String> {
    private Pattern filter;
    private File baseDirectory;

    public DirectoryTraverser(File baseDirectory, Pattern filter) {
        this.filter = filter;
        this.baseDirectory = baseDirectory;

        File directory = baseDirectory;

        if (!directory.exists()) {
            throw new RuntimeException("Given directory doesn't exist");
        }

        if (!directory.isDirectory()) {
            throw new RuntimeException("Specified path is not a directory");
        }
    }

    public List<String> getAllFiles() {
        LinkedList<String> files = new LinkedListWarningOnSlowOperations<String>();
        traverse("", files);

        return files;
    }

    public Iterator<String> iterator() {
        return getAllFiles().iterator();
    }

    private void traverse(String relDir, List<String> files) {
        File currentDir = new File(baseDirectory, relDir);

        String[] list = currentDir.list();
        if (list != null) {
            for (String entryName : list) {
                File absEntry = new File(currentDir, entryName);

                String relPath;

                /* add current relative dir if necessary */
                if (relDir.length() != 0) {
                    relPath = relDir + entryName;
                } else {
                    relPath = entryName;
                }

                if (absEntry.isDirectory()) {
                    traverse(relPath + File.separator, files);
                } else {
                    Matcher matcher = filter.matcher(absEntry.getAbsolutePath());

                    if (matcher.matches()) {
                        files.add(relPath);
                    }
                }
            }
        }
    }
}
