/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PySelectionTest extends TestCase {

    private PySelection ps;
    private Document doc;
    private String docContents;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PySelectionTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        docContents = "" +
		"TestLine1\n"+
		"TestLine2#comm2\n"+
		"TestLine3#comm3\n"+
		"TestLine4#comm4\n";
        doc = new Document(docContents);
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
    public void testGeneral() throws BadLocationException {
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        assertEquals("",ps.getSelection());
        assertEquals("",ps.getLineContentsToCursor());
        ps.selectCompleteLine();
        
        assertEquals("TestLine1",ps.getSelection());
        assertEquals("TestLine1",ps.getLine(0));
        assertEquals("TestLine2#comm2",ps.getLine(1));
        
        ps.deleteLine(0);
        assertEquals("TestLine2#comm2",ps.getLine(0));
        ps.addLine("TestLine1", 0);
        
    }
    
    /**
     * 
     */
    public void testSelectAll() {
        ps = new PySelection(doc, new TextSelection(doc, 0,0));
        ps.selectAll(false);
        assertEquals(docContents, ps.getSelection());
        
        ps = new PySelection(doc, new TextSelection(doc, 0,9)); //first line selected
        ps.selectAll(false); //changes
        assertEquals(docContents, ps.getSelection());
        
        ps = new PySelection(doc, new TextSelection(doc, 0,9)); //first line selected
        ps.selectAll(true); //nothing changes
        assertEquals(ps.getLine(0), ps.getSelection());
        
        
    }
}
