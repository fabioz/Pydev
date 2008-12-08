package org.python.pydev.parser;

import java.io.File;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
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
            test.testMetaClass3();
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
    
    
    public void testMetaClass() {
        String s = "" +
        "class IOBase(metaclass=abc.ABCMeta):\n" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMetaClass2() {
        String s = "" +
        "class IOBase(**kwargs):\n" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMetaClass3() {
        String s = "" +
        "class IOBase(object, *args, metaclas=abc.ABCMeta):\n" +
        "    pass\n" +
        "";
//        parseLegalDocStr(s);
    }
    
    public void testReprNotAccepted() {
        String s = "" +
        "`error`\n" +
        "";
        parseILegalDocStr(s);
    }
    
    
    public void testAnnotations() {
        String s = "" +
        "def seek(self, pos, whence) -> int:\n" +
        "    pass";
        parseLegalDocStr(s);
    }
    
    public void testAnnotations2() {
        String s = "" +
        "def seek(self, pos: int, whence: int) -> int:\n" +
        "    pass";
        parseLegalDocStr(s);
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
    
    public void testMethodDef() {
        String s = "def _dump_registry(cls, file=None):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMethodDef2() {
        String s = "def _dump_registry(cls, file=None, *args, **kwargs):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    
    public void testMethodDef3() {
        String s = "def _dump_registry(cls, file=None, *args:list, **kwargs:dict):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    
    public void testMethodDef4() {
        String s = "def initlog(*allargs):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMethodDef5() {
        String s = "def initlog(**allargs):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMethodDef6() {
        String s = "def iterencode(iterator, encoding, errors='strict', **kwargs):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMethodDef7() {
        String s = "def __init__(self,):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMethodDef8() {
        String s = "def __init__(self, a, *, xx:int=10, yy=20):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMethodDef9() {
        String s = "def __init__(self, a, *args, xx:int=10, yy=20):" +
        "    pass\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testLambdaArgs2() {
        String s = "a = lambda self, a, *, xx=10, yy=20:1" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testLambdaArgs3() {
        String s = "a = lambda self, a, *args, xx=10, yy=20:1" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testMisc() {
        String s = "" +
        		"def __init__(self, dirname, factory=None, create=True):\n" +
        		"    '''Initialize a Maildir instance.'''\n" +
        		"    os.mkdir(self._path, 0o700)\n" +
        		"\n" +
        		"\n" +
        		"";
        parseLegalDocStr(s);
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
    
    public void testFuncCall() {
        String s = "Call()\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testFuncCall2() {
        String s = "Call(a)\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testFuncCall3() {
        String s = "Call(a, *b)\n" +
        "";
        parseLegalDocStr(s);
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
    
    
    public void testImportAndClass() {
        String s = "" +
        "from a import b\n" +
        "class C(A):\n" +
        "    pass\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testDictDecl() {
        String s = "" +
        "a = {a:1, b:2,}\n" +
        "\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testWithStmt() {
        String s = "" +
        "with a:\n" +
        "    print(a)\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testRaiseFrom() {
        String s = "" +
        "try:\n" +
        "    print(a)\n" +
        "except Exception as e:\n" +
        "    raise SyntaxError() from e" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testLambdaArgs() {
        String s = "" +
        "a = lambda b=0: b+1" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testOctal() {
        String s = "" +
        "0o700" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testInvalidOctal() {
        String s = "" +
        "0700" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testNonLocalAndShortcuts() {
        String s = "" +
        "def m1():\n" +
        "    a = 20\n" +
        "    def m2():\n" +
        "        nonlocal a = 30\n" +
        "        global x = 30\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testFunctionDecorated() {
        String s = "" +
        "from a import b\n" +
        "@dec1\n" +
        "def func(A):\n" +
        "    pass\n" +
        "\n" +
        "";
        SimpleNode ast = parseLegalDocStr(s);
        assertEquals("Module[body=[ImportFrom[module=NameTok[id=a, ctx=ImportModule], names=[alias[name=NameTok[id=b, ctx=ImportName], asname=null]], level=0], FunctionDef[name=NameTok[id=func, ctx=FunctionName], args=arguments[args=[Name[id=A, ctx=Param, reserved=false]], vararg=null, kwarg=null, defaults=[null]], body=[Pass[]], decs=[decorators[func=Name[id=dec1, ctx=Load, reserved=false], args=[], keywords=[], starargs=null, kwargs=null]], returns=null]]]", 
                ast.toString());
    }
    
    
    public void testLib() throws Exception {
        parseFilesInDir(new File(TestDependent.PYTHON_30_LIB), false);
    }


}
