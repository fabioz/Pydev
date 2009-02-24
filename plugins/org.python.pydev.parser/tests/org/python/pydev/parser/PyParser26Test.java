package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.visitors.NodeUtils;

public class PyParser26Test extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParser26Test test = new PyParser26Test();
            test.setUp();
            test.testErrorHandled5();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser26Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PyParser.USE_FAST_STREAM = true;
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_2_6);
    }
    
    public void testWith(){
        String str = "def m1():\n" +
        		"    with a:\n" +
        		"        print a\n" +
        		"\n" +
        		"";
        parseLegalDocStr(str);
    }

    public void testExceptAs(){
        String str = "" +
        "try:\n" +
        "    a = 10\n" +
        "except RuntimeError as x:\n" +
        "    print x\n" +
        "";
        parseLegalDocStr(str);
    }
    
    public void testBinaryObj(){
        String str = "" +
        "b'foo'\n" +
        "";
        parseLegalDocStr(str);
    }
    
    public void testOctal(){
        String str = "" +
        "0o700\n" +
        "0700\n" +
        "";
        assertEquals("Module[body=[Expr[value=Num[n=448, type=Int, num=0o700]], Expr[value=Num[n=448, type=Int, num=0700]]]]",
                parseLegalDocStr(str).toString());
    }
    
    
    public void testFunctionCall(){
        String str = "" +
        "Call(1,2,3, *(4,5,6), keyword=13, **kwargs)\n" +
        "";
        parseLegalDocStr(str);
    }
    
    public void testFunctionCallWithListComp(){
        String str = "" +
        "any(cls.__subclasscheck__(c) for c in [subclass, subtype])\n" +
        "";
        parseLegalDocStr(str);
    }
    
    public void testClassDecorator() {
        String s = "" +
                "@classdec\n" +
                "@classdec2\n" +
                "class A:\n" +
                "    pass\n" +
                "";
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        ClassDef d = (ClassDef) m.body[0];
        assertEquals(2, d.decs.length);
        assertEquals("classdec", NodeUtils.getRepresentationString(d.decs[0].func));
        assertEquals("classdec2", NodeUtils.getRepresentationString(d.decs[1].func));        
    }
    
    
    public void testSuccessWithError() {
        String s = 
            "class A:\n" +
            "    def method1(self, *args, **kwargs):\n" +
            "        "; 

            
        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
        Module m = (Module) tup.o1;
        ClassDef c = (ClassDef) m.body[0];
        FunctionDef func = (FunctionDef) c.body[0];
        assertEquals("method1", NodeUtils.getRepresentationString(func));
    }
    
    
    public void testCommonForCodeCompletion() {
        String s = 
            "a = 10\n" +
            "a."; 
        
        
        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
        Module m = (Module) tup.o1;
        Assign assign = (Assign) m.body[0];
        assertNotNull(assign);
        Expr expr = (Expr) m.body[1];
        Attribute attr = (Attribute)expr.value;
        assertEquals("a.!<MissingName>!", NodeUtils.getFullRepresentationString(attr));
    }
    
    public void testErrorHandled() {
        String s = ""+
            "class C:             \n" +  
            "                     \n" +    
            "    def makeit(self):\n" +     
            "        pass         \n" +     
            "                     \n" +       
            "class D(C.:          \n" +  
            "                     \n" +    
            "    def a(self):     \n" +   
            "        pass         \n";        
        
        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
        Module m = (Module) tup.o1;
        ClassDef d = (ClassDef) m.body[1];
        assertEquals("D", NodeUtils.getRepresentationString(d));
    }
    
    
    public void testErrorHandled2() {
        String s = ""+
        "class Test(unit \n" +
        "                \n" +
        "    def meth1():\n" +
        "        pass    \n";
        
        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
        Module m = (Module) tup.o1;
        assertEquals(1, m.body.length);
        ClassDef c = (ClassDef) m.body[0];
        assertEquals(1, c.body.length);
        FunctionDef f = (FunctionDef) c.body[0];
        assertEquals("meth1", NodeUtils.getRepresentationString(f));
    }
    
    
    public void testErrorHandled3() {
        String s = ""+
        "class Test(unit \n" +
        "                \n" +
        "def meth1():\n" +
        "    pass    \n";
        
        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
        Module m = (Module) tup.o1;
        assertEquals(2, m.body.length);
        ClassDef c = (ClassDef) m.body[0];
        assertEquals("Test", NodeUtils.getRepresentationString(c));
        FunctionDef f = (FunctionDef) m.body[1];
        assertEquals("meth1", NodeUtils.getRepresentationString(f));
    }
    
    
    public void testErrorHandled4() {
        String s = 
            "class A:\n" +
            "    def method1(self, *args, **kwargs):\n" +
            "        "; 
        
        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
        Module m = (Module) tup.o1;
        assertEquals(1, m.body.length);
        ClassDef c = (ClassDef) m.body[0];
        assertEquals("A", NodeUtils.getRepresentationString(c));
        FunctionDef f = (FunctionDef) c.body[0];
        assertEquals("method1", NodeUtils.getRepresentationString(f));
    }
    
    public void testErrorHandled5() {
        String s = 
            "import Imp\n" +
            "\n" +
            "eu s\n" +
            ""; 
        
        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
        Module m = (Module) tup.o1;
        assertTrue(m.body.length > 0);
        Import c = (Import) m.body[0];
        assertEquals("Imp", NodeUtils.getRepresentationString(c.names[0]));
    }
    

}
