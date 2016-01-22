/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.io;

import java.io.File;
import java.io.FileFilter;

import junit.framework.TestCase;

public class FileUtilsTest extends TestCase {

    private File baseDir;

    @Override
    protected void setUp() throws Exception {
        baseDir = new File(FileUtils.getFileAbsolutePath(new File("FileUtilsTest.temporary_dir")));
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
        if (baseDir.exists()) {
            throw new AssertionError("Not expecting: " + baseDir + " to exist.");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
    }

    public void testGetLastModifiedTime() throws Exception {
        baseDir.mkdir();
        File dir1 = new File(baseDir, "dir1");
        dir1.mkdir();
        File dir2 = new File(baseDir, "dir2");
        dir2.mkdir();

        File f1 = new File(dir1, "f1.py");
        FileUtils.writeStrToFile("test", f1);
        synchronized (this) {
            this.wait(50);
        }
        File f1a = new File(dir1, "f1a.txt");
        FileUtils.writeStrToFile("test", f1);
        synchronized (this) {
            this.wait(50);
        }
        File f2 = new File(dir2, "f2.txt");
        FileUtils.writeStrToFile("test", f2);

        FileFilter acceptAll = new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return true;
            }
        };

        FileFilter acceptOnlyDir1 = new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().equals("dir1");
            }
        };

        FileFilter acceptOnlyPy = new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".py");
            }
        };

        FileFilter acceptOnlyTxt = new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".txt");
            }
        };

        assertTrue(FileUtils.lastModified(f1) != FileUtils.lastModified(f2)); //if equal, this would invalidate the test!
        assertTrue(FileUtils.lastModified(f1a) != FileUtils.lastModified(f1)); //if equal, this would invalidate the test!
        assertTrue(FileUtils.lastModified(f1a) != FileUtils.lastModified(f2)); //if equal, this would invalidate the test!

        long lastModifiedTimeFromDir = FileUtils.getLastModifiedTimeFromDir(baseDir, acceptAll, acceptAll, 1000);
        assertEquals(lastModifiedTimeFromDir, FileUtils.lastModified(f2));

        lastModifiedTimeFromDir = FileUtils.getLastModifiedTimeFromDir(baseDir, acceptAll, acceptAll, 1);
        assertEquals(lastModifiedTimeFromDir, 0);

        lastModifiedTimeFromDir = FileUtils.getLastModifiedTimeFromDir(baseDir, acceptAll, acceptAll, 2);
        assertEquals(lastModifiedTimeFromDir, FileUtils.lastModified(f2));

        lastModifiedTimeFromDir = FileUtils.getLastModifiedTimeFromDir(baseDir, acceptAll, acceptOnlyDir1, 2);
        assertEquals(lastModifiedTimeFromDir, FileUtils.lastModified(f1));

        lastModifiedTimeFromDir = FileUtils.getLastModifiedTimeFromDir(baseDir, acceptOnlyPy, acceptAll, 2);
        assertEquals(lastModifiedTimeFromDir, FileUtils.lastModified(f1));

        lastModifiedTimeFromDir = FileUtils.getLastModifiedTimeFromDir(baseDir, acceptOnlyTxt, acceptOnlyDir1, 2);
        assertEquals(lastModifiedTimeFromDir, FileUtils.lastModified(f1a));
    }
}
