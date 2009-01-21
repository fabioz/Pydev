/*
 * Created on Mar 21, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import org.python.pydev.editor.PyCodeScanner.NumberDetector;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeScannerTest extends TestCase {

    private NumberDetector detector;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyCodeScannerTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        detector = new PyCodeScanner.NumberDetector();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNumber() {
        assertTrue(detector.isWordStart('1'));
        assertTrue(detector.isWordPart('.'));
        assertTrue(detector.isWordPart('2'));
        assertTrue(detector.isWordPart('e'));
        assertTrue(detector.isWordPart('5'));
        assertFalse(detector.isWordPart(' '));
    }
    
    public void testHexa() {
        assertTrue(detector.isWordStart('0'));
        assertTrue(detector.isWordPart('x'));
        assertTrue(detector.isWordPart('F'));
        assertTrue(detector.isWordPart('F'));
        assertFalse(detector.isWordPart(' '));
    }
}
