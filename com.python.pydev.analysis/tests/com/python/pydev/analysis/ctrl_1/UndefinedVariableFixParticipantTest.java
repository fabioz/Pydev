/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis.ctrl_1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.actions.PySelection;

import com.python.pydev.analysis.AnalysisPreferencesStub;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class UndefinedVariableFixParticipantTest extends AdditionalInfoTestsBase {

    private UndefinedVariableFixParticipant participant;
    private AnalysisPreferencesStub prefs;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(UndefinedVariableFixParticipantTest.class);

    }

    protected void setUp() throws Exception {
        super.setUp();
        participant = new UndefinedVariableFixParticipant();
        super.restorePythonPath(false);
        prefs = new AnalysisPreferencesStub();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testFix() throws Exception {
        HashMap attrs = new HashMap();
        attrs.put(AnalysisRunner.PYDEV_PROBLEM_ID_MARKER_INFO, IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE);
        attrs.put(IMarker.CHAR_START, 6);
        attrs.put(IMarker.CHAR_END, 13);

        MarkerStub marker = new MarkerStub(attrs);
        
        // easy: undefined: testlib
        String s = "print testlib"; 
        PySelection ps = new PySelection(new Document(s));
        String line = s;
        int offset = s.length();
        
        List<ICompletionProposal> props = new ArrayList<ICompletionProposal>();
        participant.addProps(marker, prefs, line, ps, offset, nature, null, props);
        printProps(1, props);
        assertEquals("Import testlib", props.get(0).getDisplayString());
        
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
        
        
    }

    private void assertContains(String expected, List<ICompletionProposal> props) {
        for (ICompletionProposal proposal : props) {
            if(proposal.getDisplayString().equals(expected)){
                return;
            }
        }
        fail("not found");
    }

    private void printProps(int i, List<ICompletionProposal> props) {
        if(props.size() != i){
            for (ICompletionProposal proposal : props) {
                System.out.println(proposal.getDisplayString());
            }
        }
        assertEquals(i, props.size());
    }

}
