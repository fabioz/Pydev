/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.tabnanny;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.editor.autoedit.TestIndentPrefs;

import com.python.pydev.analysis.AnalysisPreferencesStub;
import com.python.pydev.analysis.messages.IMessage;

public class TabNannyTest extends TestCase {

    public static void main(String[] args) {
        try {
            TabNannyTest analyzer2 = new TabNannyTest();
            analyzer2.setUp();
            analyzer2.testNoInconsistentIndentInStrings();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(TabNannyTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private AnalysisPreferencesStub prefs;

    @Override
    protected void setUp() throws Exception {
        this.prefs = new AnalysisPreferencesStub();
    }

    public void testTabErrors1() throws Exception {
        Document doc = new Document("" +
                "aaa\n" +
                "\t\n" +
                "    \n" +
                "    \t\n" +
                "ccc\n" +
                "");

        List<IMessage> messages = TabNanny.analyzeDoc(doc, this.prefs, "", new TestIndentPrefs(true, 4),
                new NullProgressMonitor());
        for (IMessage m : messages) {
            assertEquals("Mixed Indentation: Tab found", m.getMessage());
            int startLine = m.getStartLine(null);

            if (startLine == 2) {
                assertEquals(1, m.getStartCol(null));
                assertEquals(2, m.getEndCol(null));

            } else if (startLine == 4) {
                assertEquals(5, m.getStartCol(null));
                assertEquals(6, m.getEndCol(null));

            } else {
                throw new RuntimeException("Unexpected line:" + startLine);
            }
        }
        assertEquals(2, messages.size());
    }

    public void testTabErrors2() throws Exception {
        Document doc = new Document("" +
                "def m(b):\n" +
                "        pass\n" +
                "\tpass\n" +
                "    \n" +
                "    \n" +
                "    \n"
                +
                "\n" +
                "");

        List<IMessage> messages = TabNanny.analyzeDoc(doc, this.prefs, "", new TestIndentPrefs(true, 4),
                new NullProgressMonitor());
        IMessage m = messages.get(0);
        assertEquals(1, messages.size());
        assertEquals("Mixed Indentation: Tab found", m.getMessage());
        assertEquals(3, m.getStartLine(null));
        assertEquals(3, m.getEndLine(null));
        assertEquals(1, m.getStartCol(null));
        assertEquals(2, m.getEndCol(null));

    }

    public void testInconsistentIndent() throws Exception {
        Document doc = new Document("" +
                "def m(b):\n" +
                "    pass\n" +
                "   \n" +
                "\n" +
                "");

        List<IMessage> messages = TabNanny.analyzeDoc(doc, this.prefs, "", new TestIndentPrefs(true, 4),
                new NullProgressMonitor());
        assertEquals(0, messages.size());

    }

    public void testNoInconsistentIndentInStrings() throws Exception {
        Document doc = new Document("" +
                "def foo():\n" +
                "    string = \"\"\"bla\n" +
                "code()\n" +
                "  string ident\n"
                +
                "  string ident\"\"\"\n" +
                "\n" +
                "");

        List<IMessage> messages = TabNanny.analyzeDoc(doc, this.prefs, "", new TestIndentPrefs(true, 4),
                new NullProgressMonitor());
        assertEquals(0, messages.size());

    }

    public void testInconsistentIndent2() throws Exception {
        Document doc = new Document("" +
                "def m(b):\n" +
                "   pass\n" +
                "\n" +
                "");

        List<IMessage> messages = TabNanny.analyzeDoc(doc, this.prefs, "", new TestIndentPrefs(true, 4),
                new NullProgressMonitor());
        assertEquals(1, messages.size());
        IMessage m = messages.get(0);
        assertEquals("Bad Indentation (3 spaces)", m.getMessage());
        assertEquals(2, m.getStartLine(null));
        assertEquals(2, m.getEndLine(null));
        assertEquals(1, m.getStartCol(null));
        assertEquals(4, m.getEndCol(null));

    }

}
