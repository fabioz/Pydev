/*
 * Created on Mar 8, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

/**
 * This tests the 'whole' code completion, passing through all modules.
 * 
 * @author Fabio Zadrozny
 */
public class PythonCompletionTestWithoutBuiltins extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        
      try {
//          PythonCompletionProcessorTest test = new PythonCompletionProcessorTest();
//	      test.setUp();
//          test.testCompleteImportBuiltinReference();
//	      test.tearDown();
//          System.out.println("Finished");

          junit.textui.TestRunner.run(PythonCompletionTestWithoutBuiltins.class);
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


	public void testWildImportRecursive() throws BadLocationException, IOException, Exception{
        String s;
        s = "from testrecwild import *\n" +
            "";
        requestCompl(s, -1, -1, new String[] { "Class1"});
	}
	
	public void testWildImportRecursive2() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "from testrecwild2 import *\n" +
	    "";
	    requestCompl(s, -1, -1, new String[] { "Class2"});
	}
	
	public void testWildImportRecursive3() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "from testrec2 import *\n" +
	    "";
	    requestCompl(s, -1, -1, new String[] { "Leaf"});
	}
	
	public void testImportMultipleFromImport() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "import testlib.unittest.relative\n" +
	    "";
	    requestCompl(s, -1, -1, new String[] { "testlib","testlib.unittest","testlib.unittest.relative"});
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

    /**
     * @throws BadLocationException
     * @throws CoreException
     * 
     */
    public void testFor() throws CoreException, BadLocationException {
        String s;
        s = "" +
    		"for event in a:   \n" +
    		"    print event   \n" +
    		"                  \n" +
			"event.";
        try {
            requestCompl(s, s.length(), -1, new String[] {});
        } catch (StackOverflowError e) {
            System.out.println("here");
            throw new RuntimeException(e);
        }
    }

}
