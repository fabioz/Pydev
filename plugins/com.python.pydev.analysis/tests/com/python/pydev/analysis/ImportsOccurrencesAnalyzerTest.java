/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.autoedit.TestIndentPrefs;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.shared_core.SharedCorePlugin;

import com.python.pydev.analysis.messages.IMessage;

/**
 * Tests tokens gotten from imports to see if they really exist there
 */
public class ImportsOccurrencesAnalyzerTest extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            ImportsOccurrencesAnalyzerTest analyzer2 = new ImportsOccurrencesAnalyzerTest();
            analyzer2.setUp();
            analyzer2.testFromNotExistent();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(ImportsOccurrencesAnalyzerTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void testWx() throws Exception {
        if (TestDependent.PYTHON_WXPYTHON_PACKAGES != null) {
            doc = new Document("from wx import glcanvas\n" +
                    "print glcanvas");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 0);

        }
    }

    public void testModuleTokensErr() throws Exception {
        doc = new Document("from testlib.unittest import anothertest\n" +
                "print anothertest.unexistant\n" +
                "\n"
                +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable from import: unexistant", msgs[0].getMessage());
        assertEquals(19, msgs[0].getStartCol(doc));
    }

    public void testModuleTokensErr2() throws Exception {
        doc = new Document("from testlib.unittest import anothertest\n" +
                "print anothertest.unexistant()\n" +
                "\n"
                +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable from import: unexistant", msgs[0].getMessage());
        assertEquals(19, msgs[0].getStartCol(doc));
    }

    public void testModuleTokensErr3() throws Exception {
        doc = new Document("from testlib.unittest import anothertest\n"
                +
                "print anothertest.AnotherTest.unexistant()\n" +
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable from import: unexistant", msgs[0].getMessage());
        assertEquals(31, msgs[0].getStartCol(doc));
    }

    public void testModuleTokens3() throws Exception {
        doc = new Document("import testAssist\n" +
                "print testAssist.assist.ExistingClass.existingMethod\n" +
                "\n"
                +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testModuleTokens2() throws Exception {
        doc = new Document("from testlib.unittest import anothertest\n" +
                "print anothertest.AnotherTest.__init__\n"
                +
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testQtInit() throws Exception {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        if (TestDependent.PYTHON_QT4_PACKAGES != null) {
            doc = new Document("import PyQt4.QtGui\n" +
                    "print PyQt4.QtGui.QWidget.__init__\n" +
                    "\n" +
                    "\n");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 0);
        }
    }

    public void testTokenFromWildImport() throws Exception {
        doc = new Document("from testlib.unittest.anothertest import *\n" +
                "AnotherTest.__init__\n" +
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1); //unused imports
    }

    public void testRedefinedToken() throws Exception {
        doc = new Document("from testlib.unittest import anothertest\n" +
                "anothertest = anothertest.AnotherTest()\n"
                +
                "print anothertest.__init__\n" +
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testImportFromInit() throws Exception {
        doc = new Document("import testlib.unittest\n" + //as it resolves to testlib.unittest.__init__
                "print testlib.unittest.anothertest\n" + //this line works
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testImportFromInit2() throws Exception {
        doc = new Document("import testlib.unittest\n" + //as it resolves to testlib.unittest.__init__
                "print testlib.unittest.anothertest.AnotherTest\n" + //this line works
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testMethod() throws Exception {
        doc = new Document("from testlib.unittest import anothertest\n"
                +
                "print anothertest.AnotherTest().another.__class__\n" + //we should just get to the AnotherTest() part
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testUnresolvedImport() throws Exception {
        doc = new Document("from testlib import notexistant\n" + //it is not resolved,
                "print notexistant.foo\n" + //as it is not resolved, it should not be analyzed
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
    }

    public void testSilencedUnresolvedImport() throws Exception {
        doc = new Document("from testlib import notexistant #@UnresolvedImport\n" + //it is not resolved, so, let's signal this
                "print notexistant.foo\n" + //after silencing the unresolved import, this should also be silenced
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testStatic() throws Exception {
        doc = new Document("import extendable.static\n" +
                "print extendable.static.TestStatic.static1\n" +
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testStatic2() throws Exception {
        doc = new Document("from extendable import static\n" +
                "print static.TestStatic.static1\n" +
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testNested() throws Exception {
        doc = new Document("from extendable import nested\n" +
                "print nested.NestedClass.nestedMethod\n" +
                "\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 0);
    }

    public void testFromNotExistent() throws Exception {
        doc = new Document("from notExistent import foo\n" +
                "\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 2);
        assertContainsMsg("Unused import: foo", msgs);
        assertContainsMsg("Unresolved import: foo", msgs);
    }

    private IMessage[] analyzeDoc() {
        try {
            return analyzer.analyzeDocument(nature,
                    AbstractModule.createModuleFromDoc(null, null, doc, nature, true), prefs, doc,
                    new NullProgressMonitor(), new TestIndentPrefs(true, 4));
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void testQt() throws Exception {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON_QT4_PACKAGES != null) {
            doc = new Document("import PyQt4.QtGui\n" +
                    "print PyQt4.QtGui.QColor.red\n" +
                    "\n" +
                    "\n");
            analyzer = new OccurrencesAnalyzer();
            msgs = analyzeDoc();

            printMessages(msgs, 0);
        }
    }

}
