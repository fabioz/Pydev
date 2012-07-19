/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;

public class PyCommentTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testComment() throws Exception {
        Document doc = new Document("a\n" +
                "\n" +
                "\n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 5), new PyComment().perform(ps));

        String expected = "#a\n" +
                "#\n" +
                "\n";
        assertEquals(expected, doc.get());

    }

    public void testComment2() throws Exception {
        Document doc = new Document("a\r" +
                "\r" +
                "\r");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 5), new PyComment().perform(ps));

        String expected = "#a\r" +
                "#\r" +
                "\r";
        assertEquals(expected, doc.get());

    }

    public void testComment3() throws Exception {
        Document doc = new Document("a\r\n" +
                "\r\n" +
                "\r\n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 7), new PyComment().perform(ps));

        String expected = "#a\r\n" +
                "#\r\n" +
                "\r\n";
        assertEquals(expected, doc.get());

    }

    public void testComment4() throws Exception {
        Document doc = new Document("a\r\n" +
                "b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 6), new PyComment().perform(ps));

        String expected = "#a\r\n" +
                "#b";
        assertEquals(expected, doc.get());

    }
}
