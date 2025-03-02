package org.python.pydev.shared_core.utils;

import java.util.NoSuchElementException;

import junit.framework.TestCase;

public class ArrayUtilsTest extends TestCase {

    public void testConcatArrays() throws Exception {
        String[] arrays = ArrayUtils.concatArrays(new String[0], new String[0]);
        assertEquals(0, arrays.length);

        Object[] arrays2 = ArrayUtils.concatArrays(new String[0], new Object[0]);
        assertEquals(0, arrays2.length);

        int[] arr = new int[] { 1, 2, 3 };
        int[] copy = ArrayUtils.reversedCopy(arr);
        assertTrue(arr != copy);
        assertEquals(copy[0], 3);
        assertEquals(copy[1], 2);
        assertEquals(copy[2], 1);

        arr = new int[] { 1, 2 };
        copy = ArrayUtils.reversedCopy(arr);
        assertTrue(arr != copy);
        assertEquals(copy[0], 2);
        assertEquals(copy[1], 1);
    }

    public void testArraysIteratorEmpty() throws Exception {
        ArrayUtils.ArraysIterator<String> iterator = new ArrayUtils.ArraysIterator<>();
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected exception
        }
    }

    public void testArraysIterator() throws Exception {
        String[] array1 = new String[] { "a", "b" };
        String[] array2 = new String[] { "c", null };
        String[] array3 = new String[] { "e", "f" };

        ArrayUtils.ArraysIterator<String> iterator = new ArrayUtils.ArraysIterator<>();
        iterator.addArray(array1);
        iterator.addArray(null);
        iterator.addArray(array2);
        iterator.addArray(array3);

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());

        assertEquals("a", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("b", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("c", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(null, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("e", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("f", iterator.next());

        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }
}
