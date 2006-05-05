/*
 * Created on Mar 14, 2006
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

public class ParsingUtilsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ParsingUtilsTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsInCommOrStr() {
        String str = "" +
                "#comm1\n" +
                "'str'\n" +
                "pass\n" +
                "";
        assertEquals(ParsingUtils.PY_COMMENT, ParsingUtils.getContentType(str, 2));
        assertEquals(ParsingUtils.PY_SINGLELINE_STRING, ParsingUtils.getContentType(str, 10));
        assertEquals(ParsingUtils.PY_DEFAULT, ParsingUtils.getContentType(str, 17));
    }
    
    public void testIterator() throws Exception {
    	String str = "" +
    	"#c\n" +
    	"'s'\n" +
    	"pass\n" +
    	"";
    	Document d = new Document(str);
    	Iterator it = ParsingUtils.getNoLiteralsOrCommentsIterator(d);
    	assertEquals("\n",it.next());
    	assertEquals(true,it.hasNext());
    	assertEquals("\n",it.next());
    	assertEquals(true,it.hasNext());
    	assertEquals("pass\n",it.next());
    	assertEquals(false,it.hasNext());
	}
    
    public void testIterator2() throws Exception {
    	String str = "" +
    	"#c\n" +
    	"'s'" +
    	"";
    	Document d = new Document(str);
    	Iterator it = ParsingUtils.getNoLiteralsOrCommentsIterator(d);
    	assertEquals("\n",it.next());
    	assertEquals(true,it.hasNext());
    	assertEquals("",it.next());
    	assertEquals(false,it.hasNext());
    }
    
    public void testIterator3() throws Exception {
    	String str = "" +
    	"#c";
    	Document d = new Document(str);
    	Iterator it = ParsingUtils.getNoLiteralsOrCommentsIterator(d);
    	assertEquals(true,it.hasNext());
    	assertEquals("",it.next());
    	assertEquals(false,it.hasNext());
    }
    
    public void testIterator4() throws Exception {
    	String str = "" +
    	"pass\r" +
    	"foo\n" +
    	"bla\r\n" +
    	"what";
    	Document d = new Document(str);
    	Iterator it = ParsingUtils.getNoLiteralsOrCommentsIterator(d);
    	assertEquals(true,it.hasNext());
    	assertEquals("pass\r",it.next());
    	assertEquals(true,it.hasNext());
    	assertEquals("foo\n",it.next());
    	assertEquals(true,it.hasNext());
    	assertEquals("bla\r\n",it.next());
    	assertEquals(true,it.hasNext());
    	assertEquals("what",it.next());
    	assertEquals(false,it.hasNext());
    }
    
    
    
    
}
