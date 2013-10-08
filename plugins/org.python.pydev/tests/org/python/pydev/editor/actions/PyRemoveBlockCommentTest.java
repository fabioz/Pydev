/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
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

    public static void main(String[] args) {
        try {
            PyRemoveBlockCommentTest test = new PyRemoveBlockCommentTest();
            test.setUp();
            test.test8();
            test.tearDown();
            junit.textui.TestRunner.run(PyRemoveBlockCommentTest.class);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testUncommentBlock() throws Exception {
        doc = new Document("#a\n" +
                "#b");
        ps = new PySelection(doc, 0, 0, 0);
        new PyRemoveBlockComment().perform(ps);

        expected = "a\n" +
                "b";
        assertEquals(expected, doc.get());

        doc = new Document("#----\n" +
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

        doc = new Document("#---- aa\n" +
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

        doc = new Document("    #---- aa\n" +
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

        doc = new Document("    #---- aa\n" +
                "        #---- b\n" +
                "    #---- c\n" +
                "\n" +
                "\n");
        ps = new PySelection(doc, 0, 0, 0);
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        pyRemoveBlockComment.perform(ps);

        expected = "    aa\n" +
                "        b\n" +
                "    c\n" +
                "\n" +
                "\n";
        assertEquals(expected, doc.get());

    }

    public void test5() throws Exception {

        doc = new Document("\t#---- aa\n" +
                "\t\t#---- b\n" +
                "\t#---- c\n" +
                "\n" +
                "\n");
        ps = new PySelection(doc, 0, 0, 0);
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        pyRemoveBlockComment.perform(ps);

        expected = "\taa\n" +
                "\t\tb\n" +
                "\tc\n" +
                "\n" +
                "\n";
        assertEquals(expected, doc.get());

    }

    public void test6() throws Exception {

        doc = new Document("\t#---- aa\n" +
                "\t\t'--#--' b\n" + //Ignore this line when removing block comments
                "\t#---- c\n" + //This won't be touched as the block broke on the previous line.
                "\n" +
                "\n");
        ps = new PySelection(doc, 0, 0, 0);
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        pyRemoveBlockComment.perform(ps);

        expected = "\taa\n" +
                "\t\t'--#--' b\n" +
                "\t#---- c\n" +
                "\n" +
                "\n";
        assertEquals(expected, doc.get());
    }

    public void test7() throws Exception {

        doc = new Document("\t# aa\n" +
                "\t# aa\n" +
                "\n");
        ps = new PySelection(doc, 0, 0, 0);
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        pyRemoveBlockComment.perform(ps);

        expected = "\taa\n" +
                "\taa\n" +
                "\n";
        assertEquals(expected, doc.get());
    }

    public void test8() throws Exception {

        doc = new Document("# aa\n" +
                "# aa\n" +
                "\n");
        ps = new PySelection(doc, 0, 0, 0);
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        pyRemoveBlockComment.perform(ps);

        expected = "aa\n" +
                "aa\n" +
                "\n";
        assertEquals(expected, doc.get());
    }

    public void test9() throws Exception {

        doc = new Document("    #  aa\n" +
                "    #  aa\n" +
                "\n");
        ps = new PySelection(doc, 0, 0, 0);
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        pyRemoveBlockComment.perform(ps);

        expected = "    aa\n" +
                "    aa\n" +
                "\n";
        assertEquals(expected, doc.get());
    }

}
