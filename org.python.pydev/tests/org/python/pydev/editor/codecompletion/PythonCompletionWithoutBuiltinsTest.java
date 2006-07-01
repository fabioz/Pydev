/*
 * Created on Mar 8, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.python.pydev.core.IModule;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

/**
 * This tests the 'whole' code completion, passing through all modules.
 * 
 * @author Fabio Zadrozny
 */
public class PythonCompletionWithoutBuiltinsTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        
      try {
          //DEBUG_TESTS_BASE = true;
          PythonCompletionWithoutBuiltinsTest test = new PythonCompletionWithoutBuiltinsTest();
	      test.setUp();
	      test.testCalltips4();
	      test.tearDown();
          System.out.println("Finished");

          junit.textui.TestRunner.run(PythonCompletionWithoutBuiltinsTest.class);
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
        codeCompletion = new PyCodeCompletion();
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
	    
	    requestCompl("from testlib.unittest import  ", getTestLibUnittestTokens());

	    requestCompl("from testlib.unittest.testcase.TestCase import  assertImagesNotE", new String[]{"assertImagesNotEqual"});
	    requestCompl("from testlib.unittest.testcase.TestCase import  assertBM", new String[]{"assertBMPsNotEqual","assertBMPsEqual"});
    }

    /**
     * @return
     */
    private String[] getTestLibUnittestTokens() {
        return new String[]{
          "__init__"
        , "anothertest"
        , "AnotherTest"
        , "GUITest"
        , "guitestcase"
        , "main"
        , "relative"
        , "t"
        , "TestCase"
        , "testcase"
        , "TestCaseAlias"
        };
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
	
	public void testProj2() throws CoreException, BadLocationException{
		String s;
		s = ""+
		"import proj2root\n" +
		"print proj2root.";
		requestCompl(s, s.length(), -1, new String[] { "Proj2Root"}, nature2);
	}
	
	public void testProj2Global() throws CoreException, BadLocationException{
		String s;
		s = ""+
		"import ";
		requestCompl(s, s.length(), -1, new String[] { "proj2root", "testlib"}, nature2);
	}
	
	public void testClassAttrs() throws CoreException, BadLocationException{
		String s;
		s = ""+
		"class A:\n" +
		"    aa, bb, cc = range(3)\n" + //the heuristic to find the attrs (class HeuristicFindAttrs) was not getting this
		"    dd = 1\n" +
		"    def m1(self):\n" +
		"        self.";
		requestCompl(s, s.length(), -1, new String[] { "aa", "bb", "cc", "dd"});
	}

	
	public void testInnerImport() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
        "def m1():\n" +
        "    from testlib import unittest\n" +
        "    unittest.";
	    requestCompl(s, s.length(), -1, new String[]{
            "AnotherTest"
            , "GUITest"
            , "main"
            , "TestCase"
            , "testcase"
            , "TestCaseAlias"
            
            //gotten because unittest is actually an __init__, so, gather others that are in the same level
            , "anothertest"
            , "guitestcase"
            , "testcase"
            });
	}
	
	
	public void testSelfReferenceWithTabs() throws CoreException, BadLocationException{
	    String s;
	    s = "class C:\n" +
	    "    def met1(self):\n" +
	    "        pass\n" +
	    "        \n" +
	    "class B:\n" +
	    "    def met2(self):\n" +
	    "        self.c = C()\n" +
	    "        \n" +
	    "    def met3(self):\n" +
	    "        self.c.";
        s = s.replaceAll("\\ \\ \\ \\ ", "\t");
	    requestCompl(s, s.length(), -1, new String[] { "met1()"});
	}

	
	public void testClassCompl() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "class Test:\n" +
        "    classVar = 1\n"+
	    "    def findIt(self):\n"+
	    "        self.";
	    requestCompl(s, s.length(), -1, new String[] { "classVar"});
	}
	
	public void testInnerCtxt() throws CoreException, BadLocationException{
		String s;
		s = "" +
		"class Test:\n"+
		"    def findIt(self):\n"+
		"        pass\n"+
		"    \n"+
		"def m1():\n"+
		"    s = Test()\n"+
		"    s.";
		requestCompl(s, s.length(), -1, new String[] { "findIt()"});
	}
	
	
	public void testDeepNested() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
    	    "from extendable.nested2 import hub\n"+
    	    "hub.c1.a.";
	    requestCompl(s, s.length(), -1, new String[] { "fun()"});
	}
	
	public void testDeepNested2() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "from extendable.nested2 import hub\n"+
	    "hub.c1.b.";
	    requestCompl(s, s.length(), -1, new String[] { "another()"});
	}
	
	public void testDeepNested3() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "from extendable.nested2 import hub\n"+
	    "hub.c1.c.";
	    requestCompl(s, s.length(), -1, new String[] { "another()"});
	}
	
	public void testDeepNested4() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "from extendable.nested2 import hub\n"+
	    "hub.c1.d.";
	    requestCompl(s, s.length(), -1, new String[] { "AnotherTest"});
	}
	
	public void testDeepNested5() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
	    "from extendable.nested2 import hub\n"+
	    "hub.c1.e.";
	    requestCompl(s, s.length(), -1, new String[] { "assertBMPsNotEqual"});
	}
	
	public void testDeepNested6() throws CoreException, BadLocationException{
		String s;
		s = "" +
		"from extendable.nested2 import mod2\n"+
		"mod2.c1.a.";
		requestCompl(s, s.length(), -1, new String[] { "fun()"});
	}
	
	
	public void testSelfReferenceWithTabs2() throws CoreException, BadLocationException{
	    String s;
	    s = "" +
        "class C:\n" +
        "    def met3(self):\n" +
        "        self.COMPLETE_HERE\n" +
        "                    \n" +
	    "    def met1(self): \n" +
	    "        pass        \n" +
        "";
	    s = s.replaceAll("\\ \\ \\ \\ ", "\t");
        int iComp = s.indexOf("COMPLETE_HERE");
        s = s.replaceAll("COMPLETE_HERE", "");
	    requestCompl(s, iComp, -1, new String[] { "met1()"});
	}
	
	public void testRelativeImport() throws FileNotFoundException, CoreException, BadLocationException{
        String file = TestDependent.TEST_PYSRC_LOC+"testlib/unittest/relative/testrelative.py";
        String strDoc = "from toimport import ";
        requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[]{"Test1", "Test2"});   
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
	
	public void testProperties() throws BadLocationException, IOException, Exception{
		String s;
		s = 
		"class C:\n" +
		"    \n" +
		"    properties.create(test = 0)\n" +
		"    \n" +
		"c = C.";
		requestCompl(s, -1, -1, new String[] { "test"});
	}
	
	public void testImportMultipleFromImport() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "import testlib.unittest.relative\n" +
	    "";
	    requestCompl(s, -1, -1, new String[] { "testlib","testlib.unittest","testlib.unittest.relative"});
    }
	
	public void testImportMultipleFromImport2() throws BadLocationException, IOException, Exception{
	    String s;
	    s = "import testlib.unittest.relative\n" +
	    "testlib.";
	    requestCompl(s, -1, 0, new String[] { });
	}
	
	
	public void testNestedImports() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable import nested\n"+ 
		"print nested.NestedClass.";   
		requestCompl(s, -1, 1, new String[] { "nestedMethod()" });
	}
	
	
	public void testSameName() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable.namecheck import samename\n"+ 
		"print samename.";   
		requestCompl(s, -1, 1, new String[] { "method1()" });
	}
	
	
	public void testSameName2() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable import namecheck\n"+ 
		"print namecheck.samename.";   
		requestCompl(s, -1, 1, new String[] { "method1()" });
	}
	
	public void testCompositeImport() throws BadLocationException, IOException, Exception{
		String s;
		s = "import xml.sax\n"+ 
		"print xml.sax.";   
		requestCompl(s, -1, -1, new String[] { "default_parser_list" });
	}
	
	public void testIsInGlobalTokens() throws BadLocationException, IOException, Exception{
		IModule module = nature.getAstManager().getModule("testAssist.__init__", nature, true);
		assertTrue(module.isInGlobalTokens("assist.ExistingClass.existingMethod", nature));
	}
	
	
	
    public void testGetActTok(){
        String strs[];
        
        strs = PySelection.getActivationTokenAndQual(new Document(""), 0, false);
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0, C).text())"), 42, false);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("self.assertEquals( DECAY_COEF, t.item(0,C).text())"), 41, false);
        assertEquals("" , strs[0]);
        assertEquals("C", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("m = met(self.c, self.b)"), 14, false);
        assertEquals("self." , strs[0]);
        assertEquals("c", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("[a,b].ap"), 8, false);
        assertEquals("list." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("{a:1,b:2}.ap"), 12, false);
        assertEquals("dict." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("''.ap"), 5, false);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("\"\".ap"), 5, false);
        assertEquals("str." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod.ap"), 20, false);
        assertEquals("ClassA.someMethod." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod().ap"), 22, false);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("ClassA.someMethod( a, b ).ap"), 28, false);
        assertEquals("ClassA.someMethod()." , strs[0]);
        assertEquals("ap", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2, false);
        assertEquals("" , strs[0]);
        assertEquals("fo", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2, false);
        assertEquals("" , strs[0]);
        assertEquals("fo", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar"), 2, true);
        assertEquals("" , strs[0]);
        assertEquals("foo", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 2, true);
        assertEquals("" , strs[0]);
        assertEquals("foo", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 5, true); //get the full qualifier
        assertEquals("foo.", strs[0]);
        assertEquals("bar", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 5, false); //get just a part of it
        assertEquals("foo.", strs[0]);
        assertEquals("b", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar   "), 100, true); //out of the league
        assertEquals("", strs[0]);
        assertEquals("", strs[1]);
        
        String importsTipperStr = PyCodeCompletion.getImportsTipperStr(new Document("from coilib.decorators import "), 30);
        assertEquals("coilib.decorators" , importsTipperStr);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar.xxx   "), 9, true); 
        assertEquals("foo.bar.", strs[0]);
        assertEquals("xxx", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("foo.bar.xxx   "), 9, false); 
        assertEquals("foo.bar.", strs[0]);
        assertEquals("x", strs[1]);
        
        strs = PySelection.getActivationTokenAndQual(new Document("m1(a.b)"), 4, false); 
        assertEquals("", strs[0]);
        assertEquals("a", strs[1]);
        
        //Ok, now, the tests for getting the activation token and qualifier for the calltips.
        //We should 'know' that we're just after a parentesis and get the contents before it
        //This means: get the char before the offset (excluding spaces and tabs) and see
        //if it is a ',' or '(' and if it is, go to that offset and do the rest of the process
        //as if we were on that position
        ActivationTokenAndQual act = PySelection.getActivationTokenAndQual(new Document("m1()"), 3, false, true); 
        assertEquals("", act.activationToken);
        assertEquals("m1", act.qualifier);
        assertTrue(act.changedForCalltip);
        
        act = PySelection.getActivationTokenAndQual(new Document("m1.m2()"), 6, false, true); 
        assertEquals("m1.", act.activationToken);
        assertEquals("m2", act.qualifier);
        assertTrue(act.changedForCalltip);
        
        act = PySelection.getActivationTokenAndQual(new Document("m1.m2(  \t)"), 9, false, true); 
        assertEquals("m1.", act.activationToken);
        assertEquals("m2", act.qualifier);
        assertTrue(act.changedForCalltip);
        
        act = PySelection.getActivationTokenAndQual(new Document("m1(a  , \t)"), 9, false, true); 
        assertEquals("", act.activationToken);
        assertEquals("m1", act.qualifier);
        assertTrue(act.changedForCalltip);
        
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
            throw new RuntimeException(e);
        }
    }
    
    public void testCalltips2() throws CoreException, BadLocationException {
        String s;
        s = "" +
        "GLOBAL_VAR = 10\n" + 
        "def m1(a, b):\n" +
        "    print a, b\n" +
        "def m1Other(a, b):\n" + //this one should not show, as we're returning it for calltip purposes only 
        "    print a, b\n" +
        "\n" +
        "m1()"; 
        ICompletionProposal[] proposals = requestCompl(s, s.length()-1, -1, new String[] {});
        assertEquals(1, proposals.length); 
        
    }
    
    public void testCalltips3() throws CoreException, BadLocationException {
        String s;
        s = "" +
        "def m1(a, b):\n" +
        "    print a, b\n" +
        "m1()";  
        PyContextInformationValidator validator = new PyContextInformationValidator();
        int requestOffset = s.length()-1;
        ICompletionProposal[] proposals = requestCompl(s, requestOffset, -1, new String[] {});
        assertEquals(1, proposals.length); 
        PyCalltipsContextInformation contextInformation = (PyCalltipsContextInformation) proposals[0].getContextInformation();
        
        validator.install(contextInformation, new Document(s), requestOffset);
        assertFalse(validator.isContextInformationValid(0));
        assertTrue(validator.isContextInformationValid(requestOffset));
        assertFalse(validator.isContextInformationValid(requestOffset+1));
    }
    
    public void testCalltips4() throws CoreException, BadLocationException {
        String s;
        s = "" +
        "def m1(a, b):\n" +
        "    print a, b\n" +
        "m1(a,b)";  
        int requestOffset = s.length()-4;
        ICompletionProposal[] proposals = requestCompl(s, requestOffset, -1, new String[] {});
        assertEquals(1, proposals.length); 
        PyContextInformationValidator validator = new PyContextInformationValidator();
        PyCalltipsContextInformation contextInformation = (PyCalltipsContextInformation) proposals[0].getContextInformation();
        
        validator.install(contextInformation, new Document(s), requestOffset);
        assertFalse(validator.isContextInformationValid(requestOffset-1));
        assertTrue(validator.isContextInformationValid(requestOffset));
        assertTrue(validator.isContextInformationValid(requestOffset+3));
        assertFalse(validator.isContextInformationValid(requestOffset+4));
    }
    
    public void testCalltips1() throws CoreException, BadLocationException {
    	String s;
    	s = "" +
    	"GLOBAL_VAR = 10\n" + //this variable should not show in the return
    	"def m1(a, b):\n" +
    	"    print a, b\n" +
    	"\n" +
    	"m1()"; //we'll request a completion inside the parentesis to check for calltips. For calltips, we
    	        //should get the activation token as an empty string and the qualifier as "m1", 
    			//so, the completion that should return is "m1(a, b)", with the information context
    			//as "a, b". 
    			//
    			//
    			//
    			//The process of getting the completions actually starts at:
    			//org.python.pydev.editor.codecompletion.PyCodeCompletion#getCodeCompletionProposals
    	
    	ICompletionProposal[] proposals = requestCompl(s, s.length()-1, -1, new String[] {});
    	
    	
    	if(false){ //make true to see which proposals were returned.
	    	for (ICompletionProposal proposal : proposals) {
				System.out.println(proposal.getDisplayString());
			}
    	}
    	
    	assertEquals(1, proposals.length); //now, here's the first part of the failing test: we can only have one
    	    							   //returned proposal: m1(a, b)
    	
        //check if the returned proposal is there
    	ICompletionProposal prop = proposals[0];
    	assertEquals("m1(a, b)", prop.getDisplayString());
        ICompletionProposalExtension4 p4 = (ICompletionProposalExtension4) prop;
        assertTrue(p4.isAutoInsertable());
    	
        //the display string for the context 'context' and 'information' should be the same
    	PyCalltipsContextInformation contextInformation = (PyCalltipsContextInformation) prop.getContextInformation();
    	assertEquals("a, b", contextInformation.getContextDisplayString());
    	assertEquals("a, b", contextInformation.getInformationDisplayString());
    	
    	//now, this proposal has one interesting thing about it: it should actually not change the document
    	//where it is applied (it is there just to show the calltip). 
    	//
    	//To implement that, when we see that it is called inside some parenthesis, we should create a subclass of 
    	//PyCompletionProposal that will have its apply method overriden, so that nothing happens here (the calltips will
    	//still be shown)
    	Document doc = new Document();
		prop.apply(doc);
		assertEquals("", doc.get());
	}
    
    

}
