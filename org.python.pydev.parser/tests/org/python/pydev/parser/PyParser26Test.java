package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.visitors.NodeUtils;

public class PyParser26Test extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParser26Test test = new PyParser26Test();
            test.setUp();
            test.testExceptAs();
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
    
    
    public void testFunctionCall(){
        String str = "" +
        "Call(1,2,3, *(4,5,6), keyword=13)\n" +
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
    
    

}
