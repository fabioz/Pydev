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
import org.python.pydev.editor.autoedit.TestIndentPrefs;

public class PyBackspaceTest extends TestCase {

    public static void main(String[] args) {
        PyBackspaceTest test = new PyBackspaceTest();
        try {
            test.setUp();
            test.testBackspace23();
            test.tearDown();
            junit.textui.TestRunner.run(PyBackspaceTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PyBackspace backspace;

    @Override
    protected void setUp() throws Exception {
        this.backspace = new PyBackspace();
        this.backspace.setIndentPrefs(new TestIndentPrefs(true, 4));
    }

    public void testBackspace() throws Exception {
        Document doc = new Document("a = 10");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 1", doc.get());
    }

    public void testEraseMoreThan() throws Exception {
        Document doc = new Document("a = 10");

        backspace.perform(new PySelection(doc, 0, doc.getLength(), 0));
        assertEquals("a = 1", doc.get());

        backspace.setDontEraseMoreThan(5);
        backspace.perform(new PySelection(doc, 0, doc.getLength(), 0));
        assertEquals("a = 1", doc.get());

        backspace.setDontEraseMoreThan(4);
        backspace.perform(new PySelection(doc, 0, doc.getLength(), 0));
        assertEquals("a = ", doc.get());
    }

    public void testBackspace2() throws Exception {
        Document doc = new Document("a = 10     ");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace2a() throws Exception {
        this.backspace.setIndentPrefs(new TestIndentPrefs(false, 4));
        Document doc = new Document("a = 10\t\t");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace3() throws Exception {
        Document doc = new Document("a =  10");
        PySelection ps = new PySelection(doc, 0, 5, 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace3a() throws Exception {
        this.backspace.setIndentPrefs(new TestIndentPrefs(false, 4));
        Document doc = new Document("a =  10");
        PySelection ps = new PySelection(doc, 0, 5, 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace4() throws Exception {
        Document doc = new Document("a =  10");
        PySelection ps = new PySelection(doc, 0, 3, 2);

        backspace.perform(ps);
        assertEquals("a =10", doc.get());
    }

    public void testBackspace5() throws Exception {
        Document doc = new Document("a = 10");
        PySelection ps = new PySelection(doc, 0, 0, 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace6() throws Exception {
        Document doc = new Document("a = 10\r\n");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace7() throws Exception {
        Document doc = new Document("a = 10\n");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace8() throws Exception {
        Document doc = new Document("a = 10\r");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace9() throws Exception {
        Document doc = new Document("a = 10\r");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10", doc.get());
    }

    public void testBackspace10() throws Exception {
        Document doc = new Document("a = 10\n" +
                "    ");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10\n", doc.get());
    }

    public void testBackspace10a() throws Exception {
        this.backspace.setIndentPrefs(new TestIndentPrefs(false, 4));
        Document doc = new Document("a = 10\n" +
                "\t");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10\n", doc.get());
    }

    public void testBackspace11() throws Exception {
        Document doc = new Document("a = 10\n" +
                "\t");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10\n", doc.get());
    }

    public void testBackspace12() throws Exception {
        Document doc = new Document("a = 10\n" +
                "      ");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a = 10\n    ", doc.get());
    }

    public void testBackspace13() throws Exception {
        Document doc = new Document("      a = 10");
        PySelection ps = new PySelection(doc, 0, 6, 0);

        backspace.perform(ps);
        assertEquals("    a = 10", doc.get());
    }

    public void testBackspace14() throws Exception {
        Document doc = new Document("      a = 10  #comment");
        PySelection ps = new PySelection(doc, 0, 14, 0);

        backspace.perform(ps);
        assertEquals("      a = 10 #comment", doc.get());
    }

    public void testBackspace15() throws Exception {
        Document doc = new Document("        ");
        PySelection ps = new PySelection(doc, 0, 4, 0);

        backspace.perform(ps);
        assertEquals("    ", doc.get());
    }

    public void testBackspace16() throws Exception {
        Document doc = new Document("          ");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("        ", doc.get());
    }

    public void testBackspace16a() throws Exception {
        Document doc = new Document("\t\t");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("\t", doc.get());
    }

    public void testBackspace16b() throws Exception {
        this.backspace.setIndentPrefs(new TestIndentPrefs(false, 4));
        Document doc = new Document("\t\t");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("\t", doc.get());
    }

    public void testBackspace16c() throws Exception {
        this.backspace.setIndentPrefs(new TestIndentPrefs(true, 4));
        Document doc = new Document("\t\t ");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("\t\t", doc.get());
    }

    public void testBackspace16d() throws Exception {
        this.backspace.setIndentPrefs(new TestIndentPrefs(true, 4));
        Document doc = new Document("\t\t  ");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("\t\t", doc.get());
    }

    public void testBackspace17() throws Exception {
        Document doc = new Document("ab(\n     ");
        PySelection ps = new PySelection(doc, 0, 9, 0);

        backspace.perform(ps);
        assertEquals("ab(\n    ", doc.get());
    }

    public void testBackspace18() throws Exception {
        Document doc = new Document("ab(\n    ");
        PySelection ps = new PySelection(doc, 0, 8, 0);

        backspace.perform(ps);
        assertEquals("ab(\n" +
                "   ", doc.get());
    }

    public void testBackspace19() throws Exception {
        Document doc = new Document("a(\n    ");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a(\n" +
                "  ", doc.get());
    }

    public void testBackspace20() throws Exception {
        Document doc = new Document("a(\n  ");
        PySelection ps = new PySelection(doc, 0, doc.getLength(), 0);

        backspace.perform(ps);
        assertEquals("a(\n" +
                "", doc.get());
    }

    public void testBackspace21() throws Exception {
        Document doc = new Document("a()");
        PySelection ps = new PySelection(doc, 0, doc.getLength() - 1, 0);

        backspace.perform(ps);
        assertEquals("a", doc.get());

    }

    public void testBackspace21a() throws Exception {
        Document doc = new Document("a(()");
        PySelection ps = new PySelection(doc, 0, doc.getLength() - 1, 0);

        backspace.perform(ps);
        assertEquals("a()", doc.get());

    }

    public void testBackspace22() throws Exception {
        Document doc = new Document("''");
        PySelection ps = new PySelection(doc, 0, doc.getLength() - 1, 0);

        backspace.perform(ps);
        assertEquals("", doc.get());

    }

    public void testBackspace23() throws Exception {
        Document doc = new Document("d = [a(b[c]) []");
        PySelection ps = new PySelection(doc, 0, doc.getLength() - 1, 0);

        backspace.perform(ps);
        assertEquals("d = [a(b[c]) ]", doc.get());

    }
}
