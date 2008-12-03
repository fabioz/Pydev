package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Set;
import org.python.pydev.parser.jython.ast.SetComp;
import org.python.pydev.parser.visitors.NodeUtils;

public class PyParser30Test extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParser30Test test = new PyParser30Test();
            test.setUp();
            test.testSetCreation();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser30Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PyParser.USE_FAST_STREAM = true;
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_0);
    }

    public void testTryExceptAs() {
        String s = "" +
        "try:\n" +
        "    print('10')\n" +
        "except RuntimeError as e:\n" +
        "    print('error')\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testWrongPrint() {
        String s = "" +
        "print 'error'\n" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testBytes() {
        String s = "" +
        "a = b'error'\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testReprNotAccepted() {
        String s = "" +
        "`error`\n" +
        "";
        parseILegalDocStr(s);
    }
    
    
    public void testNoLessGreater() {
        String s = "a <> b" +
        "\n" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testNoAssignToFalse() {
        String s = "False = 1" +
        "\n" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testNoAssignToTrue() {
        String s = "True = 1" +
        "\n" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testNoAssignToNone() {
        String s = "None = 1" +
        "\n" +
        "";
        parseILegalDocStr(s);
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
    
    
    public void testSetComprehension() {
        String s = "" +
        "namespace = {'a':1, 'b':2, 'c':1, 'd':1}\n" +
        "abstracts = {name\n" +
        "             for name, value in namespace.items()\n" +
        "             if value==1}\n" +
        "print(abstracts)\n" +
        "\n" +
        "";
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign a0 = (Assign) m.body[0];
        Assign a1 = (Assign) m.body[1];
        assertTrue(a0.value instanceof Dict); 
        SetComp setComp = (SetComp) a1.value;
        
        assertEquals("name", ((Name)setComp.elt).id);
    }
    
    
    public void testSetCreation() {
        String s = "" +
        "namespace = {1, 2, 3, 4}\n" +
        "";
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign a0 = (Assign) m.body[0];
        assertTrue(a0.value instanceof Set); 
        assertEquals("Assign[targets=[Name[id=namespace, ctx=Store, reserved=false]], value=" +
        		"Set[elts=[Num[n=1, type=Int, num=1], Num[n=2, type=Int, num=2], " +
        		"Num[n=3, type=Int, num=3], Num[n=4, type=Int, num=4]]]]", a0.toString());
    }
    
    
    public void testDictComprehension() {
        String s = "" +
        "namespace = {'a':1, 'b':2, 'c':1, 'd':1}\n" +
        "abstracts = {name: value\n" +
        "             for name, value in namespace.items()\n" +
        "             if value==1}\n" +
        "print(abstracts)\n" +
        "\n" +
        "";
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign a0 = (Assign) m.body[0];
        Assign a1 = (Assign) m.body[1];
        assertTrue(a0.value instanceof Dict); 
        DictComp dictComp = (DictComp) a1.value;
        
        assertEquals("name", ((Name)dictComp.key).id);
        assertEquals("value", ((Name)dictComp.value).id);
    }
    
    
//    public void testLib() throws Exception {
//        parseFilesInDir(new File(TestDependent.PYTHON_30_LIB), false);
//    }


}
