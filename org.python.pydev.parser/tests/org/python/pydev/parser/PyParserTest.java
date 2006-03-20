/*
 * Created on 27/08/2005
 */
package org.python.pydev.parser;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class PyParserTest extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParserTest test = new PyParserTest();
            test.setUp();
            test.testErr();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParserTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    public void testCorrectArgs() {
        String s = "" +
        "class Class1:         \n" +
        "    def met1(self, a):\n" +
        "        pass";
        SimpleNode node = parseLegalDocStr(s);
        Module m = (Module) node;
        ClassDef d = (ClassDef) m.body[0];
        FunctionDef f = (FunctionDef) d.body[0];
        assertEquals("self",((Name)f.args.args[0]).id);
        assertEquals("a",((Name)f.args.args[1]).id);
    }
    
    public void testErr() {
    	String s = "" +
    	"def m():\n" +
    	"    call(a,";
    	ParseException exception = parseILegalDoc(new Document(s));
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
    
    public void testOnNumarray() {
        if(TestDependent.HAS_NUMARRAY_INSTALLED){
            
            File file = new File(TestDependent.PYTHON_NUMARRAY_PACKAGES);
            parseFilesInDir(file);
            file = new File(TestDependent.PYTHON_NUMARRAY_PACKAGES+"linear_algebra/");
            parseFilesInDir(file);
        }
    }
    
    public void testOnWxPython() {
        if(TestDependent.HAS_WXPYTHON_INSTALLED){
            File file = new File(TestDependent.PYTHON_WXPYTHON_PACKAGES+"wxPython");
            parseFilesInDir(file);
            file = new File(TestDependent.PYTHON_WXPYTHON_PACKAGES+"wx");
            parseFilesInDir(file);
        }
    }

    public void testOnCompleteLib() {
        File file = new File(TestDependent.PYTHON_LIB);
        parseFilesInDir(file);
    }

    /**
     * @param file
     */
    private void parseFilesInDir(File file) {
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
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
    
    public void testOnDocBaseHTTPServer() {
        String loc = TestDependent.PYTHON_LIB+"BaseHTTPServer.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testOnDocXMLRPCServerMod() {
        String loc = TestDependent.PYTHON_LIB+"DocXMLRPCServer.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
    public void testNewImportParser() {
        String s = "" +
        "from a import (b,\n" +
        "            c,\n" +
        "            d)\n" +
        "\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testNewImportParser2() {
        String s = "" +
        "from a import (b,\n" +
        "            c,\n" +
        "            )\n" +
        "\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testNewImportParser3() {
        String s = "" +
        "from a import (b,\n" +
        "            c,,\n" + //err
        "            )\n" +
        "\n" +
        "\n" +
        "";
        parseILegalDoc(new Document(s));
    }
    
    public void testParser() {
        String s = "class C: pass";
        parseLegalDocStr(s);
    }

    public void testEndWithComment() {
        String s = "class C: \n" +
                "    pass\n" +
                "#end\n" +
                "";
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
    
    
    public void testParser9() {
        String s = "" +
        "a[1,]\n"+
        "a[1,2]\n"+
        "\n"+
        "\n"+
        "\n"+
        "\n";        
        parseLegalDocStr(s);
    }
    
    /**
     * l = [ "encode", "decode" ]
     * 
     * expected beginCols at: 7 and 17
     */
    public void testParser10() {
    	String s = "" +
    	"l = [ \"encode\", \"decode\" ] \n"+
    	"\n";        
    	SimpleNode node = parseLegalDocStr(s);
    	List<ASTEntry> strs = SequencialASTIteratorVisitor.create(node).getAsList(new Class[]{Str.class});
    	assertEquals(7, strs.get(0).node.beginColumn);
    	assertEquals(17, strs.get(1).node.beginColumn);
    }
    
    
    public void testParser11() {
        String s = "" +
        "if True:\n"+        
        "    pass\n"+        
        "elif True:\n"+        
        "    pass\n"+        
        "else:\n"+        
        "    pass\n"+        
        "\n"+        
        "\n";        
        parseLegalDocStr(s);
    }
    
}
