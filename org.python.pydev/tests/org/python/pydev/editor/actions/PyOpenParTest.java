/*
 * Created on Apr 7, 2005
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
public class PyOpenParTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyOpenParTest.class);
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
    public void testOpenPar() throws BadLocationException {
        String doc = ""+
        "    class Test";
        
        String result = ""+
        "    class Test():";
        Document d = new Document(doc);
        new PyOpenPar().performOpenPar(d, 0, 14);
        assertEquals(result, d.get());

        
        doc = ""+
        "method1(  ))";
        
        result = ""+
        "method1((  ))";

        d = new Document(doc);
        new PyOpenPar().performOpenPar(d, 0, 8);
        assertEquals(result, d.get());
    }
}
