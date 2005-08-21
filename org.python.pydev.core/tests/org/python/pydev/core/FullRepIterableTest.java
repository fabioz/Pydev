/*
 * Created on 28/07/2005
 */
package org.python.pydev.core;

import java.util.Iterator;

import junit.framework.TestCase;

public class FullRepIterableTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FullRepIterableTest.class);
    }

    public void testiterator1() {
        Iterator iterator = new FullRepIterable("var1.var2.var3.var4").iterator();
        assertTrue(iterator.hasNext());
        assertEquals("var1", iterator.next());
        assertEquals("var1.var2", iterator.next());
        assertEquals("var1.var2.var3", iterator.next());
        assertEquals("var1.var2.var3.var4", iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    public void testiterator1Rev() {
        Iterator iterator = new FullRepIterable("var1.var2.var3.var4", true).iterator();
        assertTrue(iterator.hasNext());
        assertEquals("var1.var2.var3.var4", iterator.next());
        assertEquals("var1.var2.var3", iterator.next());
        assertEquals("var1.var2", iterator.next());
        assertEquals("var1", iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    public void testiterator2() {
        Iterator iterator = new FullRepIterable("var1").iterator();
        assertTrue(iterator.hasNext());
        assertEquals("var1", iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testiterator2Rev() {
        Iterator iterator = new FullRepIterable("var1", true).iterator();
        assertTrue(iterator.hasNext());
        assertEquals("var1", iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    
    public void testiterator3() {
        Iterator iterator = new FullRepIterable("testlib.unittest.relative").iterator();
        assertTrue(iterator.hasNext());
        assertEquals("testlib", iterator.next());
        assertEquals("testlib.unittest", iterator.next());
        assertEquals("testlib.unittest.relative", iterator.next());
        assertFalse(iterator.hasNext());
        
        int i = 0;
        for(String dummy : new FullRepIterable("testlib.unittest.relative")){
            i++;
        }
        assertEquals(3, i);
    }
    
    public void testiterator3Rev() {
        Iterator iterator = new FullRepIterable("testlib.unittest.relative", true).iterator();
        assertTrue(iterator.hasNext());
        assertEquals("testlib.unittest.relative", iterator.next());
        assertEquals("testlib.unittest", iterator.next());
        assertEquals("testlib", iterator.next());
        assertFalse(iterator.hasNext());
        
        int i = 0;
        for(String dummy : new FullRepIterable("testlib.unittest.relative")){
            i++;
        }
        assertEquals(3, i);
    }
}
