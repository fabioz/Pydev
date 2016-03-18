/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis.ctrl_1;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;

import com.python.pydev.analysis.AnalysisPreferencesStub;
import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.ui.AutoImportsPreferencesPage;

public class UndefinedVariableFixParticipantTest extends AdditionalInfoTestsBase {

    private UndefinedVariableFixParticipant participant;
    private AnalysisPreferencesStub prefs;
    private int start;
    private int end;
    private int type;
    private MarkerAnnotationAndPosition marker;
    private String s;
    private PySelection ps;
    private String line;
    private int offset;
    private ArrayList<ICompletionProposal> props;

    public static void main(String[] args) {
        try {
            UndefinedVariableFixParticipantTest test = new UndefinedVariableFixParticipantTest();
            test.setUp();
            test.testFix7();
            test.tearDown();

            System.out.println("finished");
            junit.textui.TestRunner.run(UndefinedVariableFixParticipantTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        participant = new UndefinedVariableFixParticipant();
        super.restorePythonPath(false);
        prefs = new AnalysisPreferencesStub();

        props = new ArrayList<ICompletionProposal>();

    }

    public void testFix() throws Exception {
        start = 6;
        end = 13;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        s = "print testlib";
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        assertEquals("Import testlib", props.get(0).getDisplayString());
    }

    public void testFix2() throws Exception {
        start = 6;
        end = 13;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        // hard: undefined: testlib
        // the result should be testlib.unittest because the token is actually a module. If it was
        // not a module, there would be no problems
        s = "print testlib.unittest";
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();

        props = new ArrayList<ICompletionProposal>();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(2, props);
        assertContains("Import testlib.unittest", props);
        assertContains("Import testlib", props);
    }

    public void testFix3() throws Exception {
        start = 6;
        end = 13;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        //try 2
        s = "print testlib.unittest.anothertest";
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();

        props = new ArrayList<ICompletionProposal>();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(3, props);
        assertContains("Import testlib.unittest.anothertest", props);
        assertContains("Import testlib.unittest", props);
        assertContains("Import testlib", props);
    }

    public void testFix4() throws Exception {
        start = 6;
        end = 13;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        //try 3
        s = "print testlib.unittest.anothertest.AnotherTest";
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();

        props = new ArrayList<ICompletionProposal>();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(3, props);
        assertContains("Import testlib.unittest.anothertest", props);
        assertContains("Import testlib.unittest", props);
        assertContains("Import testlib", props);

    }

    public void testFix5() throws Exception {
        start = 6;
        end = 17;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        //try 3
        s = "print AnotherTest";
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();

        props = new ArrayList<ICompletionProposal>();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        assertContains("Import AnotherTest (testlib.unittest.anothertest)", props);

    }

    public void testFix6() throws Exception {
        start = 6;
        end = 11;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        //try 3
        s = "print DTest";
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();

        props = new ArrayList<ICompletionProposal>();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        //appears with __init__
        assertContains("Import DTest (relative.rel1.__init__)", props);
        CtxInsensitiveImportComplProposal compl = (CtxInsensitiveImportComplProposal) props.get(0);

        //but applies without it
        assertEquals("from relative.rel1 import DTest", compl.realImportRep);

    }

    public void testFix7() throws Exception {
        AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = true;
        try {
            start = 6;
            end = 11;
            type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

            marker = createMarkerStub(start, end, type);

            s = "print Priv3";
            ps = new PySelection(new Document(s));
            line = s;
            offset = s.length();

            props = new ArrayList<ICompletionProposal>();
            participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
            printProps(1, props);
            //appears with _
            assertContains("Import Priv3 (relative.rel1._priv1._priv2._priv3)", props);
            CtxInsensitiveImportComplProposal compl = (CtxInsensitiveImportComplProposal) props.get(0);

            //but applies without it
            assertEquals("from relative.rel1 import Priv3", compl.realImportRep);
        } finally {
            AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;
        }
    }

    public void testFix8() throws Exception {
        start = 6;
        end = 14;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        s = "print NotPriv3";
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();

        props = new ArrayList<ICompletionProposal>();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        //appears with _
        assertContains("Import NotPriv3 (relative.rel1._priv1._priv2.notpriv3)", props);
        CtxInsensitiveImportComplProposal compl = (CtxInsensitiveImportComplProposal) props.get(0);

        //but applies without it
        assertEquals("from relative.rel1._priv1._priv2.notpriv3 import NotPriv3", compl.realImportRep);
    }

    private void assertContains(String expected, List<ICompletionProposal> props) {
        StringBuffer buffer = new StringBuffer();
        for (ICompletionProposal proposal : props) {
            buffer.append("\n");
            buffer.append(proposal.getDisplayString());
            if (proposal.getDisplayString().equals(expected)) {
                return;
            }
        }
        fail("not found. Available:\n" + buffer);
    }

    private void printProps(int i, List<ICompletionProposal> props) {
        if (props.size() != i) {
            for (ICompletionProposal proposal : props) {
                System.out.println(proposal.getDisplayString());
            }
        }
        assertEquals(i, props.size());
    }

}
