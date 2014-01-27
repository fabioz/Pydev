/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.structure;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.python.pydev.shared_core.string.FastStringBuffer;

public class FastStringBufferTest extends TestCase {

    private static final int ITERATIONS = 10000;
    private static final int OUTER_ITERATIONS = 50;

    public void testFastStringBufferConstructor() throws Exception {
        char[] buf = new char[] { 'a', 'b' };
        FastStringBuffer fastString = new FastStringBuffer(buf);
        assertSame(buf, fastString.getInternalCharsArray());
        assertEquals(buf.length, fastString.length());
        assertEquals("ab", fastString.toString());
    }

    public void testAppendNoResize() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(5);
        buf.appendNoResize("aa");
        assertEquals("aa", buf.toString());
        buf.appendNoResize("bb");
        assertEquals("aabb", buf.toString());
        try {
            buf.appendNoResize("cc");
            fail("Expected resize to fail");
        } catch (Exception e) {
            //Expected
        }
        //Should not have changed
        assertEquals("aabb", buf.toString());
    }

    public void testFastString1() throws Exception {

        FastStringBuffer fastString = new FastStringBuffer(2);
        fastString.append("bbb");
        assertEquals("bbb", fastString.toString());
        fastString.append("ccc");
        assertEquals("bbbccc", fastString.toString());
        fastString.clear();
        assertEquals("", fastString.toString());
        fastString.append("abc");
        assertEquals("abc", fastString.toString());
        fastString.reverse();
        assertEquals("cba", fastString.toString());

        fastString.clear();
        fastString.append("aaa");
        FastStringBuffer other = new FastStringBuffer(3);
        other.append("bbcccdddddddddddddddddddddddddddddd");
        fastString.append(other);
        assertEquals("aaabbcccdddddddddddddddddddddddddddddd", fastString.toString());
        fastString.insert(1, "22");
        assertEquals("a22aabbcccdddddddddddddddddddddddddddddd", fastString.toString());
        fastString.append('$');
        assertEquals("a22aabbcccdddddddddddddddddddddddddddddd$", fastString.toString());
        fastString.insert(1, ".");
        assertEquals("a.22aabbcccdddddddddddddddddddddddddddddd$", fastString.toString());
        fastString.replace(0, 1, "xxx");
        assertEquals("xxx.22aabbcccdddddddddddddddddddddddddddddd$", fastString.toString());
        fastString.delete(0, 1);
        assertEquals("xx.22aabbcccdddddddddddddddddddddddddddddd$", fastString.toString());

        char[] charArray = fastString.toString().toCharArray();
        char[] charArray2 = fastString.toCharArray();
        assertEquals(charArray.length, charArray2.length);
        for (int i = 0; i < charArray2.length; i++) {
            assertEquals(charArray[i], charArray2[i]);

        }
    }

    public void testReverseIterating() {
        FastStringBuffer fastStringBuffer = new FastStringBuffer("abc", 0);
        FastStringBuffer fastStringBuffer2 = new FastStringBuffer("", 3);
        for (Character c : fastStringBuffer.reverseIterator()) {
            fastStringBuffer2.append(c);
        }
        assertEquals("cba", fastStringBuffer2.toString());
    }

    public void testEndsWith() throws Exception {
        FastStringBuffer fastStringBuffer = new FastStringBuffer("abc", 0);
        assertTrue(fastStringBuffer.endsWith("c"));
        assertTrue(fastStringBuffer.endsWith("bc"));
        assertTrue(fastStringBuffer.endsWith("abc"));
        assertTrue(!fastStringBuffer.endsWith("aabc"));

        assertTrue(fastStringBuffer.startsWith("abc"));
        assertTrue(fastStringBuffer.startsWith("a"));
        assertTrue(fastStringBuffer.startsWith("ab"));
        assertTrue(!fastStringBuffer.startsWith("abcd"));

        fastStringBuffer.setCharAt(0, 'h');
        assertTrue(fastStringBuffer.startsWith("hb"));
    }

    public void testReplace() throws Exception {
        assertEquals("def", new FastStringBuffer("abcdefabc", 0).replaceAll("abc", "").toString());
        assertEquals("xyzdef", new FastStringBuffer("abcdef", 0).replaceAll("abc", "xyz").toString());
        assertEquals("xyzdefxyz", new FastStringBuffer("abcdefabc", 0).replaceAll("abc", "xyz").toString());
        assertEquals("aaa", new FastStringBuffer("abcabcabc", 0).replaceAll("abc", "a").toString());
        assertEquals("xyzxyzxyz", new FastStringBuffer("aaa", 0).replaceAll("a", "xyz").toString());
        assertEquals("", new FastStringBuffer("aaa", 0).replaceAll("a", "").toString());
        assertEquals("ba", new FastStringBuffer("aaa", 0).replaceAll("aa", "b").toString());
    }

    public void testAppendN() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(0);
        assertEquals("   ", buf.appendN(' ', 3).toString());
        assertEquals("   ", buf.clear().appendN(" ", 3).toString());
        assertEquals("   aaa", buf.appendN('a', 3).toString());
        assertEquals("   aaabbbbbb", buf.appendN("bb", 3).toString());
    }

    public void testStartsWith() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(0);
        assertFalse(buf.startsWith('"'));
        buf.append("a");
        assertFalse(buf.startsWith('"'));
        assertTrue(buf.startsWith('a'));
        buf.deleteFirst();
        assertFalse(buf.startsWith('"'));
        assertFalse(buf.startsWith('a'));
    }

    public void testEndsWithChar() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(0);
        assertFalse(buf.endsWith('"'));
        buf.append("a");
        assertFalse(buf.endsWith('"'));
        assertTrue(buf.endsWith('a'));
        buf.deleteFirst();
        assertFalse(buf.endsWith('"'));
        assertFalse(buf.endsWith('a'));
        buf.append("ab");
        assertTrue(buf.endsWith('b'));
        assertFalse(buf.endsWith('a'));
    }

    public void testCountNewLines() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(0);
        assertEquals(0, buf.countNewLines());
        buf.append('\r');
        assertEquals(1, buf.countNewLines());
        buf.append('\n');
        assertEquals(1, buf.countNewLines());
        buf.append('\n');
        assertEquals(2, buf.countNewLines());
    }

    public void testDeleteLastChars() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(0);
        buf.append("rara");
        buf.append("foo");
        buf.deleteLastChars("foo".length());
        assertEquals("rara", buf.toString());
        buf.deleteLastChars(50);
        assertEquals("", buf.toString());
    }

    public void testInsertN() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(0);
        buf.append("rara");
        buf.insertN(0, ' ', 2);
        assertEquals("  rara", buf.toString());
        buf.insertN(1, 'a', 1);
        assertEquals(" a rara", buf.toString());
        buf.insertN(7, 'b', 3);
        assertEquals(" a rarabbb", buf.toString());
    }

    public void testGetLastWord() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(0);
        assertEquals("", buf.getLastWord());
        buf.append("b");
        assertEquals("b", buf.getLastWord());
        buf.append("ar");
        assertEquals("bar", buf.getLastWord());
        buf.append("1");
        assertEquals("bar1", buf.getLastWord());
        buf.append("\tsome");
        assertEquals("some", buf.getLastWord());
        buf.append(" some1");
        assertEquals("some1", buf.getLastWord());
        buf.append("  \t");
        assertEquals("some1", buf.getLastWord());
    }

    public void testRemoveWhitespaces() throws Exception {
        FastStringBuffer buf = new FastStringBuffer(0);
        buf.removeWhitespaces();
        assertEquals(0, buf.length());

        buf.append("aa  bb b  \nbb");
        buf.removeWhitespaces();
        assertEquals("aabbbbb", buf.toString());
        buf.clear();

        buf.append("  a\ra  bb b  \nbb  ");
        buf.removeWhitespaces();
        assertEquals("aabbbbb", buf.toString());
    }

    public void testRightTrim() throws Exception {
        FastStringBuffer buf = new FastStringBuffer("", 0);
        buf.rightTrim();
        assertEquals("", buf.toString());
        buf = new FastStringBuffer("   ", 0);
        buf.rightTrim();
        assertEquals("", buf.toString());
        buf = new FastStringBuffer("foo", 0);
        buf.rightTrim();
        assertEquals("foo", buf.toString());
        buf = new FastStringBuffer("foo   ", 0);
        buf.rightTrim();
        assertEquals("foo", buf.toString());
        buf = new FastStringBuffer("foo bar", 0);
        buf.rightTrim();
        assertEquals("foo bar", buf.toString());
        buf = new FastStringBuffer("foo bar   ", 0);
        buf.rightTrim();
        assertEquals("foo bar", buf.toString());
    }

    public void testDeleteFirstChars() throws Exception {
        FastStringBuffer buf = new FastStringBuffer("aaabbb", 0);
        buf.deleteFirstChars(3);
        assertEquals("bbb", buf.toString());
    }

    public void testRemoveChars() throws Exception {
        FastStringBuffer buf = new FastStringBuffer("abcdef", 0);
        Set<Character> set = new HashSet<Character>();
        set.add('a');
        set.add('c');
        assertEquals("bdef", buf.removeChars(set).toString());
    }

    public void testSubSequence() throws Exception {
        FastStringBuffer buf = new FastStringBuffer("abcdef", 0);
        CharSequence subSequence = buf.subSequence(2, 5);
        assertEquals("cde", subSequence.toString());
        assertEquals(3, subSequence.length());
        assertEquals('c', subSequence.charAt(0));
        assertEquals('d', subSequence.charAt(1));
        assertEquals('e', subSequence.charAt(2));
        try {
            subSequence.charAt(3);
            fail("Expected exception");
        } catch (Exception e) {
        }
        CharSequence seq2 = subSequence.subSequence(1, 2);
        assertEquals("d", seq2.toString());
        assertEquals('d', seq2.charAt(0));
        assertEquals(1, seq2.length());
    }

    public void testIndexOf() throws Exception {
        FastStringBuffer buf = new FastStringBuffer("aacc", 0);
        assertEquals(0, buf.indexOf('a'));
        assertEquals(0, buf.indexOf("aa"));
        assertEquals(0, buf.indexOf("a"));
        assertEquals(-1, buf.indexOf("b"));
        assertEquals(2, buf.indexOf('c'));
        assertEquals(2, buf.indexOf("c"));
        assertEquals(2, buf.indexOf("cc"));
        assertEquals(-1, buf.indexOf("ccc"));
        assertEquals(-1, buf.indexOf("aaa"));
        assertEquals(1, buf.indexOf("ac"));
    }

    public void testIndexOfFrom() throws Exception {
        FastStringBuffer buf = new FastStringBuffer("aacc", 0);
        assertEquals(0, buf.indexOf("aa", 0));
        assertEquals(-1, buf.indexOf("aa", 1));
        assertEquals(0, buf.indexOf("a", 0));
        assertEquals(-1, buf.indexOf("b", 0));
        assertEquals(2, buf.indexOf("c", 0));
        assertEquals(2, buf.indexOf("cc", 0));
        assertEquals(2, buf.indexOf("cc", 1));
        assertEquals(2, buf.indexOf("cc", 2));
        assertEquals(-1, buf.indexOf("cc", 3));
        assertEquals(-1, buf.indexOf("cc", 10));
        assertEquals(-1, buf.indexOf("ccc", 0));
        assertEquals(-1, buf.indexOf("aaa", 0));
        assertEquals(1, buf.indexOf("ac", 0));
    }

    //    public void testFastString() throws Exception {
    //        
    //        long total=0;
    //        FastStringBuffer fastString = new FastStringBuffer(50);
    //        for(int j=0;j<OUTER_ITERATIONS;j++){
    //            final long start = System.nanoTime();
    //            
    //            
    //            fastString.clear();
    //            for(int i=0;i<ITERATIONS;i++){
    //                fastString.append("test").append("bar").append("foo").append("foo").append("foo").append("foo");
    //            }
    //            
    //            final long end = System.nanoTime();
    //            long delta=(end-start)/1000000;
    //            total+=delta;
    ////            System.out.println("Fast: " + delta);
    //        }        
    //        System.out.println("Fast Total:"+total);
    //    }
    //    
    //    public void testStringBuffer() throws Exception {
    //        
    //        long total=0;
    //        StringBuffer fastString = new StringBuffer(50);
    //        for(int j=0;j<OUTER_ITERATIONS;j++){
    //            final long start = System.nanoTime();
    //            
    //            
    //            fastString.setLength(0);
    //            for(int i=0;i<ITERATIONS;i++){
    //                fastString.append("test").append("bar").append("foo").append("foo").append("foo").append("foo");
    //            }
    //            
    //            final long end = System.nanoTime();
    //            long delta=(end-start)/1000000;
    //            total+=delta;
    ////            System.out.println("Buffer: " + delta);
    //        }   
    //        System.out.println("Buffer Total:"+total);
    //    }

}
