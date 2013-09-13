/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.structure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.shared_core.structure.LowMemoryArrayList;

import junit.framework.TestCase;

/**
 * @author Fabio
 *
 */
public class LowMemoryArrayListTest extends TestCase {
    @SuppressWarnings("unchecked")
    public void testArrayList() throws Exception {
        List array = new LowMemoryArrayList();
        assertEquals(0, array.size());
        assertTrue(array.isEmpty());

        array.add(new Integer(0));
        array.add(0, new Integer(1));
        array.add(1, new Integer(2));
        array.add(new Integer(3));
        array.add(new Integer(1));

        assertEquals(5, array.size());
        assertFalse(array.isEmpty());

        assertEquals(1, ((Integer) array.get(0)).intValue());
        assertEquals(2, ((Integer) array.get(1)).intValue());
        assertEquals(0, ((Integer) array.get(2)).intValue());
        assertEquals(3, ((Integer) array.get(3)).intValue());
        assertEquals(1, ((Integer) array.get(4)).intValue());

        assertFalse(array.contains(null));
        assertTrue(array.contains(new Integer(2)));
        assertEquals(0, array.indexOf(new Integer(1)));
        assertEquals(4, array.lastIndexOf(new Integer(1)));
        assertTrue(array.indexOf(new Integer(5)) < 0);
        assertTrue(array.lastIndexOf(new Integer(5)) < 0);

        array.remove(1);
        array.remove(1);

        assertEquals(3, array.size());
        assertFalse(array.isEmpty());
        assertEquals(1, ((Integer) array.get(0)).intValue());
        assertEquals(3, ((Integer) array.get(1)).intValue());
        assertEquals(1, ((Integer) array.get(2)).intValue());

        assertFalse(array.contains(null));
        assertFalse(array.contains(new Integer(2)));
        assertEquals(0, array.indexOf(new Integer(1)));
        assertEquals(2, array.lastIndexOf(new Integer(1)));
        assertTrue(array.indexOf(new Integer(5)) < 0);
        assertTrue(array.lastIndexOf(new Integer(5)) < 0);

        array.clear();

        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
        assertTrue(array.indexOf(new Integer(5)) < 0);
        assertTrue(array.lastIndexOf(new Integer(5)) < 0);

        List al = new LowMemoryArrayList();

        assertFalse(al.remove(null));
        assertFalse(al.remove("string"));

        al.add("string");
        al.add(null);

        assertTrue(al.remove(null));
        assertTrue(al.remove("string"));

        List<Integer> asList = Arrays.asList(1, 2);

        al = new LowMemoryArrayList();
        Iterator iterator = al.iterator();
        assertTrue(!iterator.hasNext());

        al.addAll(asList);
        al.addAll(asList);
        assertEquals(1, al.get(0));
        assertEquals(2, al.get(1));
        assertEquals(1, al.get(2));
        assertEquals(2, al.get(3));

        iterator = al.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.next());

        assertTrue(!iterator.hasNext());
    }
}
