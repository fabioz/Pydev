/*
 * Created on Oct 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

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
    protected void setUp() throws Exception {
        super.setUp();
        cache = new CoverageCache();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAddRoot() throws NodeNotFoudException{
        String folder1 = "a";     //all files
        String folder2 = "a.b";   //no files
        String folder3 = "a.c";   //file3 and file4 + file5
        String folder4 = "a.c.d"; //only file5

        String file1 = "b";
        String file2 = "c";
        String file3 = "d";
        String file4 = "e";
        String file5 = "fgggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg";

        cache.addFolder(folder1);
        cache.addFolder(folder2, folder1);
        cache.addFolder(folder3, folder1);
        cache.addFolder(folder4, folder3);

        cache.addFile(file1, folder1, 20,10, "6-10");
        cache.addFile(file2, folder1, 22,10, "6-10");
        cache.addFile(file3, folder3, 24,10, "6-10");
        cache.addFile(file4, folder3, 26,10, "6-10");
        cache.addFile(file5, folder4, 28,10, "6-10");
        
        List folder1files = cache.getFiles(folder1);
        assertEquals(5, folder1files.size());

        List folder2files = cache.getFiles(folder2);
        assertEquals(0, folder2files.size());

        List folder3files = cache.getFiles(folder3);
        assertEquals(3, folder3files.size());

        
        List folder4files = cache.getFiles(folder4);
        assertEquals(1, folder4files.size());
        assertEquals(folder4files, cache.getFiles(file5));


        System.out.println(cache.getStatistics(folder1));
        
    }
}
