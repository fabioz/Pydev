/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.ast.codecompletion.PyCodeCompletion;
import org.python.pydev.ast.codecompletion.revisited.PyEditStub;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.refactoring.AbstractPyRefactoring;
import org.python.pydev.core.IAnalysisPreferences;
import org.python.pydev.core.IMarkerInfoForAnalysis;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.IMiscConstants;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.editor_input.PydevFileEditorInput;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.AnalysisTestsBase;
import com.python.pydev.analysis.additionalinfo.builders.AnalysisRunner;
import com.python.pydev.analysis.marker_quick_fixes.TddQuickFixFromMarkersParticipant;
import com.python.pydev.analysis.refactoring.quick_fixes.DummyMarkerInfoForAnalysis;
import com.python.pydev.analysis.refactoring.refactorer.Refactorer;
import com.python.pydev.analysis.refactoring.tdd.TddCodeGenerationQuickFixWithoutMarkersParticipant;
import com.python.pydev.analysis.refactoring.tdd.TemplateInfo;
import com.python.pydev.refactoring.tdd.completions.TddRefactorCompletion;
import com.python.pydev.refactoring.tdd.completions.TddRefactorCompletionInModule;

/**
 * @author Fabio
 *
 */
public class TddCodeGenerationQuickFixParticipantWithMarkersTest extends AnalysisTestsBase {

    public static void main(String[] args) {

        try {
            //DEBUG_TESTS_BASE = true;
            TddCodeGenerationQuickFixParticipantWithMarkersTest test = new TddCodeGenerationQuickFixParticipantWithMarkersTest();
            test.setUp();
            test.testCreateClass();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(TddCodeGenerationQuickFixParticipantWithMarkersTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        AbstractPyRefactoring.setPyRefactoring(new Refactorer());
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(TestDependent.getCompletePythonLib(true, isPython3Test()) +
                "|" + TestDependent.PYTHON2_PIL_PACKAGES +
                "|"
                + TestDependent.TEST_PYSRC_TESTING_LOC +
                "configobj-4.6.0-py2.6.egg", false);

        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
        TddCodeGenerationQuickFixWithoutMarkersParticipant.onGetTddPropsError = new ICallback<Boolean, Exception>() {

            @Override
            public Boolean call(Exception e) {
                throw new RuntimeException("Error:" + org.python.pydev.shared_core.log.Log.getExceptionStr(e));
            }
        };
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            @Override
            public Object call(CompletionRecursionException e) {
                throw new RuntimeException(
                        "Recursion error:" + org.python.pydev.shared_core.log.Log.getExceptionStr(e));
            }

        };
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
        AbstractPyRefactoring.setPyRefactoring(null);
        PyCodeCompletion.onCompletionRecursionException = null;
    }

    public void testCreateClass() throws Exception {
        String initial = """
                class Foo(object):
                    def m1(self):
                        NewClass
                """;
        String expected = """
                class NewClass(object):
                    pass


                class Foo(object):
                    def m1(self):
                        NewClass
                """;
        String stringForRegion = "NewClass";
        String expectedLabel = "Create NewClass class";
        int markerType = IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE;

        check(initial, expected, stringForRegion, expectedLabel, markerType);
    }

    public void testFromActualError() throws Exception {
        String initial = """
                class Foo(object):
                    def m1(self):
                        NewClass
                """;
        String expected = """
                class NewClass(object):
                    pass


                class Foo(object):
                    def m1(self):
                        NewClass
                """;
        String expectedLabel = "Create NewClass class";

        checkFromAnalysis(initial, expected, expectedLabel);
    }

    public void testCreateModule() throws Exception {
        String initial = """
                import some_module
                """;
        // i.e.: New empty module
        String expected = """
                """;
        String stringForRegion = "some_module";
        String expectedLabel = "Create some_module module";
        int markerType = IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT;

        check(initial, expected, stringForRegion, expectedLabel, markerType);
    }

    public void testCreateMethod() throws Exception {
        String initial = """
                print i
                class Foo(object):

                #comment

                    def m1(self):
                        self.m2
                """;
        String expected = """
                print i
                class Foo(object):

                #comment


                    def m2(self):
                        pass
                ----
                ----
                    def m1(self):
                        self.m2
                """.replace('-', ' ');
        String stringForRegion = "m2";
        String expectedLabel = "Create m2 method at Foo";

        int markerType = -1; // No marker
        check(initial, expected, stringForRegion, expectedLabel, markerType);
    }

    private void check(String initial, String expected, String stringForRegion, String expectedLabel, int markerType)
            throws BadLocationException, CoreException, MisconfigurationException {
        int usedGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = PythonNature.LATEST_GRAMMAR_PY3_VERSION;
        try {

            Document doc = new Document(initial);

            int markerStart = initial.indexOf(stringForRegion);
            int markerLen = stringForRegion.length();

            IMarkerInfoForAnalysis markerInfo = new DummyMarkerInfoForAnalysis(
                    markerType, markerStart, markerLen);
            IAnalysisPreferences analysisPreferences = new AnalysisPreferences(null);
            int offset = markerStart;
            PySelection ps = new PySelection(doc, offset);
            String line = ps.getLine();
            File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC + "extendable/check_analysis_and_tdd.py");
            PydevFileEditorInput editorInputStub = new PydevFileEditorInput(file);
            IPyEdit edit = new PyEditStub(doc, editorInputStub, nature, file);

            TddCodeGenerationQuickFixWithoutMarkersParticipant participant2 = new TddCodeGenerationQuickFixWithoutMarkersParticipant();
            List<ICompletionProposalHandle> props = participant2.getProps(ps, null, file, nature, edit, offset);
            if (markerType != -1) {
                TddQuickFixFromMarkersParticipant participant = new TddQuickFixFromMarkersParticipant();
                participant.addProps(markerInfo, analysisPreferences, line, ps, offset, nature, edit, props);
            }

            ICompletionProposalExtension2 found = findCompletion(props, expectedLabel, true);
            if (found instanceof TddRefactorCompletion) {
                TddRefactorCompletion completion = (TddRefactorCompletion) found;
                TemplateInfo templateInfo = completion.getAsTemplateInfo();
                templateInfo.apply(doc);
                assertEquals(expected, doc.get());
            } else if (found instanceof TddRefactorCompletionInModule) {
                TddRefactorCompletion completion = (TddRefactorCompletion) found;
                TemplateInfo templateInfo = completion.getAsTemplateInfo();
                Document d = new Document();
                templateInfo.apply(d);
                assertEquals(expected, d.get());
            }

        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = usedGrammar;
        }
    }

