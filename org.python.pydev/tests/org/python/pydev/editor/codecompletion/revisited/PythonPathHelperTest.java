/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.plugin.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class PythonPathHelperTest extends TestCase {

    //NOTE: this should be gotten from some variable to point to the python lib (less system dependence, but still, some).
    public static final String PYTHON_INSTALL="C:/bin/Python23/";
    //NOTE: this should set to the tests pysrc location, so that it can be added to the pythonpath.
    public static final String TEST_PYSRC_LOC="D:/dev_programs/eclipse_3/eclipse/workspace/org.python.pydev/tests/pysrc/";
    
	public ASTManager manager ;
	public PythonNature nature;
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
	    manager = new ASTManager();
	    nature = new PythonNature();
	    manager.changePythonPath(PYTHON_INSTALL+"lib|"+TEST_PYSRC_LOC, null, new NullProgressMonitor());
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
        helper.setPythonPath(PYTHON_INSTALL+"lib| "+PYTHON_INSTALL+"lib/site-packages|"+
                TEST_PYSRC_LOC);
        
        assertEquals("unittest",helper.resolveModule(PYTHON_INSTALL+"lib/unittest.py"));
        assertEquals("compiler.ast",helper.resolveModule(PYTHON_INSTALL+"lib/compiler/ast.py"));
        
        assertEquals("email",helper.resolveModule(PYTHON_INSTALL+"lib/email"));
        assertSame(null ,helper.resolveModule(PYTHON_INSTALL+"lib/curses/invalid"));
        assertSame(null ,helper.resolveModule(PYTHON_INSTALL+"lib/invalid"));
        
        assertEquals("testlib",helper.resolveModule(TEST_PYSRC_LOC+"testlib"));
        assertEquals("testlib.__init__",helper.resolveModule(TEST_PYSRC_LOC+"testlib/__init__.py"));
        assertEquals("testlib.unittest",helper.resolveModule(TEST_PYSRC_LOC+"testlib/unittest"));
        assertEquals("testlib.unittest.__init__",helper.resolveModule(TEST_PYSRC_LOC+"testlib/unittest/__init__.py"));
        assertEquals("testlib.unittest.testcase",helper.resolveModule(TEST_PYSRC_LOC+"testlib/unittest/testcase.py"));
        assertEquals(null,helper.resolveModule(TEST_PYSRC_LOC+"testlib/unittest/invalid.py"));
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
        comps = manager.getCompletionsForToken(doc, state);
        assertEquals(6, comps.length);

        ASTManagerTest.assertIsIn("TestCase", comps);
        ASTManagerTest.assertIsIn("main", comps);
        ASTManagerTest.assertIsIn("TestCaseAlias", comps);
        ASTManagerTest.assertIsIn("GUITest", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
        ASTManagerTest.assertIsIn("AnotherTest", comps);
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
		comps = manager.getCompletionsForToken(doc, state);
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
		comps = manager.getCompletionsForToken(doc, state);
		assertTrue(comps.length > 5);
        ASTManagerTest.assertIsIn("assertEquals", comps);
        ASTManagerTest.assertIsIn("assertNotEquals", comps);
        ASTManagerTest.assertIsIn("assertAlmostEquals", comps);
        ASTManagerTest.assertIsIn("SetWidget", comps);
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
		comps = manager.getCompletionsForToken(doc, state);
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
        comps = manager.getCompletionsForToken(doc, state);
        assertEquals(6, comps.length);

        ASTManagerTest.assertIsIn("TestCase", comps);
        ASTManagerTest.assertIsIn("main", comps);
        ASTManagerTest.assertIsIn("TestCaseAlias", comps);
        ASTManagerTest.assertIsIn("GUITest", comps);
        ASTManagerTest.assertIsIn("testcase", comps);
        ASTManagerTest.assertIsIn("AnotherTest", comps);
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
		comps = manager.getCompletionsForToken(doc, state);
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
		"from testlib.unittest.relative.testrelative import  Derived \n"+ 
		"                                                            \n"+  
		"Derived.                                                    \n";

		IToken[] comps = null;
        Document doc = new Document(sDoc);
        CompletionState state = new CompletionState(line,col, token, nature);
		comps = manager.getCompletionsForToken(doc, state);
		assertEquals(2, comps.length);
        ASTManagerTest.assertIsIn("test1", comps);
        ASTManagerTest.assertIsIn("test2", comps);

        
    }
    
	public static void main(String[] args) {
	    //IMPORTANT: I don't want to test the compiled modules, only the source modules.
        
        junit.textui.TestRunner.run(PythonPathHelperTest.class);
//        try {
//            PythonPathHelperTest test = new PythonPathHelperTest();
//            test.setUp();
//            test.testRelativeImport();
//            test.tearDown();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } catch(Error e){
//            e.printStackTrace();
//        }
    }
}
















