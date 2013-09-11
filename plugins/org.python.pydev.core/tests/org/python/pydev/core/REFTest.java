/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.CharArrayReader;
import java.io.File;
import java.util.HashSet;

import junit.framework.TestCase;

import org.python.pydev.shared_core.io.FileUtils;

public class REFTest extends TestCase {

    public void testLog() {
        //These are the values we get from python.
        double[] expected = new double[] { 0.0, 1.70951129135, 2.70951129135, 3.4190225827, 3.96936229592,
                4.4190225827, 4.79920493809, 5.12853387405, 5.4190225827, 5.67887358727, 5.91393741372, 6.12853387405,
                6.32594348113, 6.50871622944, 6.67887358727, 6.83804516541, 6.9875638801, 7.12853387405, 7.26188004907, };

        for (int i = 1; i < 20; i++) {
            //            System.out.println(i+": "+(i+Math.round(REF.log(i, 1.4))));
            assertTrue("" + expected[i - 1] +
                    " !=" + MathUtils.log(i, 1.5) +
                    "for log " + i,
                    Math.abs(expected[i - 1] - MathUtils.log(i, 1.5)) < 0.01);
        }

    }

    public void testGetTempFile() throws Exception {
        FileUtils.clearTempFilesAt(new File("."), "ref_test_case");
        File parentDir = new File(".");
        try {
            assertEquals("ref_test_case0", writeAt(parentDir).getName());
            assertEquals("ref_test_case1", writeAt(parentDir).getName());
            assertEquals("ref_test_case2", writeAt(parentDir).getName());
        } finally {
            try {
                HashSet<String> expected = new HashSet<String>();
                expected.add("ref_test_case0");
                expected.add("ref_test_case1");
                expected.add("ref_test_case2");
                assertEquals(expected, FileUtils.getFilesStartingWith(parentDir, "ref_test_case"));

                assertEquals("ref_test_case3", FileUtils.getTempFileAt(parentDir, "ref_test_case").getName());
                assertEquals("ref_test_case4", FileUtils.getTempFileAt(parentDir, "ref_test_case").getName());
                FileUtils.clearTempFilesAt(parentDir, "ref_test_case");
                assertEquals("ref_test_case0", FileUtils.getTempFileAt(parentDir, "ref_test_case").getName());

            } finally {
                FileUtils.clearTempFilesAt(parentDir, "ref_test_case");
            }
        }

    }

    public File writeAt(File parentDir) {
        File tempFileAt = FileUtils.getTempFileAt(parentDir, "ref_test_case");
        FileUtils.writeStrToFile("foo", tempFileAt);
        return tempFileAt;
    }

    public void testDeleteDirectoryTree() throws Exception {
        File currentDir = new File(".");
        File start_dir = new File(currentDir, "test_start_dir");
        assertTrue(!start_dir.exists());
        try {
            File dir2 = new File(currentDir, "test_start_dir/dir1/dir2");
            File file1 = new File(currentDir, "test_start_dir/dir1/dir2/file1.txt");
            dir2.mkdirs();
            FileUtils.writeStrToFile("something", file1);

            assertTrue(dir2.exists());
        } finally {
            FileUtils.deleteDirectoryTree(start_dir);
        }
        assertTrue(!start_dir.exists());
    }

    public void testHasPythonShebang() {
        String s = "" +
                "#!bla\n" +
                "\n" +
                "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        assertFalse(FileUtils.hasPythonShebang(reader));
    }

    public void testHasPythonShebang1() {
        String s = "" +
                "#!python\n" +
                "\n" +
                "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        assertTrue(FileUtils.hasPythonShebang(reader));
    }

    public void testHasPythonShebang2() {
        String s = "" +
                "#!python2\n" +
                "\n" +
                "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        assertTrue(FileUtils.hasPythonShebang(reader));
    }

    public void testHasPythonShebang3() {
        String s = "" +
                "#!python3\n" +
                "\n" +
                "";
        CharArrayReader reader = new CharArrayReader(s.toCharArray());
        assertTrue(FileUtils.hasPythonShebang(reader));
    }

    public void testHasPythonShebang4() {
        CharArrayReader reader = new CharArrayReader(new char[0]);
        assertFalse(FileUtils.hasPythonShebang(reader));
    }

}
