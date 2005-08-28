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
            test.testOnDocXMLRPCServerMod();
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

    public void testDecorator() {
        String s = "" +
            "class C:\n" +
            "    \n" +
            "    @staticmethod\n" +
            "    def m():\n" +
            "        pass\n" +
            "";
        parseLegalDocStr(s);
    }
    
    public void testDecorator2() {
        String s = "" +
            "@funcattrs(status=\"experimental\", author=\"BDFL\")\n" +
            "@staticmethod\n" +
            "def longMethodNameForEffect(*args):\n" +
            "    pass\n" +
            "\n" +
            "";
        parseLegalDocStr(s);
    }
    
    public void testDecorator4() {
        String s = "" +
        "@funcattrs(1)\n" +
        "def longMethodNameForEffect(*args):\n" +
        "    pass\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testDecorator5() {
        String s = "" +
        "@funcattrs(a)\n" +
        "def longMethodNameForEffect(*args):\n" +
        "    funcattrs(1)\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testDecorator3() {
        String s = "" +
        "@funcattrs(a, 1, status=\"experimental\", author=\"BDFL\", *args, **kwargs)\n" +
        "@staticmethod1\n" +
        "@staticmethod2(b)\n" +
        "def longMethodNameForEffect(*args):\n" +
        "    pass\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testDecorator6() {
        String s = "" +
        "@funcattrs(b for b in x)\n" +
        "def longMethodNameForEffect(*args):\n" +
        "    pass\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testOnCompleteLib() {
        File file = new File(TestDependent.PYTHON_LIB);
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if(f.getAbsolutePath().toLowerCase().endsWith(".py")){
                parseLegalDocStr(REF.getFileContents(f), f);
            }
        }
    }
    
    public void testOnUnittestMod() {
        String loc = TestDependent.PYTHON_LIB+"unittest.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testOnCodecsMod() {
        String loc = TestDependent.PYTHON_LIB+"codecs.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testOnDocXMLRPCServerMod() {
        String loc = TestDependent.PYTHON_LIB+"DocXMLRPCServer.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
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
    
    public void testParser8() {
        String s = "" +
"if type(clsinfo) in (types.TupleType, types.ListType):\n"+
"    pass\n"+
"\n"+
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
    private void parseLegalDocStr(String s, Object ... additionalErrInfo) {
        Document doc = new Document(s);
        parseLegalDoc(doc, additionalErrInfo);
    }

    /**
     * @param additionalErrInfo 
     * @param parser
     */
    private void parseLegalDoc(IDocument doc, Object[] additionalErrInfo) {
        parser.setDocument(doc);
        Object[] objects = parser.reparseDocument((IPythonNature)null);
        Object err = objects[1];
        if(err != null){
            String s = "";
            for (int i = 0; i < additionalErrInfo.length; i++) {
                s += additionalErrInfo[i];
            }
            fail("Expected no error, received: "+err+" "+s);
        }
        assertNotNull(objects[0]);
    }
    
}
