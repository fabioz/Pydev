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
    
    public void testHeadAndTail() {
        String[] strings = FullRepIterable.headAndTail("aa.bb.cc");
        assertEquals("aa.bb", strings[0]);
        assertEquals("cc", strings[1]);

        strings = FullRepIterable.headAndTail("aa.bb");
        assertEquals("aa", strings[0]);
        assertEquals("bb", strings[1]);
        
        strings = FullRepIterable.headAndTail("aa");
        assertEquals("", strings[0]);
        assertEquals("aa", strings[1]);
    }
    
    public void testGetWithoutLastPart() throws Exception {
		assertEquals("",FullRepIterable.getWithoutLastPart("test"));
		assertEquals("test",FullRepIterable.getWithoutLastPart("test.__init__"));
		assertEquals("test.test",FullRepIterable.getWithoutLastPart("test.test.__init__"));
	}
    
    public void testGetLastPart() throws Exception {
    	assertEquals("test",FullRepIterable.getLastPart("test"));
    	assertEquals("__init__",FullRepIterable.getLastPart("test.__init__"));
    	assertEquals("__init__",FullRepIterable.getLastPart("test.test.__init__"));
    }
    
    
    public void testDotSplit() throws Exception {
    	String[] strings = FullRepIterable.dotSplit("foo.bar.f");
    	assertEquals(3, strings.length);
    	assertEquals("foo", strings[0]);
    	assertEquals("bar", strings[1]);
    	assertEquals("f", strings[2]);
		
    	strings = FullRepIterable.dotSplit("foo.bar.");
    	assertEquals(2, strings.length);
    	assertEquals("foo", strings[0]);
    	assertEquals("bar", strings[1]);
    	
    	assertEquals(0, "...".split("\\.").length);
    	strings = FullRepIterable.dotSplit("...");
    	assertEquals(0, strings.length);
    	
    	strings = FullRepIterable.dotSplit("");
    	assertEquals(0, strings.length);
    	
    	strings = FullRepIterable.dotSplit("foo");
    	assertEquals(1, strings.length);
    	assertEquals("foo", strings[0]);
    	
    	strings = FullRepIterable.dotSplit("f.bu");
    	assertEquals(2, strings.length);
    	assertEquals("f", strings[0]);
    	assertEquals("bu", strings[1]);
    	
    	strings = FullRepIterable.dotSplit("..f.b...u..");
    	assertEquals(3, strings.length);
    	assertEquals("f", strings[0]);
    	assertEquals("b", strings[1]);
    	assertEquals("u", strings[2]);
    	
	}

}