    private void checkFromAnalysis(String initial, String expected, String expectedLabel)
            throws BadLocationException, CoreException, MisconfigurationException {
        int usedGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = PythonNature.LATEST_GRAMMAR_PY3_VERSION;
        try {

            doc = new Document(initial);
            IMessage[] messages = checkError(1);
            ArrayList<MarkerInfo> markers = AnalysisRunner.generateMarkers(doc, messages, new NullProgressMonitor());
            assertEquals(1, markers.size());
            MarkerInfo marker = markers.get(0);
            marker.getAsMap(); // Updates absoluteStart/absoluteEnd
            int markerStart = marker.absoluteStart;
            int markerLen = marker.absoluteEnd - marker.absoluteStart;

            int offset = markerStart;
            PySelection ps = new PySelection(doc, offset);

            int id = (int) marker.additionalInfo.get(IMiscConstants.PYDEV_ANALYSIS_TYPE);
            IMarkerInfoForAnalysis markerInfo = new DummyMarkerInfoForAnalysis(
                    id, markerStart, markerLen);
            IAnalysisPreferences analysisPreferences = new AnalysisPreferences(null);
            String line = ps.getLine();
            File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC + "extendable/check_analysis_and_tdd.py");
            PydevFileEditorInput editorInputStub = new PydevFileEditorInput(file);
            IPyEdit edit = new PyEditStub(doc, editorInputStub, nature, file);

            TddCodeGenerationQuickFixWithoutMarkersParticipant participant2 = new TddCodeGenerationQuickFixWithoutMarkersParticipant();
            List<ICompletionProposalHandle> props = participant2.getProps(ps, null, file, nature, edit, offset);

            TddQuickFixFromMarkersParticipant participant = new TddQuickFixFromMarkersParticipant();
            participant.addProps(markerInfo, analysisPreferences, line, ps, offset, nature, edit, props);

            TddRefactorCompletion completion = (TddRefactorCompletion) findCompletion(props, expectedLabel,
                    true);
            TemplateInfo templateInfo = completion.getAsTemplateInfo();
            templateInfo.apply(doc);
            assertEquals(expected, doc.get());

        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = usedGrammar;
        }
    }

    private ICompletionProposalExtension2 findCompletion(List<ICompletionProposalHandle> props,
            String expectedCompletion, boolean throwException) {
        List<String> buf = new ArrayList<String>(1 + (2 * props.size()));
        buf.add("Available:");
        for (ICompletionProposalHandle iCompletionProposal : props) {
            if (iCompletionProposal.getDisplayString().equals(expectedCompletion)) {
                ICompletionProposalExtension2 p = (ICompletionProposalExtension2) iCompletionProposal;
                return p;
            }
            buf.add("\n");
            buf.add(iCompletionProposal.getDisplayString());
        }
        if (throwException) {
            throw new AssertionError("Could not find completion: " + expectedCompletion +
                    "\n"
                    + StringUtils.join("\n", buf));
        }
        return null;
    }
}
