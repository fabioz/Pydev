/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyEdit.MyResources;

import junit.framework.TestCase;

public class PyMoveLineActionTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyMoveLineActionTest test = new PyMoveLineActionTest();
            test.setUp();
            test.testMoveWithString();
            test.tearDown();
            junit.textui.TestRunner.run(PyMoveLineActionTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private MyResources resources;
    private PyMoveLineDownAction actionDown;
    private PyMoveLineUpAction actionUp;

    @Override
    protected void setUp() throws Exception {
        resources = new PyEdit.MyResources();
        actionDown = new PyMoveLineDownAction(resources, null, null);
        actionUp = new PyMoveLineUpAction(resources, null, null);
    }

    public void testMoveDownWithIndent() {
        String content1 = "" +
                "b = 10\n" +
                "def m1():";

        String content2 = "" +
                "def m1():\n" +
                "    b = 10";

        check(actionDown, content1, content2, 0, 0);
    }

    public void testMoveDownWithIndent2() {
        String content1 = "" +
                "def m1():\n" +
                "    b = 10\n" +
                "";

        String content2 = "" +
                "def m1():\n" +
                "\n" +
                "    b = 10";

        check(actionDown, content1, content2, 15, 0);
    }

    public void testMoveUpWithIndent() {
        String content1 = "" +
                "def m1():\n" +
                "    b = 10";

        String content2 = "" +
                "b = 10\n" +
                "def m1():";

        check(actionUp, content1, content2, 15, 0);

        content1 = "" +
                "def m1():\n" +
                "\n" +
                "    b = 10";

        content2 = "" +
                "def m1():\n" +
                "    b = 10\n" +
                "";

        check(actionUp, content1, content2, 15, 0);
    }

    public void testMoveUpWithIndent2() {
        String content1 = "" +
                "def m1(a,\n" +
                "       b):\n" +
                "    b = 10";

        String content2 = "" +
                "def m1(a,\n" +
                "       b = 10\n" +
                "       b):";

        check(actionUp, content1, content2, content1.length() - 2, 0);
    }

    public void testMoveUpWithIndent3() {
        String content1 = "" +
                "def m1(a,\n" +
                "       b):\n" +
                "    b = 10";

        String content2 = "" +
                "b):\n" +
                "def m1(a,\n" +
                "    b = 10";

        check(actionUp, content1, content2, 17, 0); //line1
    }

    public void testMove() {
        String aBeforeContent = "" +
                "a = 10\n" +
                "b = 10";

        String bBeforeContent = "" +
                "b = 10\n" +
                "a = 10";

        check(actionDown, aBeforeContent, bBeforeContent, 0, 0);
        check(actionDown, bBeforeContent, aBeforeContent, 0, 0);
        check(actionDown, aBeforeContent, aBeforeContent, 50, 0); //out of range
        check(actionDown, aBeforeContent, aBeforeContent, 10, 0); //last line

        check(actionUp, aBeforeContent, bBeforeContent, 10, 0);
        check(actionUp, bBeforeContent, aBeforeContent, 10, 0);
        check(actionUp, aBeforeContent, aBeforeContent, 0, 0); //first line selected
        check(actionUp, aBeforeContent, aBeforeContent, 50, 0); //out of range
    }

    public void testMove2() {

        //Check blocks
        String aBeforeContent = "" +
                "a = 10\n" +
                "a = 10\n" +
                "b = 10";

        String bBeforeContent = "" +
                "b = 10\n" +
                "a = 10\n" +
                "a = 10";

        check(actionDown, aBeforeContent, bBeforeContent, 0, 10);
        check(actionUp, bBeforeContent, aBeforeContent, 10, 8);

    }

    public void testMoveWithComments() {

        String content1 = "" +
                "def m1(a,\n" +
                "       b):\n" +
                "    b = 10\n" +
                "#    c=30"; //should remain in the same indentation

        String content2 = "" +
                "def m1(a,\n" +
                "       b = 10\n" +
                "#    c=30\n" +
                "       b):";

        check(actionUp, content1, content2, content1.length() - "b = 10\n#    c=30".length(), "b = 10\n#   ".length());
    }

    public void testMoveWithComments2() {

        String content1 = "" +
                "def m1(a):\n" +
                "#    b = 10\n" +
                "    c=30";

        String content2 = "" +
                "def m1(a):\n" +
                "    c=30\n" +
                "#    b = 10";

        //check if it properly disconsiders indentation when going from v2 to v1 here.
        check(actionDown, content2, content1, content2.length() - "c=30\n#    b = 10".length(), 0);

        check(actionUp, content1, content2, content1.length(), 0);
    }

    public void testMoveWithEmptyLines() {

        String content1 = "" +
                "def m1(a):\n" +
                "    b = 10\n" +
                "\n" +
                "    c=30";

        String content1a = "" +
                "def m1(a):\n" +
                "    b = 10\n" +
                "    \n" +
                "    c=30";

        String content2 = "" +
                "b = 10\n" +
                "\n" +
                "c=30\n" +
                "def m1(a):";

        check(actionUp, content1, content2, 17, 13);
        check(actionDown, content2, content1a, 0, 13);
    }

    public void testMoveWithString() {

        String content1 = "" +
                "def m1(a):\n" +
                "    '''\n" +
                "        test\n" +
                "    '''\n" +
                "    c=30";

        String content2 = "" +
                "def m1(a):\n" +
                "    '''\n" +
                "        test\n" +
                "    c=30\n" +
                "    '''";

        String content2a = "" +
                "def m1(a):\n" +
                "    '''\n" +
                "        test\n" +
                "        c=30\n" +
                "    '''";

        check(actionDown, content2a, content1, content2a.length() - "c=30\n    '''".length(), 0);
        check(actionUp, content1, content2, content1.length(), 0);
    }

    public void check(PyMoveLineAction action, String initialContent, String finalContent, int offset, int len) {
        IDocument document = new Document(initialContent);
        ITextSelection sel = new TextSelection(document, offset, len);

        action.move(null, null, document, sel);
        assertEquals(finalContent, document.get());
    }
}
