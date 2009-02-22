/*
 * Created on Feb 11, 2006
 */
package org.python.pydev.parser.prettyprinter;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.commentType;


public class PrettyPrinter30Test extends AbstractPrettyPrinterTestBase{

    public static void main(String[] args) {
        try {
            DEBUG = true;
            PrettyPrinter30Test test = new PrettyPrinter30Test();
            test.setUp();
            test.testIf3();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PrettyPrinter30Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
    }

    public void testMetaClass() throws Exception {
        String s = "" +
        "class IOBase(metaclass=abc.ABCMeta):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    

    public void testMetaClass2() throws Exception {
        String s = "" +
        "class IOBase(object,*args,metaclass=abc.ABCMeta):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    
    
    public void testIf() throws Exception {
        String s = "" +
        "if a:\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testIf2() throws Exception {
        String s = "" +
        "if a:\n" +
        "    pass\n" +
        "elif b:\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testIf3() throws Exception {
        String s = "" +
        "if a:\n" +
        "    pass\n" +
        "elif b:\n" +
        "    pass\n" +
        "elif c:\n" +
        "    pass\n" +
        "else:\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testMetaClass3() throws Exception {
        String s = "" +
        "class B(*[x for x in [object]]):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testAnnotations() throws Exception {
        String s = "" +
        "def seek(self,pos,whence)->int:\n" +
        "    pass\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testAnnotations2() throws Exception {
        String s = "" +
        "def seek(self,pos:int,whence:int)->int:\n" +
        "    pass\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testAnnotations3() throws Exception {
        String s = "" +
        "def seek(self,pos:int,whence:int,*args:list,foo:int=10,**kwargs:dict)->int:\n" +
        "    pass\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testAnnotations4() throws Exception {
        String s = "" +
        "def seek(whence:int,*,foo:int=10):\n" +
        "    pass\n";
        checkPrettyPrintEqual(s);
    }

    public void testLambdaArgs2() throws Exception {
        String s = "a = lambda self,a,*,xx=10,yy=20:1\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testLambdaArgs3() throws Exception {
        String s = "a = lambda self,a,*args,xx=10,yy=20:1\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testFuncCall() throws Exception {
        String s = "Call(1,2,3,*(4,5,6),keyword=13)\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testClassDecorator() throws Exception {
        String s = "" +
        "@classdec\n" +
        "@classdec2\n" +
        "class A:\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testSetComprehension() throws Exception {
        String s = "" +
        "namespace = {'a':1,'b':2,'c':1,'d':1}\n" +
        "abstracts = {name for name,value in namespace.items() if value == 1}\n" +
        "print(abstracts)\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testDictComprehension() throws Exception {
        String s = "" +
        "namespace = {'a':1,'b':2,'c':1,'d':1}\n" +
        "abstracts = {name:value for name,value in namespace.items() if value == 1}\n" +
        "print(abstracts)\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testSet() throws Exception {
        String s = "" +
        "namespace = {'a','b','c','d'}\n" +
        "print(abstracts)\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testRaiseFrom() throws Exception {
        String s = "" +
        "try:\n" +
        "    print(a)\n" +
        "except Exception as e:\n" +
        "    raise SyntaxError() from e\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testMisc() throws Exception {
        String s = "" +
        "class ABCMeta(type):\n" +
        "    _abc_invalidation_counter = 0\n" +
        "    def __new__(mcls,name,bases,namespace):\n" +
        "        cls = super().__new__(mcls,name,bases,namespace)\n" +
        "        # Compute set of abstract method names\n" +
        "        abstracts = {name for name,value in namespace.items() if getattr(value,'__isabstractmethod__',False)}\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testMethodDef() throws Exception {
        String s = "" +
        "def _dump_registry(cls,file=None):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testMethodDef2() throws Exception {
        String s = "" +
        "def _set_stopinfo(stoplineno=-1):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    
    public void testMethodDef3() throws Exception {
        String s = "" +
        "def _set_stopinfo(lnum=[arg]):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testExec() throws Exception {
        String s = "" +
        "try:\n" +
        "    exec(cmd,globals,locals)\n" +
        "except BdbQuit:\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testExec2() throws Exception {
        String s = "" +
        "try:\n" +
        "    exec(cmd,globals,locals)\n" +
        "except BdbQuit:\n" +
        "    pass\n" +
        "finally:\n" +
        "    self.quitting = 1\n" +
        "    sys.settrace(None)\n" +
        "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept() throws Exception {
        String s = "" +
        "try:\n" +
        "    a = 10\n" +
        "except BdbQuit:\n" +
        "    b = 10\n" +
        "else:\n" +
        "    c = 10\n" +
        "finally:\n" +
        "    d = 10\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testComment() throws Exception {
        String s = "" +
        		"def __enter__(self)->'IOBase':#That's a forward reference\n" +
        		"    pass\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testListComp() throws Exception {
        String s = "" +
        "lines = [line if isinstance(line,str) else str(line,coding) for line in lines]\n";
        checkPrettyPrintEqual(s);
    }
    
    public void testNewIf() throws Exception {
        String s = "" +
        "j = stop if (arg in gets) else start\n"+
        "";
        checkPrettyPrintEqual(s);
        
    }
    
    public void testEndWithComment() {
        String s = 
                "class C: \n" +
                "    pass\n" +
                "#end\n" +
                "";
        Module ast = (Module) parseLegalDocStr(s);
        ClassDef d = (ClassDef) ast.body[0];
        assertEquals(1, d.specialsAfter.size());
        commentType c = (commentType) d.specialsAfter.get(0);
        assertEquals("#end", c.id);
        
    }
    
    public void testOnlyComment() {
        String s = 
            "#end\n" +
            "\n" +
            "";
        Module ast = (Module) parseLegalDocStr(s);
        assertEquals(1, ast.specialsBefore.size());
        commentType c = (commentType) ast.specialsBefore.get(0);
        assertEquals("#end", c.id);
        
    }
    
}
