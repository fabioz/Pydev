/*
 * Created on Mar 8, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

/**
 * This tests the 'whole' code completion, passing through all modules.
 * 
 * @author Fabio Zadrozny
 */
public class PythonCompletionProcessorTest extends CodeCompletionTestsBase {

    private PyCodeCompletion codeCompletion;

    public static void main(String[] args) {
//        junit.textui.TestRunner.run(PythonCompletionProcessorTest.class);
      try {
          PythonCompletionProcessorTest test = new PythonCompletionProcessorTest();
	      test.setUp();
	      test.testCompleteImportBuiltin();
	      test.tearDown();
	  } catch (Exception e) {
	      e.printStackTrace();
	  } catch(Error e){
	      e.printStackTrace();
	  }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion(false);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }


    
    public void requestCompl(String strDoc, int documentOffset, int returned, String []retCompl) throws CoreException, BadLocationException{
        if(documentOffset == -1)
            documentOffset = strDoc.length();
        
        IDocument doc = new Document(strDoc);
        CompletionRequest request = new CompletionRequest(null, 
                nature, doc, documentOffset,
                codeCompletion);

        List props = codeCompletion.getCodeCompletionProposals(request);
        ICompletionProposal[] codeCompletionProposals = codeCompletion.onlyValidSorted(props, request.qualifier);
        
        if(returned > -1)
            assertEquals(returned, codeCompletionProposals.length);
        
        for (int i = 0; i < retCompl.length; i++) {
            assertContains(retCompl[i], codeCompletionProposals);
        }
    }
    
    /**
     * @param string
     * @param codeCompletionProposals
     */
    private void assertContains(String string, ICompletionProposal[] codeCompletionProposals) {
        for (int i = 0; i < codeCompletionProposals.length; i++) {
            if(codeCompletionProposals[i].getDisplayString().equals(string)){
                return ;
            }
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < codeCompletionProposals.length; i++) {
            buffer.append(codeCompletionProposals[i].getDisplayString());
            buffer.append("\n");
        }
        
        fail("The string "+string+" was not found in the returned completions.\nAvailable:\n"+buffer);
    }

    public void requestCompl(String strDoc, String []retCompl) throws CoreException, BadLocationException{
        requestCompl(strDoc, -1, retCompl.length, retCompl);
    }
    
    public void requestCompl(String strDoc, String retCompl) throws CoreException, BadLocationException{
        requestCompl(strDoc, new String[]{retCompl});
    }
    
	public void testCompleteImportCompletion() throws CoreException, BadLocationException{
	    requestCompl("import testl"                        , "testlib");
	    requestCompl("from testl"                          , "testlib");
	    requestCompl("from testlib import "                , new String[]{"__init__", "unittest"});
	    requestCompl("from testlib import unittest, __in"  , new String[]{"__init__"});
	    requestCompl("from testlib import unittest,__in"   , new String[]{"__init__"});
	    requestCompl("from testlib import unittest ,__in"  , new String[]{"__init__"});
	    requestCompl("from testlib import unittest , __in" , new String[]{"__init__"});
	    requestCompl("from testlib import unittest , "     , new String[]{"__init__", "unittest"});
	    
	    String s = "from testlib.unittest import  ";
	    requestCompl(s, s.length(), -1, new String[]{"anothertest", "guitestcase", "testcase", "__init__"});
    }
    
	/**
	 * Problem here is that we do not evaluate correctly if
	 * met( ddd,
	 * 		fff,
	 * 		ccc )
	 * 
	 * @throws CoreException
	 * @throws BadLocationException
	 */
	public void testCompleteCompletion() throws CoreException, BadLocationException{
    }

	public void testCompleteImportBuiltin() throws BadLocationException, IOException, CoreException{
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(true);
        codeCompletion = new PyCodeCompletion(false);

        PythonShell shell = PythonShellTest.startShell();
        PythonShell.putServerShell(PythonShell.COMPLETION_SHELL, shell);
        
        try {
            String s;
            s = "from datetime import datetime, date, MINYEAR,";
            requestCompl(s, s.length(), -1, new String[] { "date", "datetime", "MINYEAR", "MAXYEAR", "timedelta" });
            
            s = "from datetime.datetime import ";
            requestCompl(s, s.length(), -1, new String[] { "today", "now", "utcnow" });

        
        
			// Problem here is that we do not evaluate correctly if
			// met( ddd,
			// 		fff,
			// 		ccc )
            //so, for now the test just checks that we do not get in any sort of
            //look... 
    	    s = "" +
    		
    	    "class bla(object):pass\n" +
    	    "\n"+
    		"def newFunc(): \n"+
    		"    callSomething( bla.__get#complete here... stack error \n"+
    		"                  keepGoing) \n";

    	    //If we improve the parser to get the error above, uncomment line below to check it...
    	    //requestCompl(s, s.indexOf('#'), 1, new String[]{"__getattribute__"});
    	    requestCompl(s, s.indexOf('#'), 0, new String[]{""});

        
        } finally {
            shell.endIt();
        }
	}
	
	
    public void testGetActTok(){
        String strs[];
        
        strs = codeCompletion.getActivationTokenAndQual(new Document(""), 0);
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0, C).text())"), 42);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0,C).text())"), 41);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("m = met(self.c, self.b)"), 14);
        assertEquals("self." , strs[0]);
        assertEquals("c", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("[a,b].ap"), 8);
        assertEquals("list." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("{a:1,b:2}.ap"), 12);
        assertEquals("dict." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("''.ap"), 5);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("\"\".ap"), 5);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod.ap"), 20);
        assertEquals("ClassA.someMethod." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod().ap"), 22);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = codeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod( a, b ).ap"), 28);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        
    }


}
