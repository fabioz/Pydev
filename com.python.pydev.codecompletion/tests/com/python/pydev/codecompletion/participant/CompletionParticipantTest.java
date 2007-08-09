/*
 * License: Common Public License v1.0
 * Created on 25/08/2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.codecompletion.participant;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IToken;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.actions.PySelectionTest;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.ui.AutoImportsPreferencesPage;
import com.python.pydev.codecompletion.ctxinsensitive.CtxParticipant;

public class CompletionParticipantTest extends AdditionalInfoTestsBase {

    public static void main(String[] args) {
    	CompletionParticipantTest test = new CompletionParticipantTest();
    	try {
			test.setUp();
			test.testImportCompletion();
			test.tearDown();
			
			junit.textui.TestRunner.run(CompletionParticipantTest.class);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


    protected void setUp() throws Exception {
        super.setUp();
        codeCompletion = new PyCodeCompletion();
        super.restorePythonPath(false);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testImportCompletion() throws CoreException, BadLocationException {
    	participant = new ImportsCompletionParticipant();
    	
    	//check simple
    	ICompletionProposal[] proposals = requestCompl("unittest", new String[]{"unittest", "unittest - testlib"}); //the unittest module and testlib.unittest
    	
        Document document = new Document("unittest");
        ((CtxInsensitiveImportComplProposal)proposals[0]).apply(document, ' ', 0, 8);
        PySelectionTest.checkStrEquals("import unittest\r\nunittest", document.get());
            
        document = new Document("unittest");
        ((CtxInsensitiveImportComplProposal)proposals[1]).apply(document, ' ', 0, 8);
        PySelectionTest.checkStrEquals("from testlib import unittest\r\nunittest", document.get());

        
        //for imports, the behavior never changes
        AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = true;
        try {
            proposals = requestCompl("_priv3", new String[]{"_priv3 - relative.rel1._priv1._priv2"}); 
            document = new Document("_priv3");
            ((CtxInsensitiveImportComplProposal)proposals[0]).apply(document, ' ', 0, 6);
            PySelectionTest.checkStrEquals("from relative.rel1._priv1._priv2 import _priv3\r\n_priv3", document.get());
        } finally {
            AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;
        }
        
        
        //check on actual file
        requestCompl(new File(TestDependent.TEST_PYSRC_LOC+"/testlib/unittest/guitestcase.py"),"guite", -1, 0, new String[]{});
        
        Import importTok = new Import(new aliasType[]{new aliasType(new NameTok("unittest", NameTok.ImportModule), null)});
        this.imports = new ArrayList<IToken>();
        this.imports.add(new SourceToken(importTok, "unittest", "", "", ""));
        
        requestCompl("import unittest\nunittest", new String[]{}); //none because the import for unittest is already there
        requestCompl("import unittest\nunittes", new String[]{}); //the local import for unittest (won't actually show anything because we're only exercising the participant test) 
        this.imports = null;
    }
    

    public void testImportCompletion2() throws CoreException, BadLocationException {
    	participant = new CtxParticipant();
    	ICompletionProposal[] proposals = requestCompl("xml", -1, -1, new String[]{});
    	assertNotContains("xml - xmlrpclib", proposals);
        
    	requestCompl(new File(TestDependent.TEST_PYSRC_LOC+"/testlib/unittest/guitestcase.py"),"guite", -1, 0, new String[]{});
    	
    	
        //the behavior changes for tokens on modules
        AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = true;
        try {
            proposals = requestCompl("Priv3", new String[]{"Priv3 - relative.rel1._priv1._priv2._priv3"}); 
            Document document = new Document("Priv3");
            ((CtxInsensitiveImportComplProposal)proposals[0]).apply(document, ' ', 0, 5);
            PySelectionTest.checkStrEquals("from relative.rel1 import Priv3\r\nPriv3", document.get());
        } finally {
            AutoImportsPreferencesPage.TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;
        }

    }
    
    
}
