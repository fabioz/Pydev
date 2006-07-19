/*
 * Created on Jul 19, 2006
 * @author Fabio
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;

public class PyUncommentTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    
    public void testUncomment() throws Exception {
        Document doc = new Document(
                "#a\n" +
                "#b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        PyUncomment.perform(ps);
        
        String expected = "a\n" +
                          "b";
        assertEquals(expected, doc.get());
        
    }
}
