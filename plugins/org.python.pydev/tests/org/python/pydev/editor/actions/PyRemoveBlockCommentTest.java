/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;

public class PyRemoveBlockCommentTest extends TestCase {

    private Document doc;
    private String expected;
    private PySelection ps;

    public void testUncommentBlock() throws Exception {
        doc = new Document(
                "#a\n" +
                "#b");
        ps = new PySelection(doc, 0, 0, 0);
        new PyRemoveBlockComment().perform(ps);
        
        expected = "a\n" +
                   "b";
        assertEquals(expected, doc.get());
        
        doc = new Document(
                "#----\n" +
                "#a\n" +
                "#b\n" +
                "#----");
        ps = new PySelection(doc, 0, 0, 0);
        new PyRemoveBlockComment().perform(ps);
        
        expected = "a\n" +
                   "b\n";
        assertEquals(expected, doc.get());
        
    }
    
    public void test2() throws Exception {

        doc = new Document(
                "#---- aa\n" +
                "#---- b\n" +
                "#---- c");
        ps = new PySelection(doc, 0, 0, 0);
        new PyRemoveBlockComment().perform(ps);
        
        expected = "aa\n" +
                   "b\n" +
                   "c";
        assertEquals(expected, doc.get());
        
    }
    
    public void test3() throws Exception {
        
        doc = new Document(
                "    #---- aa\n" +
                "        #---- b\n" +
                "    #---- c");
        ps = new PySelection(doc, 0, 0, 0);
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        pyRemoveBlockComment.perform(ps);
        
        expected = "    aa\n" +
                   "        b\n" +
                   "    c";
        assertEquals(expected, doc.get());
        
    }
    
    public void test4() throws Exception {
        
        doc = new Document(
        "    #---- aa\n" +
        "        #---- b\n" +
        "    #---- c\n" +
        "\n" +
        "\n");
        ps = new PySelection(doc, 0, 0, 0);
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        pyRemoveBlockComment.perform(ps);
        
        expected = 
        "    aa\n" +
        "        b\n" +
        "    c\n" +
        "\n" +
        "\n";
        assertEquals(expected, doc.get());
        
    }
}
