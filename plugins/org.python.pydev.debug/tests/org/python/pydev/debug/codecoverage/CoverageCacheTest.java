/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class CoverageCacheTest extends TestCase {

    private CoverageCache cache;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CoverageCacheTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cache = new CoverageCache();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAddRoot() throws NodeNotFoudException {
        File folder1 = new File("a"); //all files
        File folder2 = new File("a.b"); //no files
        File folder3 = new File("a.c"); //file3 and file4 + file5
        File folder4 = new File("a.c.d"); //only file5

        File file1 = new File("b");
        File file2 = new File("c");
        File file3 = new File("d");
        File file4 = new File("e");
        File file5 = new File("fgggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg");

        cache.addFolder(folder1);
        cache.addFolder(folder2, folder1);
        cache.addFolder(folder3, folder1);
        cache.addFolder(folder4, folder3);

        cache.addFile(file1, folder1, 20, 10, "6-10");
        cache.addFile(file2, folder1, 22, 10, "6-10");
        cache.addFile(file3, folder3, 24, 10, "6-10");
        cache.addFile(file4, folder3, 26, 10, "6-10");
        cache.addFile(file5, folder4, 28, 10, "6-10");

        List<ICoverageNode> folder1files = cache.getFiles(folder1);
        assertEquals(5, folder1files.size());

        List<ICoverageNode> folder2files = cache.getFiles(folder2);
        assertEquals(0, folder2files.size());

        List<ICoverageNode> folder3files = cache.getFiles(folder3);
        assertEquals(3, folder3files.size());

        List<ICoverageNode> folder4files = cache.getFiles(folder4);
        assertEquals(1, folder4files.size());
        assertEquals(folder4files, cache.getFiles(file5));

        String statistics = cache.getStatistics(null, folder1).o1;
        String expected = "" + "Name                                      Stmts     Miss      Cover  Missing\n"
                + "-----------------------------------------------------------------------------\n"
                + "b                                            20       10        50%  6-10\n"
                + "c                                            22       10      54,5%  6-10\n"
                + "d                                            24       10      58,3%  6-10\n"
                + "e                                            26       10      61,5%  6-10\n"
                + "..gggggggggggggggggggggggggggggggggggggg     28       10      64,3%  6-10\n"
                + "-----------------------------------------------------------------------------\n"
                + "TOTAL                                       120       50      58,3%  \n" + "";

        if (!expected.equals(statistics) && !expected.replace(',', '.').equals(statistics)) {
            assertEquals(expected, statistics);
        }

    }
}
