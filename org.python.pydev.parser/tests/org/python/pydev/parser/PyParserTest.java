/*
 * Created on 27/08/2005
 */
package org.python.pydev.parser;

import java.io.File;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.parser.ParseException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;

import junit.framework.TestCase;

public class PyParserTest extends TestCase {

    public static void main(String[] args) {
        try {
            PyParserTest test = new PyParserTest();
            test.setUp();
            test.testParser7();
            test.tearDown();
            junit.textui.TestRunner.run(PyParserTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PyParser parser;

    protected void setUp() throws Exception {
        PyParser.ACCEPT_NULL_EDITOR = true;
        PyParser.ENABLE_TRACING = true;
        ParseException.verboseExceptions = true;
        parser = new PyParser();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        PyParser.ACCEPT_NULL_EDITOR = false;
        PyParser.ENABLE_TRACING = false;
        ParseException.verboseExceptions = false;
        super.tearDown();
    }
    
    public void testYield() {
        String s = "" +
                "def m():\n" +
                "    yield 1";
        parseLegalDocStr(s);
    }

//    public void testOnUnittestMod() {
//        String loc = TestDependent.PYTHON_LIB+"unittest.py";
//        String s = REF.getFileContents(new File(loc));
//        parseLegalDocStr(s);
//    }
    
    public void testParser() {
        String s = "class C: pass";
        parseLegalDocStr(s);
    }
    
    public void testParser7() {
        String s = "" +
        "if a < (2, 2):\n"+
        "    False, True = 0, 1\n"+
        "\n"+
        "\n";
        parseLegalDocStr(s);
    }
    
    public void testParser2() {
        String s = "" +
        "td = dict()                                                            \n"+
        "                                                                       \n"+
        "for foo in sorted(val for val in td.itervalues() if val[0] == 's'):    \n"+
        "    print foo                                                          \n";
        
        parseLegalDocStr(s);
    }
    
    public void testParser3() {
        String s = "print (x for x in y)";
        
        parseLegalDocStr(s);
    }

    public void testParser4() {
        String s = "print sum(x for x in y)";
        
        parseLegalDocStr(s);
    }
    
    public void testParser5() {
        String s = "print sum(x.b for x in y)";
        
        parseLegalDocStr(s);
    }
    
    public void testParser6() {
        String s = "" +
        "import re\n"+
        "def firstMatch(s,regexList):\n"+
        "    for match in (regex.search(s) for regex in regexList):\n"+
        "        if match: return match\n"+
        "\n"+
        "\n";        
        parseLegalDocStr(s);
    }
    
    /**
     * @param s
     */
    private void parseLegalDocStr(String s) {
        Document doc = new Document(s);
        parseLegalDoc(doc);
    }

    /**
     * @param parser
     */
    private void parseLegalDoc(IDocument doc) {
        parser.setDocument(doc);
        Object[] objects = parser.reparseDocument((IPythonNature)null);
        Object err = objects[1];
        if(err != null){
            fail("Expected no error, received: "+err);
        }
        assertNotNull(objects[0]);
    }
    
}
