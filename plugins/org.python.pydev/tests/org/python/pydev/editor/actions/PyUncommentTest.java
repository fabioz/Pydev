/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 19, 2006
 * @author Fabio
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.autoedit.TestIndentPrefs;
import org.python.pydev.shared_core.structure.Tuple;

public class PyUncommentTest extends TestCase {

    public static void main(String[] args) {
        PyUncommentTest test = new PyUncommentTest();
        try {
            test.setUp();
            test.testUncommentToProperIndentation3();
            test.tearDown();
            junit.textui.TestRunner.run(PyUncommentTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FormatStd std;

    @Override
    protected void setUp() throws Exception {
        std = new PyFormatStd.FormatStd();
    }

    public void testUncomment() throws Exception {
        std.spacesInStartComment = 1;
        Document doc = new Document("#a\n" +
                "#b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 3), new PyUncomment(std).perform(ps));

        String expected = "a\n" +
                "b";
        assertEquals(expected, doc.get());
    }

    public void testUncommentToProperIndentation() throws Exception {
        std.spacesInStartComment = 1;
        //When uncommenting, we should move the code uncommented to a proper indentation.
        Document doc = new Document("# a\n" +
                "#b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 4), new PyUncomment(std).perform(ps));

        String expected = " a\n" +
                "b";
        assertEquals(expected, doc.get());
    }

    public void testUncommentToProperIndentation2() throws Exception {
        std.spacesInStartComment = 1;
        //When uncommenting, we should move the code uncommented to a proper indentation.
        Document doc = new Document("# a\n" +
                "# b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 3), new PyUncomment(std).perform(ps));

        String expected = "a\n" +
                "b";
        assertEquals(expected, doc.get());
    }

    public void testUncommentToProperIndentation3() throws Exception {
        std.spacesInStartComment = 1;
        //When uncommenting, we should move the code uncommented to a proper indentation.
        Document doc = new Document("# a\n" +
                "# b\n#\n#\n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 5), new PyUncomment(std).perform(ps));

        String expected = "a\n" +
                "b\n\n\n";
        assertEquals(expected, doc.get());
    }

    public void testUncommentWithTabs() throws Exception {
        std.spacesInStartComment = 1;
        DefaultIndentPrefs.set(new TestIndentPrefs(false, 4));
        try {

            //When uncommenting, we should move the code uncommented to a proper indentation.
            Document doc = new Document("# \ta");
            PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
            assertEquals(new Tuple<Integer, Integer>(0, 2), new PyUncomment(std).perform(ps));

            String expected = "\ta";
            assertEquals(expected, doc.get());
        } finally {
            DefaultIndentPrefs.set(null);
        }
    }
}
