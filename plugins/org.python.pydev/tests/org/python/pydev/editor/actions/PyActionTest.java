/*
 * Created on 22/08/2005
 */
package org.python.pydev.editor.actions;

import org.python.pydev.core.docutils.PySelection;

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
        assertEquals(0, PySelection.countLineBreaks("aaa"));
        assertEquals(1, PySelection.countLineBreaks("aaa\n"));
        assertEquals(2, PySelection.countLineBreaks("aaa\n\r"));
        assertEquals(1, PySelection.countLineBreaks("aaa\r\n"));
        assertEquals(3, PySelection.countLineBreaks("aaa\nooo\nooo\n"));
        assertEquals(2, PySelection.countLineBreaks("aaa\r\nbb\r\n"));
    }
}
