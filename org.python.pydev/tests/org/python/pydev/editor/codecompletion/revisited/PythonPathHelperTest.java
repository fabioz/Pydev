/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

/**
 * @author Fabio Zadrozny
 */
public class PythonPathHelperTest extends CodeCompletionTestsBase {

    
	public String qual = "";
	public String token = "";
	public int line;
	public int col;
	public String sDoc = "";



    /**
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
    }
    
    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    
    public void testResolvePath(){
        PythonPathHelper helper = new PythonPathHelper();
        helper.setPythonPath(TestDependent.PYTHON_INSTALL+"Lib| "+TestDependent.PYTHON_INSTALL+"Lib/site-packages|"+
                TestDependent.TEST_PYSRC_LOC);
        
        assertEquals("unittest",helper.resolveModule(TestDependent.PYTHON_INSTALL+"Lib/unittest.py"));
        assertEquals("compiler.ast",helper.resolveModule(TestDependent.PYTHON_INSTALL+"Lib/compiler/ast.py"));
        
        assertEquals("email",helper.resolveModule(TestDependent.PYTHON_INSTALL+"Lib/email"));
        assertSame(null ,helper.resolveModule(TestDependent.PYTHON_INSTALL+"Lib/curses/invalid", true));
        assertSame(null ,helper.resolveModule(TestDependent.PYTHON_INSTALL+"Lib/invalid", true));
        
        assertEquals("testlib",helper.resolveModule(TestDependent.TEST_PYSRC_LOC+"testlib"));
        assertEquals("testlib.__init__",helper.resolveModule(TestDependent.TEST_PYSRC_LOC+"testlib/__init__.py"));
        assertEquals("testlib.unittest",helper.resolveModule(TestDependent.TEST_PYSRC_LOC+"testlib/unittest"));
        assertEquals("testlib.unittest.__init__",helper.resolveModule(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/__init__.py"));
        assertEquals("testlib.unittest.testcase",helper.resolveModule(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/testcase.py"));
        assertEquals(null,helper.resolveModule(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/invalid.py", true));
    }
    
    public void testModuleCompletion(){
        token = "unittest";
        line = 3;
        col = 9;
        
		sDoc = ""+
		"from testlib import unittest \n"+ 
		"                            \n"+  
		"unittest.                   \n";
		
        IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
        comps = ((ICodeCompletionASTManager)nature.getAstManager()).getCompletionsForToken(doc, state);
        assertEquals(9, comps.length);

        ASTManagerTest.assertIsIn("TestCase", comps);
        ASTManagerTest.assertIsIn("main", comps);
        ASTManagerTest.assertIsIn("TestCaseAlias", comps);
        ASTManagerTest.assertIsIn("GUITest", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
        ASTManagerTest.assertIsIn("AnotherTest", comps);
        ASTManagerTest.assertIsIn("anothertest", comps);
        ASTManagerTest.assertIsIn("guitestcase", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
    }
    
    public void testRecursionModuleCompletion(){
        token = "";
        line = 2;
        col = 0;
        
		sDoc = ""+
		"from testrec.imp1 import * \n"+ 
		"                           \n";
		
        IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
        ICodeCompletionASTManager a = (ICodeCompletionASTManager)nature.getAstManager();
        comps = a.getCompletionsForToken(doc, state);
        assertFalse(comps.length == 0);

    }
    
    public void testRecursion2(){
        token = "i";
        line = 3;
        col = 2;
        
		sDoc = ""+
		"from testrec.imp3 import MethodReturn1 \n"+ 
		"i = MethodReturn1()                    \n" +
		"i.";
		
        IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
        ICodeCompletionASTManager a = (ICodeCompletionASTManager)nature.getAstManager();
        comps = a.getCompletionsForToken(doc, state);
        assertEquals(0, comps.length);

    }
    
    
    public void testClassHierarchyCompletion(){
        
		token = "TestCase";
		line = 3;
		col = 9;
      
		sDoc = ""+
		"from testlib.unittest.testcase import TestCase \n"+ 
		"                                              \n"+  
		"TestCase.                                     \n";

		IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
		comps = ((ICodeCompletionASTManager)nature.getAstManager()).getCompletionsForToken(doc, state);
		assertTrue(comps.length > 5);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
    }
    
    public void testClassHierarchyCompletion2(){
        
		token = "GUITest";
		line = 3;
		col = 8;
      
		sDoc = ""+
		"from testlib.unittest import GUITest  \n"+ 
		"                                      \n"+  
		"GUITest.                              \n";

		IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
		comps = ((ICodeCompletionASTManager)nature.getAstManager()).getCompletionsForToken(doc, state);
		ASTManagerTest.assertIsIn("SetWidget", comps);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
        assertTrue(comps.length > 5);
    }

    public void testClassHierarchyCompletion3(){
        
		token = "AnotherTest";
		line = 3;
		col = 12;
      
		sDoc = ""+
		"from testlib.unittest import AnotherTest  \n"+ 
		"                                          \n"+  
		"AnotherTest.                              \n";

		IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
		comps = ((ICodeCompletionASTManager)nature.getAstManager()).getCompletionsForToken(doc, state);
		assertTrue(comps.length > 5);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
        ASTManagerTest.assertIsIn("another", comps);
    }
    
    public void testImportAs(){
        token = "t";
        line = 3;
        col = 2;
        
		sDoc = ""+
		"from testlib import unittest as t \n"+ 
		"                                  \n"+  
		"t.                                \n";
		
        IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
        comps = ((ICodeCompletionASTManager)nature.getAstManager()).getCompletionsForToken(doc, state);
        assertEquals(9, comps.length);

        ASTManagerTest.assertIsIn("TestCase", comps);
        ASTManagerTest.assertIsIn("main", comps);
        ASTManagerTest.assertIsIn("TestCaseAlias", comps);
        ASTManagerTest.assertIsIn("GUITest", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
        ASTManagerTest.assertIsIn("AnotherTest", comps);
        ASTManagerTest.assertIsIn("anothertest", comps);
        ASTManagerTest.assertIsIn("guitestcase", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
    }
    
    public void testImportAs2(){
		token = "t";
		line = 3;
		col = 2;
      
		sDoc = ""+
		"from testlib.unittest import AnotherTest as t \n"+ 
		"                                              \n"+  
		"t.                                            \n";

		IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
		comps = ((ICodeCompletionASTManager)nature.getAstManager()).getCompletionsForToken(doc, state);
		assertTrue(comps.length > 5);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
        ASTManagerTest.assertIsIn("another", comps);

    }

    public void testRelativeImport(){
		token = "Derived";
		line = 3;
		col = 8;
      
		sDoc = ""+
		"from testlib.unittest.relative.testrelative import Derived \n"+ 
		"                                                            \n"+  
		"Derived.                                                    \n";

		IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
		comps = ((ICodeCompletionASTManager)nature.getAstManager()).getCompletionsForToken(doc, state);
        ASTManagerTest.assertIsIn("test1", comps);
        ASTManagerTest.assertIsIn("test2", comps);
		assertEquals(2, comps.length);

        
    }
    
    public void testGetEncoding(){
        String loc = TestDependent.TEST_PYSRC_LOC+"testenc/encutf8.py";
        String encoding = PythonPathHelper.getPythonFileEncoding(new File(loc));
        assertEquals("utf-8", encoding.toLowerCase());
    }

    public static void main(String[] args) {
        
        try {
            PythonPathHelperTest test = new PythonPathHelperTest();
            test.setUp();
//            test.testClassHierarchyCompletion();
            test.testClassHierarchyCompletion2();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PythonPathHelperTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        } catch(Error e){
            e.printStackTrace();
        } catch(Throwable e){
            e.printStackTrace();
        }
    }
}
















