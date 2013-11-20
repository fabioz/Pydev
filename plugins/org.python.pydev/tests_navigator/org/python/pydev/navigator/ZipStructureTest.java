/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.python.pydev.shared_core.io.FileUtils;

public class ZipStructureTest extends TestCase {

    private File baseDir;

    public void testZipStructureWithActualZip() throws Exception {
        try {
            setup();
            File file = new File(baseDir, "my.egg");
            FileOutputStream stream = new FileOutputStream(file);
            ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(stream));
            zipOut.putNextEntry(new ZipEntry("empty1/"));
            zipOut.putNextEntry(new ZipEntry("folder/zip_mod.py"));
            zipOut.write("class ZipMod:pass".getBytes());
            zipOut.close();

            ZipFile zipFile = new ZipFile(file);
            try {
                ZipStructure zipStructure = new ZipStructure(file, zipFile);
                List<String> contents = zipStructure.contents("");
                assertEquals(Arrays.asList("empty1/", "folder/"), contents);
                contents = zipStructure.contents("folder/");
                assertEquals(Arrays.asList("folder/zip_mod.py"), contents);
                contents = zipStructure.contents("empty1/");
                assertEquals(Arrays.asList(), contents);
            } finally {
                zipFile.close();
            }
        } finally {
            finish();
        }
    }

    private void finish() {
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (IOException e) {
            //ignore
        }
    }

    private void setup() {
        baseDir = new File(FileUtils.getFileAbsolutePath(new File("ZipStructureTest.temporary_dir")));
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
        assertTrue(baseDir.mkdir());

    }

    public void testZipStructure() throws Exception {
        ZipStructure zipStructure = new ZipStructure();
        Map<Integer, TreeSet<String>> levelToContents = zipStructure.getLevelToContents();
        TreeSet<String> tree = new TreeSet<String>();
        tree.add("file1.py");
        tree.add("file2.py");
        tree.add("dir/");
        tree.add("dir2/");
        tree.add("zz/");

        levelToContents.put(0, tree);

        tree = new TreeSet<String>();
        tree.add("dir/dir3/");
        tree.add("dir/file1.py");
        tree.add("dir/zzz.py");
        tree.add("dir2/file2.py");

        levelToContents.put(1, tree);

        tree = new TreeSet<String>();
        tree.add("dir/dir3/file3.py");

        levelToContents.put(2, tree);

        Iterator<String> iterator = zipStructure.contents("").iterator();
        assertEquals("dir/", iterator.next());
        assertEquals("dir2/", iterator.next());
        assertEquals("file1.py", iterator.next());
        assertEquals("file2.py", iterator.next());
        assertEquals("zz/", iterator.next());
        assertFalse(iterator.hasNext());

        iterator = zipStructure.contents("dir/").iterator();
        assertEquals("dir/dir3/", iterator.next());
        assertEquals("dir/file1.py", iterator.next());
        assertEquals("dir/zzz.py", iterator.next());
        assertFalse(iterator.hasNext());

    }
}
