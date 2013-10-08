/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.parser.fastparser.ScopesParser;
import org.python.pydev.shared_ui.actions.ScopeSelectionAction;

/**
 * @author fabioz
 *
 */
public class PyScopeSelectionTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyScopeSelectionTest test = new PyScopeSelectionTest();
            test.setUp();
            //            test.testWithParseError();
            test.tearDown();

            junit.textui.TestRunner.run(PyScopeSelectionTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void check(String string, int initialOffset, int initialLenOffset, int finalOffset, int finalLenOffset) {
        ScopeSelectionAction scopeSelection = new ScopeSelectionAction();
        Document doc = new Document(string);
        ITextSelection selection = new TextSelection(doc, initialOffset, initialLenOffset);

        ITextSelection newSelection = scopeSelection.getNewSelection(doc, selection, new PythonPairMatcher(),
                new ScopesParser());
        assertEquals("Expected offset to be: " + finalOffset +
                " actual offset: " + newSelection.getOffset() +
                " -- ",
                finalOffset, newSelection.getOffset());
        assertEquals(finalLenOffset, newSelection.getLength());
    }

    public void testSimple() {
        check("a.b", 0, 0, 0, 1);
        check("a.b", 1, 0, 0, 1);
        check("a.b", 2, 0, 2, 1);
        check("a.b", 3, 0, 2, 1);

        check("a.b()", 3, 0, 2, 1);
        check("a.b()", 4, 0, 3, 2);
    }

    public void testWithSelection() {
        check("aa.b", 0, 1, 0, 2);
        check("a.b", 0, 1, 0, 3);
        check("aaa.b", 0, 4, 0, 5);
        check("aaa.b", 4, 1, 0, 5);
        check("aaa.b()", 4, 1, 0, 7);
        check("aaa.b().o", 4, 1, 0, 9);
        check("a().o", 1, 2, 0, 5);
        check("a(call()).o", 2, 2, 2, 4);
        check("a(call()).o", 2, 4, 2, 6);
        check("a(call()).o", 2, 6, 0, 11);
    }

    public void testWithStructures() {
        String doc = "" +
                "def m1():\n" +
                "  if True:\n" + //True starts at 15
                "    pass";

        check(doc, 15, 0, 15, 4);
        check(doc, 15, 4, 12, 17);
        check(doc, 12, 17, 0, doc.length());
    }

    public void testWithDictInParens() {
        String doc = "(1,\n" +
                " {a\n" +
                ":b})\n" +
                "\n" +
                "class Bar(object):\n" +
                "    call" +
                "";

        check(doc, doc.length() - 1, 0, doc.length() - 4, 4);
        check(doc, doc.length() - 4, 4, 14, 27);
    }

    public void testWithParseError() {
        String doc = "(1\n" +
                "\n" +
                "class Bar(object):\n" +
                "    call" +
                "";

        check(doc, doc.length() - 1, 0, doc.length() - 4, 4);
        check(doc, doc.length() - 4, 4, 4, 27);
    }

}
