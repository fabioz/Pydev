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
//	      test.testSelfReference();
//	      test.testCompleteImportCompletion();
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
        
        
        for (int i = 0; i < retCompl.length; i++) {
            assertContains(retCompl[i], codeCompletionProposals);
        }

        if(returned > -1){
	        StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);
            assertEquals("Expected "+returned+" received: "+codeCompletionProposals.length+"\n"+buffer, returned, codeCompletionProposals.length);
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
        StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);
        
        fail("The string "+string+" was not found in the returned completions.\nAvailable:\n"+buffer);
    }

    /**
     * @param codeCompletionProposals
     * @return
     */
    private StringBuffer getAvailableAsStr(ICompletionProposal[] codeCompletionProposals) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < codeCompletionProposals.length; i++) {
            buffer.append(codeCompletionProposals[i].getDisplayString());
            buffer.append("\n");
        }
        return buffer;
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
	    
	    requestCompl("from testlib.unittest import  ", 
	            new String[]{
	              "__init__"
	            , "anothertest"
	            , "AnotherTest"
	            , "GUITest"
	            , "guitestcase"
	            , "main"
	            , "relative"
	            , "TestCase"
	            , "testcase"
	            , "TestCaseAlias"
	            });

	    requestCompl("from testlib.unittest.testcase.TestCase import  assertImagesNotE", new String[]{"assertImagesNotEqual"});
	    requestCompl("from testlib.unittest.testcase.TestCase import  assertBM", new String[]{"assertBMPsNotEqual","assertBMPsEqual"});
    }

	
	public void testSelfReference() throws CoreException, BadLocationException{
        String s;
        s = "class C:            \n" +
			"    def met1(self): \n" +
			"        pass        \n" +
			"                    \n" +
			"class B:            \n" +
			"    def met2(self): \n" +
			"        self.c = C()\n" +
			"                    \n" +
			"    def met3(self): \n" +
			"        self.c.";
        requestCompl(s, s.length(), -1, new String[] { "met1()"});
	}

	public void testCompleteImportBuiltin() throws BadLocationException, IOException, Exception{
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
    	    requestCompl(s, s.indexOf('#'), 0, new String[]{});


    	    //check for builtins..1
    	    s = "" +
    	    "\n" +
    	    "";
    	    requestCompl(s, s.length(), -1, new String[]{"RuntimeError"});

    	    //check for builtins..2
    	    s = "" +
    	    "from testlib import *\n" +
    	    "\n" +
    	    "";
    	    requestCompl(s, s.length(), -1, new String[]{"RuntimeError"});

    	    //check for builtins..3 (builtins should not be available because it is an import request for completions)
    	    requestCompl("from testlib.unittest import  ", new String[]{"__init__", "anothertest"
    	            , "AnotherTest", "GUITest", "guitestcase", "main", "relative", "TestCase", "testcase", "TestCaseAlias"
    	            });

        } finally {
            shell.endIt();
        }
	}

	
	
	

	public void testCompleteImportBuiltinReference() throws BadLocationException, IOException, Exception{
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPathWithSitePackages(true);
        codeCompletion = new PyCodeCompletion(false);

        PythonShell shell = PythonShellTest.startShell();
        PythonShell.putServerShell(PythonShell.COMPLETION_SHELL, shell);
        
        try {
            String s;

    	    //check for builtins with reference..3
    	    s = "" +
			"from qt import *\n"+
			"                \n"+   
			"q = QLabel()    \n"+     
			"q.";         
    	    requestCompl(s, s.length(), -1, new String[]{"AlignAuto"});

    	    //check for builtins with reference..3
    	    s = "" +
			"from testlib.unittest import anothertest\n"+
			"anothertest.";         
    	    requestCompl(s, s.length(), 2, new String[]{"AnotherTest","testcase"});

        
        } finally {
            shell.endIt();
        }
	}
	

	
    public void testGetActTok(){
        String strs[];
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document(""), 0);
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0, C).text())"), 42);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0,C).text())"), 41);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("m = met(self.c, self.b)"), 14);
        assertEquals("self." , strs[0]);
        assertEquals("c", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("[a,b].ap"), 8);
        assertEquals("list." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("{a:1,b:2}.ap"), 12);
        assertEquals("dict." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("''.ap"), 5);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("\"\".ap"), 5);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod.ap"), 20);
        assertEquals("ClassA.someMethod." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod().ap"), 22);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PyCodeCompletion.getActivationTokenAndQual(new Document("ClassA.someMethod( a, b ).ap"), 28);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        String importsTipperStr = PyCodeCompletion.getImportsTipperStr(new Document("from coilib.decorators import "), 30);
        assertEquals("coilib.decorators" , importsTipperStr);
        
        
    }


}
