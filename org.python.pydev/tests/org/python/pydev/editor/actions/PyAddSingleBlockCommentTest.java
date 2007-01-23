/*
 * Created on Jul 19, 2006
 * @author Fabio
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;

import junit.framework.TestCase;

public class PyAddSingleBlockCommentTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testBlock() throws Exception {
        String s = "cc";
        
        Document doc = new Document(s);
        new PyAddSingleBlockComment(10, true).perform(new PySelection(doc, 0,0,0));
        assertEquals("#------ cc", doc.get());
        
        s = "    cc";
        
        doc = new Document(s);
        new PyAddSingleBlockComment(10, true).perform(new PySelection(doc, 0,0,0));
        assertEquals("    #-- cc", doc.get());
        
        doc = new Document("cc");
        new PyAddSingleBlockComment(10, false).perform(new PySelection(doc, 0,0,0));
        assertEquals("#cc ------", doc.get());
        
    }

}
