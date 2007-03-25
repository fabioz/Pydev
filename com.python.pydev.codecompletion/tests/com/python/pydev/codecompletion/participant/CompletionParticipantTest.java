/*
 * License: Common Public License v1.0
 * Created on 25/08/2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.codecompletion.participant;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;

import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.codecompletion.ctxinsensitive.CtxParticipant;

public class CompletionParticipantTest extends AdditionalInfoTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CompletionParticipantTest.class);
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
        requestCompl("unittest", new String[]{"unittest", "unittest - testlib"});
        
        requestCompl(new File(TestDependent.TEST_PYSRC_LOC+"/testlib/unittest/guitestcase.py"),"guite", -1, 0, new String[]{});
    }

    public void testImportCompletion2() throws CoreException, BadLocationException {
    	participant = new CtxParticipant();
    	ICompletionProposal[] proposals = requestCompl("xml", -1, -1, new String[]{});
    	assertNotContains("xml - xmlrpclib", proposals);
        
    	requestCompl(new File(TestDependent.TEST_PYSRC_LOC+"/testlib/unittest/guitestcase.py"),"guite", -1, 0, new String[]{});
    }
    
    
}
