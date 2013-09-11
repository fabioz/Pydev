/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 9, 2006
 * @author Fabio
 */
package com.python.pydev.analysis;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.autoedit.TestIndentPrefs;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

import com.python.pydev.analysis.messages.IMessage;

public class OccurrencesAnalyzerListCompTest extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerListCompTest analyzer2 = new OccurrencesAnalyzerListCompTest();
            analyzer2.setUp();
            analyzer2.testListComprehension6();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(OccurrencesAnalyzerListCompTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void testListComprehension() {
        doc = new Document("def m1():\n" +
                "    print [i for i in range(10)]");
        checkNoError();
    }

    public void testListComprehension2() {
        doc = new Document("enumeratedDays = ((0, ('monday', 'mon')), (1, ('tuesday', 'tue')))\n" +
                "print dict((day, index) for index, daysRep in enumeratedDays for day in daysRep)\n");
        checkNoError();
    }

    public void testListComprehension3() {
        doc = new Document("enumeratedDays = ((0, ('monday', 'mon')), (1, ('tuesday', 'tue')))\n" +
                "print dict((index) for index, daysRep in (enumeratedDays for day in daysRep))\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable: daysRep", msgs[0].getMessage());
    }

    private IMessage[] analyzeDoc() {
        try {
            return analyzer.analyzeDocument(nature,
                    (SourceModule) AbstractModule.createModuleFromDoc(null, null, doc, nature, true), prefs, doc,
                    new NullProgressMonitor(), new TestIndentPrefs(true, 4));
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void testListComprehension3a() {
        doc = new Document("enumeratedDays = ((0, ('monday', 'mon')), (1, ('tuesday', 'tue')))\n" +
                "print dict((day, index) for index, daysRep in (foo for day in enumeratedDays))\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        assertEquals(2, msgs.length);
        assertContainsMsg("Undefined variable: foo", msgs);
        assertContainsMsg("Undefined variable: day", msgs);
    }

    public void testListComprehension3b() {
        doc = new Document("enumeratedDays = ((0, ('monday', 'mon')), (1, ('tuesday', 'tue')))\n" +
                "print dict((day, index) for index, daysRep in (day for day in enumeratedDays))\n");//the day from the "(day for day in enumeratedDays)" should not be kept in the scope
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable: day", msgs[0].getMessage());
    }

    public void testListComprehension4() {
        doc = new Document("enumeratedDays = ((0, ('monday', 'mon')), (1, ('tuesday', 'tue')))\n" +
                "print dict((day, index) for index, daysRep in enumeratedDays for day in bla)\n");
        analyzer = new OccurrencesAnalyzer();
        msgs = analyzeDoc();

        printMessages(msgs, 1);
        assertEquals("Undefined variable: bla", msgs[0].getMessage());
    }

    public void testListComprehension5() {
        doc = new Document("data = [[1,2,3],[4,5,6]]\n" +
                "newdata = [[val * 2 for val in lst] for lst in data]\n");
        checkNoError();
    }

    public void testListComprehension6() {
        doc = new Document("def double_cycle():\n" +
                "    print [[i ** j for i in range(10)] for j in range(5)]\n"
                +
                "\n");
        checkNoError();
    }

}
