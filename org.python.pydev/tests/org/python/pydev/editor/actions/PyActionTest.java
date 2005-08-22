/*
 * Created on 22/08/2005
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

public class PyActionTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyActionTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCountLineBreaks() {
        assertEquals(0, PyAction.countLineBreaks("aaa"));
        assertEquals(1, PyAction.countLineBreaks("aaa\n"));
        assertEquals(2, PyAction.countLineBreaks("aaa\n\r"));
        assertEquals(1, PyAction.countLineBreaks("aaa\r\n"));
        assertEquals(3, PyAction.countLineBreaks("aaa\nooo\nooo\n"));
        assertEquals(2, PyAction.countLineBreaks("aaa\r\nbb\r\n"));
    }
}
