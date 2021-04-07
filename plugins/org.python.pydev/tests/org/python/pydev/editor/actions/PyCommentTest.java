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

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.shared_core.actions.LineCommentOption;
import org.python.pydev.shared_core.structure.Tuple;

import junit.framework.TestCase;

public class PyCommentTest extends TestCase {

    public static void main(String[] args) {
        PyCommentTest test = new PyCommentTest();
        try {
            test.setUp();
            test.testCommentWithDifferentCodingStd();
            test.tearDown();
            junit.textui.TestRunner.run(PyCommentTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FormatStd std;

    @Override
    protected void setUp() throws Exception {
        std = new FormatStd();
    }

    public void testComment() throws Exception {
        Document doc = new Document("a\n" +
                "\n" +
                "\n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 6),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED));

        String expected = "#a\n" +
                "#\n" +
                "#\n";
        assertEquals(expected, doc.get());

    }

    public void testComment2() throws Exception {
        Document doc = new Document("a\r" +
                "\r" +
                "\r");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 6),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED));

        String expected = "#a\r" +
                "#\r" +
                "#\r";
        assertEquals(expected, doc.get());

    }

    public void testComment3() throws Exception {
        Document doc = new Document("a\r\n" +
                "\r\n" +
                "\r\n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 8),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED));

        String expected = "#a\r\n" +
                "#\r\n" +
                "#\r\n";
        assertEquals(expected, doc.get());

    }

    public void testComment4() throws Exception {
        Document doc = new Document("a\r\n" +
                "b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 6),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED));

        String expected = "#a\r\n" +
                "#b";
        assertEquals(expected, doc.get());

    }

    public void testCommentWithDifferentCodingStd() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document(" a\r\n" +
                "b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 9),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_LINE_START));

        String expected = "#  a\r\n" +
                "# b";
        assertEquals(expected, doc.get());

    }

    public void testCommentWithDifferentCodingStd2() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document(" a\r\n" +
                "b");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 9),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED));

        String expected = " # a\r\n" +
                "# b";
        assertEquals(expected, doc.get());

    }

    public void testCommentWithDifferentCodingStdLineOriented() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document(" a\r\n" +
                "   b\n"
                + "\n"
                + "  c\n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 23),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED));

        String expected = " # a\r\n"
                + "   # b\n"
                + "   #\n"
                + "  # c\n"
                + "";
        assertEquals(expected, doc.get());

    }

    public void testCommentWithDifferentCodingStd3() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("\r\n" +
                "b\n"
                + "\n"
                + "c\n");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 14),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_LINE_START));

        String expected = "# \r\n"
                + "# b\n"
                + "# \n"
                + "# c\n"
                + "";
        assertEquals(expected, doc.get());

    }

    public void testIdentedComment() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "    if a:\n"
                + "        pass");
        PySelection ps = new PySelection(doc, 0, 0, doc.getLength());
        assertEquals(new Tuple<Integer, Integer>(0, 42),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT));

        String expected = "# def method():\n"
                + "#     if a:\n"
                + "#         pass";
        assertEquals(expected, doc.get());
    }

    public void testIdentedComment2() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "    if a:\n"
                + "        pass");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        assertEquals(new Tuple<Integer, Integer>(14, 26),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT));

        String expected = "def method():\n"
                + "    # if a:\n"
                + "    #     pass";
        assertEquals(expected, doc.get());
    }

    public void testIdentedComment3() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "    if a:\n"
                + "\n"
                + "        pass");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT);

        String expected = "def method():\n"
                + "    # if a:\n"
                + "    #\n"
                + "    #     pass";
        assertEquals(expected, doc.get());
    }

    public void testIdentedComment4() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "    if a:\n"
                + "\n"
                + "        pass\n");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT);

        String expected = "def method():\n"
                + "    # if a:\n"
                + "    #\n"
                + "    #     pass\n"
                + "    #\n"
                + "";
        assertEquals(expected, doc.get());
    }

    public void testIdentedComment5() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "    if a:\n"
                + "\n"
                + "  \n"
                + "        pass\n"
                + "\n");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT);

        String expected = "def method():\n"
                + "    # if a:\n"
                + "    #\n"
                + "    #\n"
                + "    #     pass\n"
                + "    #\n"
                + "    #\n"
                + "";
        assertEquals(expected, doc.get());
    }

    public void testComment5() throws Exception {
        Document doc = new Document("def method():\n"
                + "    if a:\n"
                + "        pass");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        assertEquals(new Tuple<Integer, Integer>(14, 24),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_LINE_START));

        String expected = "def method():\n"
                + "#    if a:\n"
                + "#        pass";
        assertEquals(expected, doc.get());
    }

    public void testComment6() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "    if a:\n"
                + "        pass");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        assertEquals(new Tuple<Integer, Integer>(14, 26),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED));

        String expected = "def method():\n"
                + "    # if a:\n"
                + "        # pass";
        assertEquals(expected, doc.get());
    }

    public void testComment7() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        assertEquals(new Tuple<Integer, Integer>(14, 9),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT_LINE_ORIENTED));

        String expected = "def method():\n"
                + "#\n"
                + "#\n"
                + "#\n"
                + "#\n"
                + "#";
        assertEquals(expected, doc.get());
    }

    public void testComment8() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        assertEquals(new Tuple<Integer, Integer>(14, 10),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT));

        String expected = "def method():\n"
                + "#\n"
                + "#\n"
                + "#\n"
                + "#\n"
                + "#\n";
        assertEquals(expected, doc.get());
    }

    public void testComment9() throws Exception {
        std.spacesInStartComment = 1;

        Document doc = new Document("def method():\n"
                + "\n"
                + "  c\n"
                + "\n"
                + "\n");
        PySelection ps = new PySelection(doc, 1, 0, doc.getLength() - 13);
        assertEquals(new Tuple<Integer, Integer>(14, 22),
                new PyComment(std).perform(ps, LineCommentOption.ADD_COMMENTS_INDENT));

        String expected = "def method():\n"
                + "  #\n"
                + "  # c\n"
                + "  #\n"
                + "  #\n"
                + "  #\n";
        assertEquals(expected, doc.get());
    }
}
