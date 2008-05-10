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
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IModule;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.ImportsSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
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
	      test.testRelativeImportWithSubclass();
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
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(TestDependent.GetCompletePythonLib(true)+"|"+TestDependent.PYTHON_PIL_PACKAGES, false);
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>(){

			public Object call(CompletionRecursionException e) {
				throw new RuntimeException("Recursion error:"+Log.getExceptionStr(e));
			}
        	
        };
    }

    /*
     * @see TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
        PyCodeCompletion.onCompletionRecursionException = null;
    }


    
	public void testCompleteImportCompletion() throws CoreException, BadLocationException{
	    requestCompl("from testl"                          , "testlib");
	    requestCompl("import testl"                        , "testlib");
	    requestCompl("from testlib import "                , new String[]{"__init__", "unittest", "__path__"});
	    requestCompl("from testlib import unittest, __in"  , new String[]{"__init__"});
	    requestCompl("from testlib import unittest,__in"   , new String[]{"__init__"});
	    requestCompl("from testlib import unittest ,__in"  , new String[]{"__init__"});
	    requestCompl("from testlib import unittest , __in" , new String[]{"__init__"});
	    requestCompl("from testlib import unittest , "     , new String[]{"__init__", "unittest", "__path__"});
	    
	    requestCompl("from testlib.unittest import  ", getTestLibUnittestTokens());

	    requestCompl("from testlib.unittest.testcase.TestCase import  assertImagesNotE", new String[]{"assertImagesNotEqual"});
	    requestCompl("from testlib.unittest.testcase.TestCase import  assertBM", new String[]{"assertBMPsNotEqual","assertBMPsEqual"});
    }
	
	/**
	 * This test checks the code-completion for adaptation and factory methods, provided that the 
	 * class expected is passed as one of the parameters.
	 * 
	 * This is done in AssignAnalysis
	 */
	public void testProtocolsAdaptation() throws CoreException, BadLocationException{
	    String s = 
	        "import protocols\n" +
	        "class InterfM1(protocols.Interface):\n" +
	        "    def m1(self):\n" +
	        "        pass\n" +
	        " \n" +
	        "class Bar(object):\n" +
	        "    protocols.advise(instancesProvide=[InterfM1])\n" +
	        "if __name__ == '__main__':\n" +
	        "    a = protocols.adapt(Bar(), InterfM1)\n" +
	        "    a.";
	    
	    requestCompl(s, s.length(), -1, new String[]{"m1()"});
	}
    
	/**
	 * Check if some assert for an instance is enough to get the type of some variable. This should
	 * be configurable so that the user can do something as assert IsInterfaceDeclared(obj, Class) or 
	 * AssertImplements(obj, Class), with the assert or not, providing some way for the user to configure that.
	 * 
	 * This is done in ILocalScope#getPossibleClassesForActivationToken
	 */
	public void testAssertDeterminesClass() throws CoreException, BadLocationException{
	    String s = 
            "def m1(a):\n" +
            "    import xmllib\n" +
            "    assert isinstance(a, xmllib.XMLParser)\n" +
            "    a.";
        
        requestCompl(s, s.length(), -1, new String[]{"handle_data(data)"});
        
    }
	
	public void testAssertDeterminesClass2() throws CoreException, BadLocationException{
	    String s = 
	        "def m1(a):\n" +
	        "    import xmllib\n" +
	        "    assert isinstance(a.bar, xmllib.XMLParser)\n" +
	        "    a.bar.";
	    
	    requestCompl(s, s.length(), -1, new String[]{"handle_data(data)"});
	    
	}
	
	public void testAssertDeterminesClass3() throws CoreException, BadLocationException{
	    String s = 
	        "class InterfM1:\n" +
	        "    def m1(self):\n" +
	        "        pass\n" +
	        "\n" +
	        "" +
	        "def m1(a):\n" +
	        "    assert isinstance(a, InterfM1)\n" +
	        "    a.";
	    
	    requestCompl(s, s.length(), -1, new String[]{"m1()"});
	    
	}
	
	public void testAssertDeterminesClass4() throws CoreException, BadLocationException{
	    String s = 
	        "class InterfM1:\n" +
	        "    def m1(self):\n" +
	        "        pass\n" +
	        "\n" +
	        "class InterfM2:\n" +
	        "    def m2(self):\n" +
	        "        pass\n" +
	        "\n" +
	        "" +
	        "def m1(a):\n" +
	        "    assert isinstance(a, (InterfM1, InterfM2))\n" +
	        "    a.";
	    
	    requestCompl(s, s.length(), -1, new String[]{"m1()", "m2()"});
	    
	}
	
	public void testAssertDeterminesClass5() throws CoreException, BadLocationException{
	    String s = 
	        "class InterfM1:\n" +
	        "    def m1(self):\n" +
	        "        pass\n" +
	        "\n" +
	        "" +
	        "def m1(a):\n" +
	        "    assert InterfM1.implementedBy(a)\n" +
	        "    a.";
	    
	    requestCompl(s, s.length(), -1, new String[]{"m1()"});
	    
	}
	public void testAssertDeterminesClass6() throws CoreException, BadLocationException{
	    String s = 
	        "class InterfM1:\n" +
	        "    def m1(self):\n" +
	        "        pass\n" +
	        "\n" +
	        "" +
	        "def m1(a):\n" +
	        "    assert InterfM1.implementedBy()\n" + //should give no error
	        "    a.";
	    
	    requestCompl(s, s.length(), -1, new String[]{});
	    
	}
	
	public void testMultilineImportCompletion() throws CoreException, BadLocationException{
	    String s = "from testlib import (\n";
	    
	    requestCompl(s, new String[]{"__init__", "unittest", "__path__"});
	    
	}

    /**
     * @return
     */
    public String[] getTestLibUnittestTokens() {
        return new String[]{
          "__init__"
        , "__path__"
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
	
	public void testPIL() throws CoreException, BadLocationException{
		if(TestDependent.HAS_PIL){
			String s;
			s = ""+
			"import Image\n" +
			"Image." +
			"";
			requestCompl(s, s.length(), -1, new String[] { "RASTERIZE"});
		}
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

	public void testFromImport() throws CoreException, BadLocationException{
	    //TODO: see AbstractASTManager.resolveImport
		String s;
		s = ""+
		"from testOtherImports.f3 import test\n" +
		"tes";
		ICompletionProposal[] p = requestCompl(s, s.length(), -1, new String[] { "test(a, b, c)"}, nature);
		assertEquals(p[0].getAdditionalProposalInfo(), "This is a docstring");
	}
	
	public void testFromImportAs() throws CoreException, BadLocationException{
		String s;
		s = ""+
		"from testOtherImports.f3 import test as AnotherTest\n" +
		"t = AnotherTes";
		ICompletionProposal[] p = requestCompl(s, s.length(), -1, new String[] { "AnotherTest(a, b, c)"}, nature);
		assertEquals("This is a docstring", p[0].getAdditionalProposalInfo());
	}
	
	
	public void testFromImportAs2() throws CoreException, BadLocationException{
	    String s;
	    s = ""+
	    "from testOtherImports.f3 import Foo\n" +
	    "t = Fo";
	    ICompletionProposal[] p = requestCompl(s, s.length(), -1, new String[] { "Foo(a, b)"}, nature);
	    assertEquals("SomeOtherTest", p[0].getAdditionalProposalInfo());
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

	public void testInModuleWithoutExtension() throws FileNotFoundException, CoreException, BadLocationException{
	    String file = TestDependent.TEST_PYSRC_LOC+"mod_without_extension";
	    String strDoc = REF.getFileContents(new File(file));
	    requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[]{"ClassInModWithoutExtension"});   
	}
	
	public void testRelativeImportWithSubclass() throws FileNotFoundException, CoreException, BadLocationException{
	    String file = TestDependent.TEST_PYSRC_LOC+"extendable/relative_with_sub/bb.py";
	    String strDoc = REF.getFileContents(new File(file));
	    requestCompl(new File(file), strDoc, strDoc.length(), -1, new String[]{"yyy()"});   
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
	    requestCompl(s, -1, -1, new String[] {"__path__"});
	}
	
	
	public void testNestedImports() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable import nested\n"+ 
		"print nested.NestedClass.";   
		requestCompl(s, -1, 1, new String[] { "nestedMethod(self)" });
	}
	
	
	public void testSameName() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable.namecheck import samename\n"+ 
		"print samename.";   
		requestCompl(s, -1, 1, new String[] { "method1(self)" });
	}
	
	
	public void testSameName2() throws BadLocationException, IOException, Exception{
		String s;
		s = "from extendable import namecheck\n"+ 
		"print namecheck.samename.";   
		requestCompl(s, -1, 1, new String[] { "method1(self)" });
	}
	
	public void testCompositeImport() throws BadLocationException, IOException, Exception{
		String s;
		s = "import xml.sax\n"+ 
		"print xml.sax.";   
		requestCompl(s, -1, -1, new String[] { "default_parser_list" });
	}
	
	public void testIsInGlobalTokens() throws BadLocationException, IOException, Exception{
		IModule module = nature.getAstManager().getModule("testAssist.__init__", nature, true);
		assertTrue(module.isInGlobalTokens("assist.ExistingClass.existingMethod", nature, new CompletionCache()));
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
        
        String s = "Foo().";
        strs = PySelection.getActivationTokenAndQual(new Document(s), s.length(), false); 
        assertEquals("Foo().", strs[0]);
        assertEquals("", strs[1]);
        
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
        
        String importsTipperStr = ImportsSelection.getImportsTipperStr(new Document("from coilib.decorators import "), 30).importsTipperStr;
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
        assertFalse(act.alreadyHasParams);
        
        act = PySelection.getActivationTokenAndQual(new Document("m1.m2()"), 6, false, true); 
        assertEquals("m1.", act.activationToken);
        assertEquals("m2", act.qualifier);
        assertTrue(act.changedForCalltip);
        assertFalse(act.alreadyHasParams);
        
        act = PySelection.getActivationTokenAndQual(new Document("m1.m2(  \t)"), 9, false, true); 
        assertEquals("m1.", act.activationToken);
        assertEquals("m2", act.qualifier);
        assertTrue(act.changedForCalltip);
        assertFalse(act.alreadyHasParams);
        
        act = PySelection.getActivationTokenAndQual(new Document("m1(a  , \t)"), 9, false, true); 
        assertEquals("", act.activationToken);
        assertEquals("m1", act.qualifier);
        assertTrue(act.changedForCalltip);
        assertTrue(act.alreadyHasParams);
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
    
    public void testCompletionAfterClassInstantiation() throws CoreException, BadLocationException {
        String s;
        s = "" +
        "class Foo:\n" +
        "    def m1(self):pass\n" +
        "\n" +
        "Foo()." +
        "";  
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
    }
    
    public void testClassConstructorParams() throws CoreException, BadLocationException {
        String s;
        String original = "" +
        "class Foo:\n" +
        "    def __init__(self, a, b):pass\n\n" +
        "    def m1(self):pass\n\n" +
        "Foo(%s)" + //completion inside the empty parentesis should: add the parameters in link mode (a, b) and let the calltip there.
        "";  
        s = StringUtils.format(original, "");
        
        ICompletionProposal[] proposals = requestCompl(s, s.length()-1, -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposal prop = proposals[0];
        assertEquals("Foo(a, b)", prop.getDisplayString());
        
        PyCalltipsContextInformation contextInformation = (PyCalltipsContextInformation) prop.getContextInformation();
        assertEquals("a, b", contextInformation.getContextDisplayString());
        assertEquals("a, b", contextInformation.getInformationDisplayString());
        
        Document doc = new Document(s);
        prop.apply(doc);
        String expected = StringUtils.format(original, "a, b");    
        assertEquals(expected, doc.get());
    }
    
    public void testRegularClass() throws Exception {
        String s;
        s = "" +
        "class Fooooo:\n" +
        "    def __init__(self, a, b):pass\n\n" +
        "Fooo\n";
        ICompletionProposal[] proposals = requestCompl(s, s.length()-1, -1, new String[] {});
        assertEquals(1, proposals.length); 
        ICompletionProposal p = proposals[0];
        assertEquals("Fooooo", p.getDisplayString());
    }
    
    public void testSelfCase() throws Exception {
        String s;
        s = "" +
        "class Foo:\n" +
        "    def __init__(self, a, b):pass\n\n" +
        "Foo.__init__\n"; //we should only strip the self if we're in an instance (which is not the case)
        ICompletionProposal[] proposals = requestCompl(s, s.length()-1, -1, new String[] {});
        assertEquals(1, proposals.length); 
        ICompletionProposal p = proposals[0];
        assertEquals("__init__(self, a, b)", p.getDisplayString());
    }
    
    public void testDuplicate() throws Exception {
        String s = 
            "class Foo(object):\n" +
            "    def __init__(self):\n" +
            "        self.attribute = 1\n" +
            "        self.attribute2 = 2";
        
        ICompletionProposal[] proposals = requestCompl(s, s.length()-"ute2 = 2".length(), 1, new String[] {"attribute"});
        assertEquals(1, proposals.length); 
    }
    
    public void testDuplicate2() throws Exception {
        String s = 
            "class Bar(object):\n" +
            "    def __init__(self):\n" +
            "        foobar = 10\n" +
            "        foofoo = 20";
        //locals work because it will only get the locals that are before the cursor line
        ICompletionProposal[] proposals = requestCompl(s, s.length()-"foo = 20".length(), 1, new String[] {"foobar"});
        assertEquals(1, proposals.length); 
    }
    
    public void testNoCompletionsForContext() throws Exception {
        String s = 
            "class Foo(object):\n" +
            "    pass\n" +
            "class F(object):\n" +
            "    pass";
        //we don't want completions when we're declaring a class
        ICompletionProposal[] proposals = requestCompl(s, s.length()-"(object):\n    pass".length(), 0, new String[] {});
        assertEquals(0, proposals.length); 
    }
    
    public void testClassmethod() throws Exception {
        String s0 = 
            "class Foo:\n" +
            "    @classmethod\n" +
            "    def method1(cls, a, b):\n" +
            "        pass\n" +
            "    \n" +
            "Foo.met%s";
        
        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length); 
        PyCompletionProposal p = (PyCompletionProposal) proposals[0];
        assertEquals("method1(a, b)", p.getDisplayString());
        
        
        Document document = new Document(s);
        p.apply(document);
        assertEquals(StringUtils.format(s0, "hod1(a, b)"), document.get());
    }
    
    public void testClassmethod2() throws Exception {
        String s0 = 
            "class Foo:\n" +
            "    @classmethod\n" +
            "    def method1(cls, a, b):\n" +
            "        cls.m%s";
        
        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length); 
        PyCompletionProposal p = (PyCompletionProposal) proposals[0];
        assertEquals("method1(a, b)", p.getDisplayString());
        
        
        Document document = new Document(s);
        p.apply(document);
        assertEquals(StringUtils.format(s0, "ethod1(a, b)"), document.get());
    }
    
    public void testClassmethod3() throws Exception {
        String s0 = 
            "class Foo:\n" +
            "    def __init__(self):\n" +
            "        self.myvar = 10\n" +
            "\n" +
            "    def method3(self, a, b):\n" +
            "        pass\n" +
            "\n" +
            "    myvar3=10\n" +
            "    @classmethod\n" +
            "    def method2(cls, a, b):\n" +
            "        cls.myvar2 = 20\n" +
            "\n" +
            "    @classmethod\n" +
            "    def method1(cls, a, b):\n" +
            "        cls.m%s";
        
        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(5, proposals.length); 
        assertContains("method1(a, b)", proposals);
        assertContains("method2(a, b)", proposals);
        assertContains("method3(self, a, b)", proposals);
        assertContains("myvar2", proposals);
        assertContains("myvar3", proposals);
    }
    
    
    public void testClassmethod4() throws Exception {
        String s0 = 
            "from extendable.classmet.mod1 import Foo\n" +
            "Foo.Class%s";
        
        String s = StringUtils.format(s0, "");
        ICompletionProposal[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length); 
        PyCompletionProposal p = (PyCompletionProposal) proposals[0];
        assertEquals("ClassMet()", p.getDisplayString());
        
        
        Document document = new Document(s);
        p.apply(document);
        assertEquals(StringUtils.format(s0, "Met()"), document.get());
    }
    
    public void testRecursion() throws Exception {
        String s = 
            "import testrec4\n" +
            "testrec4.url_for.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] {"m1(self)"});
        assertEquals(1, proposals.length); 
    }
    
    public void testGlobal() throws Exception {
        String s = 
            "class Log:\n" +
            "    def method1(self):\n" +
            "        pass\n" +
            "    \n" +
            "def main():\n" +
            "    global logger\n" +
            "    logger = Log()\n" +
            "    logger.method1()\n" +
            "    \n" +
            "def otherFunction():\n" +
            "    logger.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] {"method1()"});
        assertEquals(1, proposals.length); 
    }
    
    public void testClassInNestedScope() throws Exception {
        String s = 
            "def some_function():\n" +
            "   class Starter:\n" +
            "       def m1(self):\n" +
            "           pass\n" +
            "   Starter.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] {"m1(self)"});
        assertEquals(1, proposals.length); 
    }
    
    public void testClassInNestedScope2() throws Exception {
        String s = 
            "def some_function():\n" +
            "    class Starter:\n" +
            "        def m1(self):\n" +
            "            pass\n" +
            "    s = Starter()\n" +
            "    s.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] {"m1()"});
        assertEquals(1, proposals.length); 
    }
    
    public void testGlobalClassInNestedScope() throws Exception {
        String s = 
            "def some_function():\n" +
            "    class Starter:\n" +
            "        def m1(self):\n" +
            "            pass\n" +
            "    global s\n" +
            "    s = Starter()\n" +
            "\n" +
            "def foo():\n" +
            "    s.";
        ICompletionProposal[] proposals = requestCompl(s, s.length(), 1, new String[] {"m1()"});
        assertEquals(1, proposals.length); 
    }
    
    public void testAssign() throws Exception {
        String s = 
            "class Foo(object):\n" +
            "    def foo(self):\n" +
            "        pass\n" +
            "class Bar(object):\n" +
            "    def bar(self):\n" +
            "        pass\n" +
            "    \n" +
            "def m1():\n" +
            "    if 1:\n" +
            "        c = Foo()\n" +
            "    elif 2:\n" +
            "        c = Bar()\n" +
            "    c.";
        requestCompl(s, s.length(), 2, new String[] {"foo()", "bar()"});
    }

    public void testAssign2() throws Exception {
        String s = 
            "class Foo(object):\n" +
            "    def foo(self):\n" +
            "        pass\n" +
            "class Bar(object):\n" +
            "    def bar(self):\n" +
            "        pass\n" +
            "    \n" +
            "class KKK:\n" +
            "    def m1(self):\n" +
            "        self.c = Foo()\n" +
            "    def m2(self):\n" +
            "        self.c = Bar()\n" +
            "    def m3(self):\n" +
            "        self.c.";
        requestCompl(s, s.length(), 2, new String[] {"foo()", "bar()"});
    }
    
    
    public void testReturn() throws Exception {
        String s = 
            "class Foo:\n" +
            "    def foo(self):\n" +
            "        pass\n" +
            "def m1():\n" +
            "    return Foo()\n" +
            "def m2():\n" +
            "    a = m1()\n" +
            "    a.";
        requestCompl(s, s.length(), 1, new String[] {"foo()"});
    }
    
    
    public void testReturn2() throws Exception {
        String s = 
            "class Foo:\n" +
            "    def method10(self):\n" +
            "        pass\n" +
            "def some_function():\n" +
            "    class Starter:\n" +
            "        def m1(self):\n" +
            "            pass\n" +
            "    if 1:\n" +
            "        return Starter()\n" +
            "    else:\n" +
            "        return Foo()\n" +
            "    \n" +
            "def foo():\n" +
            "    some = some_function()\n" +
            "    some.";
        requestCompl(s, s.length(), 2, new String[] {"m1()", "method10()"});
    }
    
    public void testReturn3() throws Exception {
        String s = 
            "class Foo:\n" +
            "    def method10(self):\n" +
            "        pass\n" +
            "def some_function():\n" +
            "    class Starter:\n" +
            "        def m1(self):\n" +
            "            pass\n" +
            "    def inner():\n" +
            "        return Starter()\n" + //ignore this one
            "    return Foo()\n" +
            "    \n" +
            "def foo():\n" +
            "    some = some_function()\n" +
            "    some.";
        requestCompl(s, s.length(), 1, new String[] {"method10()"});
    }
    
    public void testDecorateObject() throws Exception {
        String s = 
            "class Foo:\n" +
            "    def bar():pass\n"+
            "foo = Foo()\n"+
            "foo.one = 1\n"+
            "foo.two =2\n"+
            "foo.";
        
        requestCompl(s, new String[] {"one", "two", "bar()"});
    }
    
    public void testShadeClassDeclaration() throws Exception {
        String s = 
            "class Foo:\n" +
            "    def m1(self):\n" +
            "        pass\n" +
            "    \n" +
            "Foo = Foo()\n" +
            "Foo.";
        
        //don't let the name override the definition -- as only the name will remain, we won't be able to find the original
        //one, so, it will find it as an unbound call -- this may be fixed later, but as it's a corner case, let's leave
        //it like that for now.
        requestCompl(s, new String[] {"m1()"});
    }
    
    public void testRecursion1() throws Exception {
    	String s = 
    		"from testrec5.messages import foonotexistent\n" +
    		"foonotexistent.";
    	
    	requestCompl(s, new String[] {});
    }
    
    public void testAssignErr() throws Exception {
    	String s = 
    		"class ScalarBarManager:\n" +
    		"    pass\n" +
    		"manager = ScalarBarManager()\n" +
    		"manager._scalar_bars[1].props\n" +
    		"manager.";
    	
    	requestCompl(s, new String[] {"_scalar_bars"});
    }

    public void testInnerDefinition() throws Exception {
        //NOTE: THIS TEST IS CURRENTLY EXPECTED TO FAIL!
        //testInnerDefinition2 is the same but gets the context correctly (must still check why this happens).
        String s = 
            "class Bar:\n" +
            "    \n" +
            "    class Foo:\n" +
            "        pass\n" +
            "    F"; //request at the Bar context
        
        requestCompl(s, new String[] {"Foo"});
    }
    
    
    public void testInnerDefinition2() throws Exception {
        String s = 
            "class Bar:\n" +
            "    \n" +
            "    class Foo:\n" +
            "        pass\n" +
            "    \n" +
            "    F"; //request at the Bar context
        
        requestCompl(s, new String[] {"Foo"});
    }
    
    
}



