package org.python.copiedfromeclipsesrc;

import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

public class PythonPairMatcherTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonPairMatcherTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testMatch() throws Exception {
        PythonPairMatcher matcher = getMatcher();
        String s = "test (";
        assertEquals(5, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
        s = "test ";
        assertEquals(-1, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
        s = "test () ";
        assertEquals(-1, matcher.searchForOpeningPeer(s.length(), '(', ')', new Document(s)));
    }
    
    public void testMatch1() throws Exception {
        PythonPairMatcher matcher = getMatcher();
        String s = "\ntest ('[#') ";
        assertEquals(-1, matcher.searchForAnyOpeningPeer(s.length(), new Document(s)));
        
    }
    
    public void testMatch2() throws Exception {
        PythonPairMatcher matcher = getMatcher();
        String s = "\ntest ('''\n[#''') ";
        assertEquals(-1, matcher.searchForAnyOpeningPeer(s.length(), new Document(s)));
        
        s = "\ntest (    ";
        assertEquals(6, matcher.searchForAnyOpeningPeer(s.length(), new Document(s)));
        
    }

    private PythonPairMatcher getMatcher() {
        return new PythonPairMatcher(new char[]{'(', ')', '[', ']'});
    }

}
