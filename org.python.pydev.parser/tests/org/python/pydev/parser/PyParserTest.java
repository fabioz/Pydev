/*
 * Created on 27/08/2005
 */
package org.python.pydev.parser;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.performanceeval.Timer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.prettyprinter.PrettyPrinter;
import org.python.pydev.parser.prettyprinter.PrettyPrinterPrefs;
import org.python.pydev.parser.prettyprinter.WriterEraser;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class PyParserTest extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParserTest test = new PyParserTest();
            test.setUp();
            
            //Timer timer = new Timer();
            //test.parseFilesInDir(new File("D:/bin/Python251/Lib/site-packages/wx-2.8-msw-unicode"), true);
            //test.parseFilesInDir(new File("D:/bin/Python251/Lib/"), false);
            //timer.printDiff();
            test.testThreadingInParser();
            test.tearDown();
            
            
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParserTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PyParser.USE_FAST_STREAM = true;
    }
    
    

    public void testRemoveEndingComments() throws Exception {
        String s = 
                "class Foo:pass\n" +
                "#comm1\n" +
                "#comm2\n" +
                "print 'no comm'\n" +
                "#comm3\n" +
                "#comm4";
        Document doc = new Document(s);
        
        List<commentType> comments = PyParser.removeEndingComments(doc);
        assertEquals("#comm3", comments.get(0).id);
        assertEquals(1, comments.get(0).beginColumn);
        assertEquals(5, comments.get(0).beginLine);
        
        assertEquals("#comm4", comments.get(1).id);
        assertEquals(1, comments.get(1).beginColumn);
        assertEquals(6, comments.get(1).beginLine);
        
        assertEquals("class Foo:pass\n" +
                "#comm1\n" +
                "#comm2\n" +
                "print 'no comm'\n", doc.get());
    }
    
    public void testRemoveEndingComments2() throws Exception {
        String s = 
            "class C: \n" +
            "    pass\n" +
            "#end\n" +
            "";
        Document doc = new Document(s);
        List<commentType> comments = PyParser.removeEndingComments(doc);
        assertEquals(1, comments.get(0).beginColumn);
        assertEquals(3, comments.get(0).beginLine);
        assertEquals("#end" , comments.get(0).id);
        assertEquals("class C: \n" +
                "    pass\n" 
                , doc.get());
    }
    
    public void testRemoveEndingComments3() throws Exception {
        String s = 
            "##end\n" +
            "##end\n" +
            "";
        Document doc = new Document(s);
        List<commentType> comments = PyParser.removeEndingComments(doc);
        
        assertEquals(1, comments.get(0).beginColumn);
        assertEquals(1, comments.get(0).beginLine);
        assertEquals("##end" , comments.get(0).id);
        
        assertEquals(2, comments.get(1).beginLine);
        assertEquals(1, comments.get(1).beginColumn);
        assertEquals("##end" , comments.get(1).id);
        assertEquals("", doc.get());
    }
    
    public void testTryReparse() throws BadLocationException{
        Document doc = new Document("");
        for(int i=0; i< 5; i++){
            doc.replace(0, 0, "this is a totally and completely not parseable doc\n");
        }
        
        PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(doc, true, IPythonNature.LATEST_GRAMMAR_VERSION);
        parserInfo.tryReparse = true;
        Tuple<SimpleNode,Throwable> reparseDocument = PyParser.reparseDocument(parserInfo);
        assertTrue(reparseDocument.o1 == null);
        assertTrue(reparseDocument.o2 != null);
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
    
    public void testMultilineStr() {
        String s = "" +
        "a = '''\n" +
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n"+
        "really really big string\n" +
        "really really big string\n" +
        "really really big string\n" +
        "'''";
        parseLegalDocStr(s);
    }
    
    public void testErr() {
        String s = "" +
        "def m():\n" +
        "    call(a,";
        parseILegalDoc(new Document(s));
    }
    
    
    public void testEmptyBaseForClass() {
        String s = "" +
        "class B2(): pass\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    public void testFor2() {
        String s = "" +
        "[x for x in 1,2,3,4]\n" +
        "";
        parseLegalDocStr(s);
    }
    public void testFor2a() {
        String s = "" +
        "[x for x in 2,3,4 if x > 2]\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testFor3() {
        String s = "" +
        "[x() for x in lambda: True, lambda: False if x() ] \n" +
        "";
        parseLegalDocStr(s);
    }
    
    
    public void testYield() {
        String s = "" +
                "def m():\n" +
                "    yield 1";
        parseLegalDocStr(s);
    }
    
    public void testYield2() {
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_4);
        String s = "" +
        "class Generator:\n" +
        "    def __iter__(self): \n" +
        "        for a in range(10):\n" +
        "            yield foo(a)\n" +
        "";
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

    private void parseFilesInDir(File file) {
        parseFilesInDir(file, false);
    }
    
    
//    not removed completely because we may still want to debug it later...
//    public void testOnCsv() {
//        PyParser.USE_FAST_STREAM = false;
//        String loc = TestDependent.PYTHON_LIB+"csv.py";
//        String s = REF.getFileContents(new File(loc));
//        parseLegalDocStr(s);
//        
//        PyParser.USE_FAST_STREAM = true;
//        loc = TestDependent.PYTHON_LIB+"csv.py";
//        s = REF.getFileContents(new File(loc));
//        parseLegalDocStr(s);
//    }
    
    
    public void testOnCgiMod() {
        String s = "dict((day, index) for index, daysRep in enumeratedDays for day in daysRep)";
        parseLegalDocStr(s);
    }
    
    public void testOnCgiMod2() {
        String loc = TestDependent.PYTHON_LIB+"cgi.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }
    
//    this should really give errors (but is not a priority)
//    public void testErrOnFor() {
//        //ok, it should throw errors in those cases (but that's not so urgent)
//        String s = "foo(x for x in range(10), 100)\n";
//        parseILegalDoc(new Document(s));
//        
//        String s1 = "foo(100, x for x in range(10))\n";
//        parseILegalDoc(new Document(s1));
//        
//    }
    
    public void testOnTestGrammar() {
        String loc = TestDependent.PYTHON_LIB+"test/test_grammar.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s,"(file: test_grammar.py)");
    }
    
    public void testOnTestContextLib() {
        if(TestDependent.HAS_PYTHON_TESTS){
            String loc = TestDependent.PYTHON_LIB+"test/test_contextlib.py";
            String s = REF.getFileContents(new File(loc));
            parseLegalDocStr(s,"(file: test_contextlib.py)");
        }
    }
    
    public void testOnCalendar() {
        String loc = TestDependent.PYTHON_LIB+"hmac.py";
        String s = REF.getFileContents(new File(loc));
        parseLegalDocStr(s);
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
    
    public void testParser13() throws Exception {
        String s = "plural = lambda : None";
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
    
    public void testParser12() {
        String s = "" +
        "m1()\n"+        
        "\n";        
        parseLegalDocStr(s);
    }
    
    
	public void testThreadingInParser() throws Exception {
    	String loc = TestDependent.PYTHON_LIB+"unittest.py";
        String s = REF.getFileContents(new File(loc));

        final Integer[] calls = new Integer[]{0};
        final Boolean[] failedComparisson = new Boolean[]{false};
        
        ICallback<Object, Boolean> callback = new ICallback<Object, Boolean>(){

			public Object call(Boolean failTest) {
			    synchronized (calls) {
			        calls[0] = calls[0]+1;
			        if(failTest){
			            failedComparisson[0] = true;
			        }
			        return null;
                }
			}
        	
        };
        SimpleNode node = parseLegalDocStr(s);
        String expected = printNode(node);
        
        int expectedCalls = 70;
        Timer timer = new Timer();
		for(int j=0;j<expectedCalls;j++){
			startParseThread(s, callback, expected);
		}
		
		while(calls[0] < expectedCalls){
			synchronized(this){
				wait(5);
			}
		}
		timer.printDiff();
		assertTrue(!failedComparisson[0]);
	}

	private String printNode(SimpleNode node) {
      final WriterEraser stringWriter = new WriterEraser();
      PrettyPrinterPrefs prettyPrinterPrefs = new PrettyPrinterPrefs("\n");
      prettyPrinterPrefs.setSpacesAfterComma(1);
      prettyPrinterPrefs.setSpacesBeforeComment(1);
      PrettyPrinter printer = new PrettyPrinter(prettyPrinterPrefs, stringWriter);
      try {
          node.accept(printer);
          return stringWriter.getBuffer().toString();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
	}

	
	private void startParseThread(final String contents, final ICallback<Object, Boolean> callback, 
			final String expected) {
		
		new Thread(){
			public void run() {
				try{
					SimpleNode node = parseLegalDocStr(contents);
					if(!printNode(node).equals(expected)){
						callback.call(true); //Comparison failed
					}else{
						callback.call(false);
					}
				}catch(Throwable e){
					e.printStackTrace();
					callback.call(true); //something bad happened... so, the test failed!
				}
				
			}
		}.start();
		
	}
}
