package org.python.pydev.shared_core.utils;

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
}
