/*
 * Created on Sep 1, 2006
 * @author Fabio
 */
package org.python.pydev.parser;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.With;

/**
 * Test for parsing python 2.5
 * @author Fabio
 */
public class PyParser25Test extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParser25Test test = new PyParser25Test();
            test.setUp();
            test.testNewWithStmt2();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser25Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PyParser.USE_FAST_STREAM = true;
    }

    public void testForWithCondExp() {
        String s = "" +
        "verify([ x(False) for x in (lambda x: False if x else True, lambda x: True if x else False) if x(False) ] == [True])\n" +
        "";
        parseLegalDocStr(s);
    }
    
    /**
     * This test checks the new conditional expression.
     */
    public void testConditionalExp1(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "a = 1 if True else 2\n";
        parseLegalDocStr(str);
    }
    
    /**
     * This test checks the new conditional expression.
     */
    public void testEmptyYield(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
                "def whee():\n" +
                "    yield\n" +
                "";
        parseLegalDocStr(str);
    }
    
    /**
     * This test checks the new relative import
     */
    public void testNewRelativeImport(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "from . import foo\n";
        Module mod = (Module) parseLegalDocStr(str);
        ImportFrom f = (ImportFrom) mod.body[0];
        assertEquals(1, f.level);
        assertEquals("", ((NameTok)f.module).id);
    }
    
    public void testNewRelativeImport2(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "from ..bar import foo\n";
        Module mod = (Module) parseLegalDocStr(str);
        ImportFrom f = (ImportFrom) mod.body[0];
        assertEquals(2, f.level);
        assertEquals("bar", ((NameTok)f.module).id);
    }
    
    public void testNewRelativeImport3(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "from ... import foo\n";
        Module mod = (Module) parseLegalDocStr(str);
        ImportFrom f = (ImportFrom) mod.body[0];
        assertEquals(3, f.level);
        assertEquals("", ((NameTok)f.module).id);
    }
    
    public void testNewWithStmt(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
                "from __future__ import with_statement\n" +
                "with foo:\n" +
                "    print 'bla'\n" +
                "";
        //we'll actually treat this as a try..finally with a body with try..except..else
        Module mod = (Module) parseLegalDocStr(str);
        assertEquals(2, mod.body.length);
        assertTrue(mod.body[1] instanceof With);
        With w = (With) mod.body[1];
        assertTrue(w.optional_vars == null);
        
    }
    
    public void testNewWithStmt2(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
                "from __future__ import with_statement\n" +
                "with foo as x:\n" +
                "    print 'bla'\n" +
                "";
        //we'll actually treat this as a try..finally with a body with try..except..else
        Module mod = (Module) parseLegalDocStr(str);
        assertEquals(2, mod.body.length);
        assertTrue(mod.body[1] instanceof With);
        With w = (With) mod.body[1];
        assertTrue(w.optional_vars != null);
        
    }
    
    public void testNewWithStmtError(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
        //"from __future__ import with_statement\n"  -- as it is not specified, it should throw an error
        "with foo as x:\n" +
        "    print 'bla'\n" +
        "";
        //we'll actually treat this as a try..finally with a body with try..except..else
        parseILegalDoc(new Document(str));
    }
    
    public void testNewTryFinally(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
                "try:\n" +
                "    'try'\n" +
                "except:\n" +
                "    'except'\n" +
                "else:\n" +
                "    'else'\n" +
                "finally:\n" +
                "    'finally'\n" +
                "\n" +
                "";
        //we'll actually treat this as a try..finally with a body with try..except..else
        Module mod = (Module) parseLegalDocStr(str);
        assertEquals(1, mod.body.length);
        TryFinally f = (TryFinally) mod.body[0];
        
        assertEquals(1, f.body.length);
        TryExcept exc = (TryExcept) f.body[0];
        assertTrue(exc.orelse != null);
        assertEquals(1, exc.handlers.length);
        
    }
    
    /**
     * This test checks that the old version still gives an error
     */
    public void testConditionalExp1err(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_4);
        String str = "a = 1 if True else 2\n";
        parseILegalDoc(new Document(str));
    }
    
    /**
     * This test checks that the old version still gives an error
     */
    public void testWith(){
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_4);
        String str = "" +
                "with foo:\n" +
                "    print 'bla'\n" +
                "";
        parseILegalDoc(new Document(str));
    }
}
