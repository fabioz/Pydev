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
import java.util.ArrayList;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IToken;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.actions.PySelectionTest;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.shared_core.callbacks.ICallback;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.ui.AutoImportsPreferencesPage;
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
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        PyCodeCompletionPreferencesPage.getPreferencesForTests = null;
    }

    @Override
    protected String getSystemPythonpathPaths() {
        return TestDependent.GetCompletePythonLib(true) + "|" + TestDependent.TEST_PYSRC_LOC + "myzipmodule.zip" + "|"
                + TestDependent.TEST_PYSRC_LOC + "myeggmodule.egg";
    }

    public void testImportCompletion() throws Exception {
        participant = new ImportsCompletionParticipant();

        //check simple
        ICompletionProposal[] proposals = requestCompl(
                "unittest", -1, -1, new String[] { "unittest", "unittest - testlib" }
                ); //the unittest module and testlib.unittest

        Document document = new Document("unittest");
        ICompletionProposal p0 = null;
        ICompletionProposal p1 = null;
        for (ICompletionProposal p : proposals) {
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
        final Preferences prefs = new Preferences();
        PyCodeCompletionPreferencesPage.getPreferencesForTests = new ICallback<Preferences, Object>() {

            public Preferences call(Object arg) {
                return prefs;
            }
        };

        document = new Document("unittest");
        prefs.setValue(PyCodeCompletionPreferencesPage.APPLY_COMPLETION_ON_DOT, false);
        ((CtxInsensitiveImportComplProposal) p1).indentString = "    ";
        ((CtxInsensitiveImportComplProposal) p1).apply(document, '.', 0, 8);
        PySelectionTest.checkStrEquals("unittest.", document.get());

        document = new Document("unittest");
        prefs.setValue(PyCodeCompletionPreferencesPage.APPLY_COMPLETION_ON_DOT, true);
        ((CtxInsensitiveImportComplProposal) p1).indentString = "    ";
        ((CtxInsensitiveImportComplProposal) p1).apply(document, '.', 0, 8);
        PySelectionTest.checkStrEquals("from testlib import unittest\r\nunittest.", document.get());

        //for imports, the behavior never changes
        AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = true;
        try {
            proposals = requestCompl("_priv3", new String[] { "_priv3 - relative.rel1._priv1._priv2" });
            document = new Document("_priv3");
            ((CtxInsensitiveImportComplProposal) proposals[0]).indentString = "    ";
            ((CtxInsensitiveImportComplProposal) proposals[0]).apply(document, ' ', 0, 6);
            PySelectionTest.checkStrEquals("from relative.rel1._priv1._priv2 import _priv3\r\n_priv3", document.get());
        } finally {
            AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;
        }

        //check on actual file
        requestCompl(new File(TestDependent.TEST_PYSRC_LOC + "/testlib/unittest/guitestcase.py"), "guite", -1, 0,
                new String[] {});

        Import importTok = new Import(new aliasType[] { new aliasType(new NameTok("unittest", NameTok.ImportModule),
                null) });
        this.imports = new ArrayList<IToken>();
        this.imports.add(new SourceToken(importTok, "unittest", "", "", ""));

        requestCompl("import unittest\nunittest", new String[] {}); //none because the import for unittest is already there
        requestCompl("import unittest\nunittes", new String[] {}); //the local import for unittest (won't actually show anything because we're only exercising the participant test) 
        this.imports = null;
    }

    public void testImportCompletionFromZip2() throws Exception {
        participant = new ImportsCompletionParticipant();
        ICompletionProposal[] proposals = requestCompl("myzip", -1, -1, new String[] {});
        assertContains("myzipfile - myzipmodule", proposals);
        assertContains("myzipmodule", proposals);

        proposals = requestCompl("myegg", -1, -1, new String[] {});
        assertContains("myeggfile - myeggmodule", proposals);
        assertContains("myeggmodule", proposals);
    }

    public void testImportCompletionFromZip() throws Exception {
        participant = new CtxParticipant();
        ICompletionProposal[] proposals = requestCompl("myzipc", -1, -1, new String[] {});
        assertContains("MyZipClass - myzipmodule.myzipfile", proposals);

        proposals = requestCompl("myegg", -1, -1, new String[] {});
        assertContains("MyEggClass - myeggmodule.myeggfile", proposals);
    }

    public void testImportCompletion2() throws Exception {
        participant = new CtxParticipant();
        ICompletionProposal[] proposals = requestCompl("xml", -1, -1, new String[] {});
        assertNotContains("xml - xmlrpclib", proposals);

        requestCompl(new File(TestDependent.TEST_PYSRC_LOC + "/testlib/unittest/guitestcase.py"), "guite", -1, 0,
                new String[] {});

        //the behavior changes for tokens on modules
        AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = true;
        try {
            proposals = requestCompl("Priv3", new String[] { "Priv3 - relative.rel1._priv1._priv2._priv3" });
            Document document = new Document("Priv3");
            ((CtxInsensitiveImportComplProposal) proposals[0]).indentString = "    ";
            ((CtxInsensitiveImportComplProposal) proposals[0]).apply(document, ' ', 0, 5);
            PySelectionTest.checkStrEquals("from relative.rel1 import Priv3\r\nPriv3", document.get());
        } finally {
            AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;
        }

    }

}
