package org.python.pydev.core.docutils;

import junit.framework.TestCase;

public class DocUtilsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DocUtilsTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPeer() throws Exception {
        assertEquals('(', DocUtils.getPeer(')'));
        assertEquals(')', DocUtils.getPeer('('));
        
        assertEquals('{', DocUtils.getPeer('}'));
        assertEquals('}', DocUtils.getPeer('{'));
        
        assertEquals('[', DocUtils.getPeer(']'));
        assertEquals(']', DocUtils.getPeer('['));
    }
}
