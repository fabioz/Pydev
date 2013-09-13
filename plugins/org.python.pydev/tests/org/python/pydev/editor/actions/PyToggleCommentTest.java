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
import org.python.pydev.shared_core.structure.Tuple;

public class PyToggleCommentTest extends TestCase {

    public static void main(String[] args) {
        PyToggleCommentTest test = new PyToggleCommentTest();
        try {
            test.setUp();
            test.testUncommentToProperIndentation();
            test.tearDown();
            junit.textui.TestRunner.run(PyToggleCommentTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FormatStd std;

    @Override
    protected void setUp() throws Exception {
        std = new PyFormatStd.FormatStd();
    }

    public void testCommentWithDifferentCodingStd() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document(" a\r\n" +
                "b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 9), new PyToggleComment(std).perform(ps));

        String expected = "#  a\r\n" +
                "# b";
        assertEquals(expected, doc.get());

    }

    public void testUncommentToProperIndentation() throws Exception {
        //When uncommenting, we should move the code uncommented to a proper indentation.
        Document doc = new Document("# a\n" +
                "#b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 4), new PyToggleComment(std).perform(ps));

        String expected = " a\n" +
                "b";
        assertEquals(expected, doc.get());
    }
}
