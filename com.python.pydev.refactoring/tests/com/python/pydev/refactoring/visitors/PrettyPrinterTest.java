/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Module;
import org.python.pydev.parser.PyParserTestBase;

public class PrettyPrinterTest  extends PyParserTestBase{

    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        try {
            PrettyPrinterTest test = new PrettyPrinterTest();
            test.setUp();
            test.testWhileElse();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PrettyPrinterTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PrettyPrinterPrefs prefs;
    

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        prefs = new PrettyPrinterPrefs("\n");
    }

    /**
     * @param s
     * @throws Exception
     * @throws IOException
     */
    protected void checkPrettyPrintEqual(String s) throws Exception, IOException {
        SimpleNode node = parseLegalDocStr(s);
        Module m = (Module) node;
        
        final WriterEraser stringWriter = new WriterEraser();
		PrettyPrinter printer = new PrettyPrinter(prefs, stringWriter);
        m.accept(printer);
        if(DEBUG){
            System.out.println("\n\nResult:\n");
            System.out.println("'"+stringWriter.getBuffer().toString()+"'");
        }
        assertEquals(s, stringWriter.getBuffer().toString());
        assertTrue(! printer.state.inStmt());
        assertTrue("Should not be in record:"+printer.auxComment, ! printer.auxComment.inRecord());
    }
    
//    public void test() throws Exception {
//        String s = ""+
//        "\n" +
//        "";
//        checkPrettyPrintEqual(s);
//    }
    
    
    public void testWhile() throws Exception {
        String s = ""+
        "while True:\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testWhile2() throws Exception {
        String s = ""+
        "while ((a + 1 < 0)):#comment\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testWhileElse() throws Exception {
        String s = ""+
        "while True:\n" +
        "    pass\n" +
        "else:\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    

    
    public void testTryExceptRaise() throws Exception {
        String s = ""+
        "try:\n" +
        "    print 'foo'\n" +
        "except:\n" +
        "    raise\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testTryExcept() throws Exception {
        String s = ""+
        "try:\n" +
        "    print 'foo'\n" +
        "except:\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testTryExcept2() throws Exception {
        String s = ""+
        "try:\n" +
        "    socket_map\n" +
        "except NameError:\n" +
        "    socket_map = {}\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testTryExcept3() throws Exception {
        String s = ""+
        "try:\n" +
        "    print 'foo'\n" +
        "except (NameError,e):\n" +
        "    print 'err'\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testTryExcept4() throws Exception {
        String s = ""+
        "try:\n" +
        "    print 'foo'\n" +
        "except (NameError,e):\n" +
        "    print 'err'\n" +
        "else:\n" +
        "    print 'else'\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testTryExcept5() throws Exception {
        String s = ""+
        "try:\n" +
        "    print 'foo'\n" +
        "except (NameError,e):\n" +
        "    print 'name'\n" +
        "except (TypeError,e2):\n" +
        "    print 'type'\n" +
        "else:\n" +
        "    print 'else'\n" +
        "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept6() throws Exception {
        String s = ""+
        "def read(obj):\n" +
        "    try:\n" +
        "        obj.handle_read_event()\n" +
        "    except:\n" +
        "        obj.handle_error()\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testCall() throws Exception {
        String s = ""+
        "callIt(1)\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testCall2() throws Exception {
        String s = ""+
        "callIt(1#param1\n" +
        "    )\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    
    public void testCall3() throws Exception {
        String s = ""+
        "callIt(a=2)\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testCall4() throws Exception {
        String s = ""+
        "callIt(a=2,*args,**kwargs)\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    
    public void testCall5() throws Exception {
        String s = ""+
        "m1(a,#d1\n" +
        "    b,#d2\n" +
        "    c#d3\n" +
        "    )\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testIfElse() throws Exception {
        String s = ""+
        "if True:\n" +
        "    if foo:\n" +
        "        pass\n" +
        "    else:\n" +
        "        pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testListDict() throws Exception {
        String s = ""+
        "a = [1,#this is 1\n" +
        "2]\n" +
        "a = {1:'foo'}\n" +
        "";
        checkPrettyPrintEqual(s);
        
    }
    
    public void testTupleDict() throws Exception {
        String s = ""+
        "a = (1,#this is 1\n" +
        "2)\n" +
        "a = {1:'foo'}\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testDict2() throws Exception {
        String s = ""+
        "a = {1:2,#this is 1\n" +
        "2:2}\n" +
        "a = {1:'foo'}\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testVarious() throws Exception {
        String s = ""+
        "class Foo:\n" +
        "    def __init__(self,a,b):\n" +
        "        print self,#comment0\n" +
        "        a,b\n" +
        "    def met1(self,a):#ok comment1\n" +
        "        a,b\n" +
        "        class Inner(object):\n" +
        "            pass\n" +
        "        self.met1(a)\n" +
        "print 'ok'\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testYield() throws Exception {
        String s = ""+
        "def foo():\n" +
        "    yield 10\n" +
        "print 'foo'\n" +
        "a = 3\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testYield2() throws Exception {
        String s = ""+
        "def foo():\n" +
        "    yield (10)#comment1\n" +
        "    print 'foo'\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testYield3() throws Exception {
    	String s = ""+
    	"def foo():\n" +
    	"    #comment0\n" +
    	"    print 'foo'\n" +
    	"";
    	checkPrettyPrintEqual(s);
    }
    
    
    public void testPrint() throws Exception {
    	String s = ""+
    	"print >> a,'foo'\n" +
    	"";
    	checkPrettyPrintEqual(s);
    }
    
    public void testPrintComment() throws Exception {
    	String s = ""+
		"def test():#comm1\n" +
		"    print >> (a,#comm2\n" +
		"    'foo')#comm3\n" +
    	"";
    	checkPrettyPrintEqual(s);
    }
    
    public void testAttr() throws Exception {
        String s = ""+
        "print a.b\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testAttr2() throws Exception {
        String s = ""+
        "print a.b.c.d\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testAttr3() throws Exception {
        String s = ""+
        "print a.d()\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testAttrCall() throws Exception {
        String s = ""+
        "print a.d().e(1 + 2)\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testSubscript() throws Exception {
    	String s = ""+
    	"print a[0]\n" +
    	"";
    	checkPrettyPrintEqual(s);
    }
    
    public void testDefaults() throws Exception {
        String s = ""+
        "def defaults(hi=None):\n" +
        "    if False:\n" +
        "        pass\n" +
        "";
        checkPrettyPrintEqual(s);
        
    }
    public void testDefaults2() throws Exception {
        String s = ""+
        "def defaults(a,x,lo=foo,hi=None):\n" +
        "    if hi is None:\n" +
        "        hi = a\n" +
        "";
        checkPrettyPrintEqual(s);
        
    }
    public void testNoComments() throws Exception {
        String s = ""+
        "class Class1:\n" +
        "    def met1(self,a):\n" +
        "        pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testDocStrings() throws Exception {
    	String s = ""+
    	"class Class1:\n" +
    	"    '''docstring1'''\n" +
    	"    a = '''str1'''\n" +
    	"    def met1(self,a):\n" +
    	"        '''docstring2\n" +
    	"        foo\n" +
    	"        '''\n" +
    	"        pass\n" +
        "";
    	checkPrettyPrintEqual(s);
    }
    
    public void testDocStrings2() throws Exception {
        String s = ""+
        "class Class1:\n" +
        "    \"\"\"docstring1\"\"\"\n" +
        "    a = 'str1'\n" +
        "    def met1(self,a):\n" +
        "        \"docstring2\"\n" +
        "        ur'unicoderaw'\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testDocStrings3() throws Exception {
        String s = ""+
        "class Class1:\n" +
        "    def met1(self,a):\n" +
        "        ur'unicoderaw' + 'foo'\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testDict() throws Exception {
        String s = ""+
        "if a:\n"+
        "    a = {a:1,b:2,c:3}\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testList() throws Exception {
        String s = ""+
        "if a:\n"+
        "    a = [a,b,c]\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testTuple() throws Exception {
        String s = ""+
        "if a:\n"+
        "    a = (a,b,c)\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testTuple2() throws Exception {
        String s = ""+
        "if a:\n"+
        "    a = (a,b,#comment\n" +
        "    c)\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testIfElse0() throws Exception {
        String s = ""+
        "if a:\n"+
        "    a = 1\n"+
        "elif b:\n"+
        "    b = 2\n"+
        "elif c:\n"+
        "    c = 3#foo\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testIfElse1() throws Exception {
        String s = ""+
        "if a:\n"+
        "    a = 1\n"+
        "elif b:\n"+
        "    b = 2\n"+
        "elif c:\n"+
        "    c = 3\n"+
        "else:\n"+
        "    d = 4\n";
        checkPrettyPrintEqual(s);
    }
    public void testIfElse2() throws Exception {
    	String s = ""+
    	"if a:\n"+
    	"    a = 1#comment1\n"+
    	"elif b:\n"+
    	"    b = 2#comment2\n"+
    	"elif c:\n"+
    	"    c = 3#comment3\n"+
    	"else:\n"+
    	"    d = 4#comment4\n";
    	checkPrettyPrintEqual(s);
    }
    
    public void testIfElse3() throws Exception {
    	String s = 
    	"#commentbefore\n"+      //1
    	"if a:#commentIf\n"+     //2
    	"    a = 1\n"+           //3
    	"elif b:#commentElif\n"+ //4
    	"    b = 2\n"+           //5
    	"elif c:\n"+             //6
    	"    c = 3\n"+           //7
    	"else:#commentElse\n"+   //8
    	"    d = 4\n" +          //9
    	"outOfIf = True\n";      //10
    	checkPrettyPrintEqual(s);
    }
    

    public void testPlus() throws Exception {
        String s = ""+
        "a = 1 + 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testMinus() throws Exception {
        String s = ""+
        "a = 1 - 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testPow() throws Exception {
        String s = ""+
        "a = 1 ** 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testLShift() throws Exception {
        String s = ""+
        "a = 1 << 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testRShift() throws Exception {
        String s = ""+
        "a = 1 >> 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testBitOr() throws Exception {
        String s = ""+
        "a = 1 | 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testBitXOr() throws Exception {
        String s = ""+
        "a = 1 ^ 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testBitAnd() throws Exception {
        String s = ""+
        "a = 1 & 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testFloorDiv() throws Exception {
        String s = ""+
        "a = 1 // 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testNoComments2() throws Exception {
        prefs.setSpacesAfterComma(1);
        String s = ""+
        "class Class1(obj1, obj2):\n" +
        "    def met1(self, a, b):\n" +
        "        pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testAssign() throws Exception {
        String s = ""+
        "a = 1\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testAssign2() throws Exception {
        String s = ""+
        "a = 1#comment\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testComments1() throws Exception {
        String s = "#comment00\n" +
        "class Class1:#comment0\n" +
        "    #comment1\n" +
        "    def met1(self,a):#comment2\n" +
        "        pass#comment3\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testComments2() throws Exception {
        String s = ""+
        "class Foo(object):#test comment\n" +
        "    def m1(self,a,#c1\n" +
        "        b):#c2\n" +
        "        pass\n" +
        "";
        checkPrettyPrintEqual(s);
        
    }


}
