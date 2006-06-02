/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;

public class PyASTChangerTest extends TestCase {

    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyASTChangerTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    
    public void test1() throws Exception {
        Document doc = new Document("");
        PyASTChanger changer = new PyASTChanger(doc);
        SimpleNode ast = changer.getAST();
        Module m = (Module) ast;
        assertEquals(0, m.body.length);
        
        ClassDef classDef = PyASTFactory.makePassClassDef("test");
        
        changer.addStmt(m, "body", 0, classDef);
        changer.apply(new NullProgressMonitor());
        
        String result = doc.get();
        if(DEBUG){
            System.out.println(result);
        }
        assertEquals("class test:\n    pass\n", result);
    }
    
    public void test2() throws Exception {
        Document doc = new Document("class C1:pass\n");
        PyASTChanger changer = new PyASTChanger(doc);
        SimpleNode ast = changer.getAST();
        Module m = (Module) ast;
        assertEquals(1, m.body.length);
        
        ClassDef classDef = PyASTFactory.makePassClassDef("test");
        
        changer.addStmt(m, "body", 1, classDef);
        changer.apply(new NullProgressMonitor());
        
        String result = doc.get();
        if(DEBUG){
            System.out.println(result);
        }
        assertEquals("class C1:pass\nclass test:\n    pass\n", result);
        
    }

}
