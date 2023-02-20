/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 25/08/2005
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.codecompletion.participant;

import java.io.File;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.PyCodeCompletion;
import org.python.pydev.ast.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.core.IToken;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.actions.PySelectionTest;
import org.python.pydev.editor.codecompletion.proposals.CtxInsensitiveImportComplProposal;
import org.python.pydev.editor.codecompletion.proposals.DefaultCompletionProposalFactory;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.preferences.InMemoryEclipsePreferences;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.codecompletion.ctxinsensitive.CtxParticipant;

public class CompletionParticipantTest extends AdditionalInfoTestsBase {

    public static void main(String[] args) {
        CompletionParticipantTest test = new CompletionParticipantTest();
        try {
            test.setUp();
            test.testImportCompletionFromZip();
            test.tearDown();

            junit.textui.TestRunner.run(CompletionParticipantTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        //        forceAdditionalInfoRecreation = true; -- just for testing purposes
        super.setUp();
        codeCompletion = new PyCodeCompletion();
        CompletionProposalFactory.set(new DefaultCompletionProposalFactory());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        PyCodeCompletionPreferences.getPreferencesForTests = null;
        CompletionProposalFactory.set(null);
    }

    @Override
    protected String getSystemPythonpathPaths() {
        return TestDependent.getCompletePythonLib(true, isPython3Test()) + "|" + TestDependent.TEST_PYSRC_TESTING_LOC
                + "myzipmodule.zip"
                + "|"
                + TestDependent.TEST_PYSRC_TESTING_LOC + "myeggmodule.egg";
    }

    public void testImportCompletion() throws Exception {
        participant = new ImportsCompletionParticipant();

        //check simple
        ICompletionProposalHandle[] proposals = requestCompl(
                "unittest", -1, -1, new String[] { "unittest", "unittest - testlib" }); //the unittest module and testlib.unittest

        Document document = new Document("unittest");
        ICompletionProposalHandle p0 = null;
        ICompletionProposalHandle p1 = null;
        for (ICompletionProposalHandle p : proposals) {
            String displayString = p.getDisplayString();
            if (displayString.equals("unittest")) {
                p0 = p;
            } else if (displayString.equals("unittest - testlib")) {
                p1 = p;
            }
        }

        if (p0 == null) {
            fail("Could not find unittest import");
        }
        if (p1 == null) {
            fail("Could not find unittest - testlib import");
        }
        ((CtxInsensitiveImportComplProposal) p0).indentString = "    ";
        ((CtxInsensitiveImportComplProposal) p0).apply(document, ' ', 0, 8);
        PySelectionTest.checkStrEquals("import unittest\r\nunittest", document.get());

        document = new Document("unittest");
        ((CtxInsensitiveImportComplProposal) p1).indentString = "    ";
        ((CtxInsensitiveImportComplProposal) p1).apply(document, ' ', 0, 8);
        PySelectionTest.checkStrEquals("from testlib import unittest\r\nunittest", document.get());

        document = new Document("unittest");
        final IEclipsePreferences prefs = new InMemoryEclipsePreferences();
        PyCodeCompletionPreferences.getPreferencesForTests = () -> prefs;
        document = new Document("unittest");
        prefs.putBoolean(PyCodeCompletionPreferences.APPLY_COMPLETION_ON_DOT, false);
        ((CtxInsensitiveImportComplProposal) p1).indentString = "    ";
        ((CtxInsensitiveImportComplProposal) p1).apply(document, '.', 0, 8);
        PySelectionTest.checkStrEquals("unittest.", document.get());

        document = new Document("unittest");
        prefs.putBoolean(PyCodeCompletionPreferences.APPLY_COMPLETION_ON_DOT, true);
        ((CtxInsensitiveImportComplProposal) p1).indentString = "    ";
        ((CtxInsensitiveImportComplProposal) p1).apply(document, '.', 0, 8);
        PySelectionTest.checkStrEquals("from testlib import unittest\r\nunittest.", document.get());

        //for imports, the behavior never changes
        AnalysisPreferences.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = true;
        try {
            proposals = requestCompl("_priv3", new String[] { "_priv3 - relative.rel1._priv1._priv2" });
            document = new Document("_priv3");
            ((CtxInsensitiveImportComplProposal) proposals[0]).indentString = "    ";
            ((CtxInsensitiveImportComplProposal) proposals[0]).apply(document, ' ', 0, 6);
            PySelectionTest.checkStrEquals("from relative.rel1._priv1._priv2 import _priv3\r\n_priv3", document.get());
        } finally {
            AnalysisPreferences.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;
        }

        //check on actual file
        requestCompl(new File(TestDependent.TEST_PYSRC_TESTING_LOC + "/testlib/unittest/guitestcase.py"), "guite", -1,
                0,
                new String[] {});

        Import importTok = new Import(new aliasType[] { new aliasType(new NameTok("unittest", NameTok.ImportModule),
                null) });
        this.imports = new TokensList(new IToken[] { new SourceToken(importTok, "unittest", "", "", "", null, null) });

        requestCompl("import unittest\nunittest", new String[] {}); //none because the import for unittest is already there
        requestCompl("import unittest\nunittes", new String[] {}); //the local import for unittest (won't actually show anything because we're only exercising the participant test)
        this.imports = null;
    }

    public void testImportCompletionFromZip2() throws Exception {
        participant = new ImportsCompletionParticipant();
        ICompletionProposalHandle[] proposals = requestCompl("myzip", -1, -1, new String[] {});
        assertContains("myzipfile - myzipmodule", proposals);
        assertContains("myzipmodule", proposals);

        proposals = requestCompl("myegg", -1, -1, new String[] {});
        assertContains("myeggfile - myeggmodule", proposals);
        assertContains("myeggmodule", proposals);
    }

    public void testImportCompletionFromZip() throws Exception {
        participant = new CtxParticipant();
        ICompletionProposalHandle[] proposals = requestCompl("myzipc", -1, -1, new String[] {});
        assertContains("MyZipClass - myzipmodule.myzipfile", proposals);

        proposals = requestCompl("myegg", -1, -1, new String[] {});
        assertContains("MyEggClass - myeggmodule.myeggfile", proposals);
    }

    public void testImportCompletion2() throws Exception {
        participant = new CtxParticipant();
        ICompletionProposalHandle[] proposals = requestCompl("xml", -1, -1, new String[] {});
        assertNotContains("xml - xmlrpclib", proposals);

        requestCompl(new File(TestDependent.TEST_PYSRC_TESTING_LOC + "/testlib/unittest/guitestcase.py"), "guite", -1,
                0,
                new String[] {});

        //the behavior changes for tokens on modules
        AnalysisPreferences.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = true;
        try {
            proposals = requestCompl("Priv3", new String[] { "Priv3 - relative.rel1._priv1._priv2._priv3" });
            Document document = new Document("Priv3");
            ((CtxInsensitiveImportComplProposal) proposals[0]).indentString = "    ";
            ((CtxInsensitiveImportComplProposal) proposals[0]).apply(document, ' ', 0, 5);
            PySelectionTest.checkStrEquals("from relative.rel1 import Priv3\r\nPriv3", document.get());
        } finally {
            AnalysisPreferences.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;
        }

    }

}
