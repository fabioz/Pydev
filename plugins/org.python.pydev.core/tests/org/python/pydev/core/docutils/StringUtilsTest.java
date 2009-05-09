/*
 * Created on 03/09/2005
 */
package org.python.pydev.core.docutils;

import java.util.Arrays;

import org.python.pydev.core.Tuple;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

    public static void main(String[] args) {
        try {
            StringUtilsTest test = new StringUtilsTest();
            test.setUp();
            test.testSplitOnString();
            test.tearDown();
            junit.textui.TestRunner.run(StringUtilsTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testFormat() {
        assertEquals("teste", StringUtils.format("%s", new Object[]{"teste"}));
        assertEquals("teste 1", StringUtils.format("%s 1", new Object[]{"teste"}));
        assertEquals("%", StringUtils.format("%", new Object[]{}));
    }
    
    public void testStripExt() {
        assertEquals("teste", StringUtils.stripExtension("teste.aaa"));
        assertEquals("teste.aaa", StringUtils.stripExtension("teste.aaa.bbb"));
        assertEquals("teste", StringUtils.stripExtension("teste"));
        assertEquals("a", StringUtils.stripExtension("a.a"));
        assertEquals("", StringUtils.stripExtension(""));
    }
    
    
    public void testReplaceAllSlashes() throws Exception {
        assertEquals("foo", StringUtils.replaceAllSlashes("foo"));
        assertEquals("foo/", StringUtils.replaceAllSlashes("foo\\"));
        assertEquals("/foo/", StringUtils.replaceAllSlashes("\\foo\\"));
        assertEquals("/foo///", StringUtils.replaceAllSlashes("\\foo\\\\\\"));
        
    }
    
    public void testReplaceAll() throws Exception {
        assertEquals("foo", StringUtils.replaceAll("fjj", "j", "o"));
        assertEquals("fok", StringUtils.replaceAll("fkkkk", "kkk", "o"));
        assertEquals("foo", StringUtils.replaceAll("fkkkk", "kk", "o"));
        assertEquals("kkkkkkkkk", StringUtils.replaceAll("www", "w", "kkk"));
        assertEquals("www", StringUtils.replaceAll("www", "o", "a"));
        
        String initial = ""+
        "import sys; sys.ps1=''; sys.ps2=''\r\n"+
        "print >> sys.stderr, 'PYTHONPATH:'\r\n"+
        "for p in sys.path:\r\n"+
        "    print >> sys.stderr,  p\r\n" +
        "\r\n" +                                                //to finish the for scope
        "print >> sys.stderr, 'Ok, all set up... Enjoy'\r\n"+
        "";
        assertEquals(initial, StringUtils.replaceAll(initial, "\r\n", "\r\n"));
        
        String expected = ""+
        "import sys; sys.ps1=''; sys.ps2=''\r"+
        "print >> sys.stderr, 'PYTHONPATH:'\r"+
        "for p in sys.path:\r"+
        "    print >> sys.stderr,  p\r" +
        "\r" +                                                //to finish the for scope
        "print >> sys.stderr, 'Ok, all set up... Enjoy'\r"+
        "";
        assertEquals(expected, StringUtils.replaceAll(initial, "\r\n", "\r"));
    }
    
    public void testRemoveWhitespaceColumnsToLeft() throws Exception {
        assertEquals("foo", StringUtils.removeWhitespaceColumnsToLeft("   foo"));
        assertEquals("foo\n", StringUtils.removeWhitespaceColumnsToLeft("   foo\n"));
        assertEquals("foo\n   foo\n", StringUtils.removeWhitespaceColumnsToLeft(" foo\n    foo\n"));
    }
    
    public void testSplitOn1st() throws Exception {
        assertEquals(new Tuple<String, String>("aa", "bb.cc"), StringUtils.splitOnFirst("aa.bb.cc", '.'));
        assertEquals(new Tuple<String, String>("aa_bb_cc", ""), StringUtils.splitOnFirst("aa_bb_cc", '.'));
        
    }
    
    public void testSplit() throws Exception{
        String[] split = StringUtils.split("aaa bb  ", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "bb"}, split));
        
        split = StringUtils.split("  aaa  bb   ", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "bb"}, split));
        
        split = StringUtils.split("aaa  bb", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "bb"}, split));
        
        split = StringUtils.split("aaa  bb  ", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "bb"}, split));
        
        split = StringUtils.split("aaa ", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa"}, split));
        
        split = StringUtils.split(" aaa", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa"}, split));
        
        split = StringUtils.split("aaa", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa"}, split));
        
        split = StringUtils.split("aaa bb\tccc\nbb ", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "bb", "ccc", "bb"}, split));
        
        split = StringUtils.split("aaa bb\t\t ccc\nbb ", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "bb", "ccc", "bb"}, split));
        
        split = StringUtils.split("aaa bb\t \n", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "bb"}, split));
        
        split = StringUtils.split("aaa \t\nbb\t \n", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "bb"}, split));
        
        split = StringUtils.split("aaa", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa"}, split));
        
        split = StringUtils.split("aaa\t\n ", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa"}, split));
        
        split = StringUtils.split("\t\n  aaa\t\n ", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa"}, split));
        
        split = StringUtils.split("\t\n  aaa", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa"}, split));
        
        split = StringUtils.split(" aaa   ", new char[]{' '}).toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa"}, split));
        
        split = StringUtils.splitAndRemoveEmptyTrimmed("|| |a||b||", '|').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"a", "b"}, split));
    }
    
    
    public void testSplitOnString() throws Exception {
        String[] split = StringUtils.split("aaa bb ccc bb kkk bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "ccc", "kkk"}, split));
        
        split = StringUtils.split("aaa bb ccc bb kkk bb", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "ccc", "kkk bb"}, split));
        
        split = StringUtils.split("aaa bb ccc bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "ccc"}, split));
        
        split = StringUtils.split(" bb aaa bb ccc bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"aaa", "ccc"}, split));
        
        split = StringUtils.split(" bb  bb ccc bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"ccc"}, split));
        
        split = StringUtils.split("ccc", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"ccc"}, split));
        
        split = StringUtils.split("", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{}, split));
        
        split = StringUtils.split("a", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"a"}, split));
        
        split = StringUtils.split(" bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{}, split));
        
        split = StringUtils.split(" bb b", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"b"}, split));
        
        split = StringUtils.split(" bb b bb", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"b bb"}, split));
        
        split = StringUtils.split(" bb b  bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{"b "}, split));
        
        split = StringUtils.split(" bb b  bb ", " bb2 ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{" bb b  bb "}, split));
        
        split = StringUtils.split(" bb b  bb ", "b").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{" ", " ", "  ", " "}, split));
        
        split = StringUtils.split(" bb bb  bb ", "bb").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[]{" ", " ", "  ", " "}, split));
    }
    
    public void testReplaceChars() throws Exception {
        assertEquals("aaaXeeeXcccX", StringUtils.replaceNewLines("aaa\neee\r\nccc\r", "X"));
        assertEquals("aaabbbccc", StringUtils.removeNewLineChars("aaa\r\nbbb\rccc\n"));
    }
}

