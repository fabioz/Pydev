/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;

public class PyASTChangerTest extends TestCase {

    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        try {
            PyASTChangerTest test = new PyASTChangerTest();
            test.setUp();
            test.test4();
            test.tearDown();
            
            junit.textui.TestRunner.run(PyASTChangerTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    
    public void test1() throws Exception {
        Document doc = new Document("");
        PyASTChanger changer = new PyASTChanger(doc, IPythonNature.LATEST_GRAMMAR_VERSION);
        SimpleNode ast = changer.getAST();
        Module m = (Module) ast;
        assertEquals(0, m.body.length);
        
        ClassDef classDef = PyASTFactory.makePassClassDef("test");
        
        changer.addStmtToNode(m, "body", 0, classDef, true);
        changer.apply(new NullProgressMonitor());
        
        String result = doc.get();
        if(DEBUG){
            System.out.println(result);
        }
        assertEquals("class test:\n    pass\n", result);
    }
    
    public void test2() throws Exception {
        Document doc = new Document("class C1:pass\n");
        PyASTChanger changer = new PyASTChanger(doc, IPythonNature.LATEST_GRAMMAR_VERSION);
        SimpleNode ast = changer.getAST();
        Module m = (Module) ast;
        assertEquals(1, m.body.length);
        
        ClassDef classDef = PyASTFactory.makePassClassDef("test");
        
        changer.addStmtToNode(m, "body", 1, classDef, true);
        changer.apply(new NullProgressMonitor());
        
        String result = doc.get();
        if(DEBUG){
            System.out.println(result);
        }
        assertEquals("class C1:pass\nclass test:\n    pass\n", result);
        
    }
    
    public void test3() throws Exception {
        Document doc = new Document("" +
                "class C1:pass\n" +
                "class C2:pass\n" +
                "");
        PyASTChanger changer = new PyASTChanger(doc, IPythonNature.LATEST_GRAMMAR_VERSION);
        SimpleNode ast = changer.getAST();
        Module m = (Module) ast;
        assertEquals(2, m.body.length);
        
        ClassDef classDef = PyASTFactory.makePassClassDef("test");
        
        changer.addStmtToNode(m, "body", 1, classDef, true);
        changer.apply(new NullProgressMonitor());
        
        String result = doc.get();
        if(DEBUG){
            System.out.println(result);
        }
        assertEquals("class C1:pass\nclass test:\n    pass\nclass C2:pass\n", result);
        
    }
    
    public void test4() throws Exception {
        Document doc = new Document("" +
                "class C1:pass\n" +
                "class C2:pass\n" +
        "");
        PyASTChanger changer = new PyASTChanger(doc, IPythonNature.LATEST_GRAMMAR_VERSION);
        SimpleNode ast = changer.getAST();
        Module m = (Module) ast;
        assertEquals(2, m.body.length);
        
        changer.delStmtFromNode(m, "body", 1);
        changer.apply(new NullProgressMonitor());
        
        String result = doc.get();
        if(DEBUG){
            System.out.println(result);
        }
        assertEquals("class C1:pass\n", result);
        
    }
    

}
