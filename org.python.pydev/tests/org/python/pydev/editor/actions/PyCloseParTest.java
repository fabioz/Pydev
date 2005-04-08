/*
 * Created on Apr 8, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PyCloseParTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyCloseParTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @throws BadLocationException
     * 
     */
    public void testClosePar() throws BadLocationException {
        String doc = ""+
        "Test(";
        
        String result = ""+
        "Test()";
        Document d = new Document(doc);
        new PyClosePar().performClosePar(d, 0, 5);
        assertEquals(result, d.get());

        
        doc = ""+
        "test()";
        
        result = ""+
        "test()";

        d = new Document(doc);
        new PyClosePar().performClosePar(d, 0, 5);
        assertEquals(result, d.get());
    }
}
