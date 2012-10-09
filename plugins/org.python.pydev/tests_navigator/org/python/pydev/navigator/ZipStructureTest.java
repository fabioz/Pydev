/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import junit.framework.TestCase;

public class ZipStructureTest extends TestCase {

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
