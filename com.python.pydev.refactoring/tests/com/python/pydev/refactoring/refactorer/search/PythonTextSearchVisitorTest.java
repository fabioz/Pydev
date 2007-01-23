package com.python.pydev.refactoring.refactorer.search;

import junit.framework.TestCase;

public class PythonTextSearchVisitorTest extends TestCase {

    public void testSearch() throws Exception {
        
        PythonTextSearchVisitor s = new PythonTextSearchVisitor("foo");
        assertTrue(s.hasMatch(null, "foo"));
        assertTrue(s.hasMatch(null, " foo"));
        assertTrue(s.hasMatch(null, "foo "));
        assertTrue(s.hasMatch(null, " foo "));
        assertTrue(s.hasMatch(null, "a foo)o")); 
        assertTrue(s.hasMatch(null, "a foo.o")); 
        
        //we only match on 'exact' matches
        assertTrue(!s.hasMatch(null, "bar"));
        assertTrue(!s.hasMatch(null, "fooo")); 
        assertTrue(!s.hasMatch(null, "afoo")); 
        assertTrue(!s.hasMatch(null, "fooa")); 
        assertTrue(!s.hasMatch(null, "foao")); 
        assertTrue(!s.hasMatch(null, "fo")); 
    }
}
