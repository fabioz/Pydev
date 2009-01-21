/*
 * Created on 03/09/2005
 */
package org.python.pydev.core.docutils;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(StringUtilsTest.class);
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
}
