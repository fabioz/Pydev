/*
 * Created on 16/07/2005
 */
package org.python.pydev.parser.visitors;

import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.structure.FastStringBuffer;

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
    
    public void testRemoveCommentsAndWhitespaces() {
        String s = "a , b = 0,#ignore\n*args, **kwargs";
        FastStringBuffer buf = new FastStringBuffer(s, 0);
        ParsingUtils.removeCommentsAndWhitespaces(buf);
        assertEquals("a,b=0,*args,**kwargs", buf.toString());
    }
    
    public void testRemoveCommentsWhitespacesAndLiterals() {
        String s = 
            "a , b = 0,#ignore\n" +
            "*args, **kwargs\n" +
            "'''";
        FastStringBuffer buf = new FastStringBuffer(s, 0);
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf);
        assertEquals("a,b=0,*args,**kwargs", buf.toString());
        
        s = 
            "a , b = 0,#ignore\n" +
            "*args, **kwargs\n" +
            "'''remove'\"";
        buf = new FastStringBuffer(s, 0);
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf);
        assertEquals("a,b=0,*args,**kwargs", buf.toString());
        
        s = 
            "a , b = 0,#ignore\n" +
            "*args, **kwargs\n" +
            "'''remove'''keep";
        buf = new FastStringBuffer(s, 0);
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf);
        assertEquals("a,b=0,*args,**kwargskeep", buf.toString());
    }

}
