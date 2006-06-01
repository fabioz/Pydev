/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;

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
        SimpleNode ast = changer.getInitialAST();
        Module m = (Module) ast;
        assertEquals(0, m.body.length);
        
        NameTok name = new NameTok("test", NameTok.ClassName);
        name.addSpecial(":", true);
        Pass pass = new Pass();
        pass.addSpecial("pass", false);
        ClassDef classDef = new ClassDef(name, new exprType[0], new stmtType[]{pass});
        
        changer.addStmt(m, "body", 0, classDef);
        
        changer.apply();
        
        String result = doc.get();
        if(DEBUG){
            System.out.println(result);
        }
        assertEquals("class test:\n    pass\n", result);
    }
}
