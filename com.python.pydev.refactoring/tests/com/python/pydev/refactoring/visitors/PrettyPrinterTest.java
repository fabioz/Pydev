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
            test.testDefaults2();
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
    }
    

    public void testDefaults() throws Exception {
        String s = ""+
        "def defaults(hi=None):\n" +
        "    if False:\n" +
        "        pass\n" +
        "    ";
        checkPrettyPrintEqual(s);
        
    }
    public void testDefaults2() throws Exception {
        String s = ""+
        "def defaults(a,x,lo=foo,hi=None):\n" +
        "    if hi is None:\n" +
        "        hi = a\n" +
        "    ";
        checkPrettyPrintEqual(s);
        
    }
    public void testNoComments() throws Exception {
        String s = ""+
        "class Class1:\n" +
        "    def met1(self,a):\n" +
        "        pass\n" +
        "        ";
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
        "        ";
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
        "        ";
        checkPrettyPrintEqual(s);
    }
    
    public void testDocStrings3() throws Exception {
        String s = ""+
        "class Class1:\n" +
        "    def met1(self,a):\n" +
        "        ur'unicoderaw' + 'foo'\n" +
        "        ";
        checkPrettyPrintEqual(s);
    }
    
    public void testIfElse() throws Exception {
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
    	String s = "#commentbefore\n"+
    	"if a:#commentIf\n"+
    	"    a = 1\n"+
    	"elif b:#commentElif\n"+
    	"    b = 2\n"+
    	"elif c:\n"+
    	"    c = 3\n"+
    	"else:#commentElse\n"+
    	"    d = 4\n" +
    	"outOfIf = True\n";
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
        "        ";
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
        "        ";
        checkPrettyPrintEqual(s);
    }
    
    public void testComments2() throws Exception {
        String s = ""+
        "class Foo(object):#test comment\n" +
        "    def m1(self,a,#c1\n" +
        "        b):#c2\n" +
        "        pass\n" +
        "        ";
        checkPrettyPrintEqual(s);
        
    }


}
