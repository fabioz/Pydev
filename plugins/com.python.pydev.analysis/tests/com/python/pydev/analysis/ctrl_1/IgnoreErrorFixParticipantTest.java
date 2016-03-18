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
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;

import com.python.pydev.analysis.AnalysisPreferencesStub;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;

public class IgnoreErrorFixParticipantTest extends AdditionalInfoTestsBase {

    private IgnoreErrorParticipant participant;
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
    private FormatStd format;

    public static void main(String[] args) {
        try {
            IgnoreErrorFixParticipantTest test = new IgnoreErrorFixParticipantTest();
            test.setUp();
            test.testFix5();
            test.tearDown();

            System.out.println("finished");
            junit.textui.TestRunner.run(IgnoreErrorFixParticipantTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        format = new FormatStd();
        format.spacesBeforeComment = 2;
        participant = new IgnoreErrorParticipant(format);
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
        assertEquals("UndefinedVariable", props.get(0).getDisplayString());

        props.get(0).apply(ps.getDoc());

        assertEquals("print testlib  #@UndefinedVariable", ps.getDoc().get());
    }

    public void testFix2() throws Exception {
        start = 6;
        end = 13;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        s = "print testlib  #comment";
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        assertEquals("UndefinedVariable", props.get(0).getDisplayString());

        props.get(0).apply(ps.getDoc());

        assertEquals("print testlib  #comment @UndefinedVariable", ps.getDoc().get());
    }

    public void testFix3() throws Exception {
        start = 6;
        end = 13;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        s = "print testlib  #comment  "; //2 spaces at end
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        assertEquals("UndefinedVariable", props.get(0).getDisplayString());

        props.get(0).apply(ps.getDoc());

        assertEquals("print testlib  #comment  @UndefinedVariable", ps.getDoc().get());
    }

    public void testFix4() throws Exception {
        start = 0;
        end = 0;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        s = ""; //empty doc
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        assertEquals("UndefinedVariable", props.get(0).getDisplayString());

        props.get(0).apply(ps.getDoc());

        assertEquals("#@UndefinedVariable", ps.getDoc().get());
    }

    public void testFix5() throws Exception {
        start = 0;
        end = 0;
        type = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        marker = createMarkerStub(start, end, type);

        s = " "; //only one space in doc
        ps = new PySelection(new Document(s));
        line = s;
        offset = s.length();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        assertEquals("UndefinedVariable", props.get(0).getDisplayString());

        props.get(0).apply(ps.getDoc());

        assertEquals(" #@UndefinedVariable", ps.getDoc().get());
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
