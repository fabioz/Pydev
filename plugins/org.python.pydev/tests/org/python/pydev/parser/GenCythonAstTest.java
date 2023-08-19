package org.python.pydev.parser;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.cython.GenCythonAstImpl;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.comparator.DifferException;
import org.python.pydev.parser.visitors.comparator.SimpleNodeComparator;
import org.python.pydev.parser.visitors.comparator.SimpleNodeComparator.LineColComparator;
import org.python.pydev.parser.visitors.comparator.SimpleNodeComparator.RegularLineComparator;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.string.StringUtils;

public class GenCythonAstTest extends CodeCompletionTestsBase {

    IGrammarVersionProvider grammarVersionProvider = new IGrammarVersionProvider() {

        @Override
        public int getGrammarVersion() throws MisconfigurationException {
            // Note: this is used in reparseDocument but not when generating the cython ast as we call the internal implementation.
            return IPythonNature.GRAMMAR_PYTHON_VERSION_3_8;
        }

        @Override
        public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
            return null;
        }

    };

    @Override
    protected boolean isPython3Test() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        GenCythonAstImpl.IN_TESTS = true;
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);

        //        CorePlugin.setBundleInfo(new BundleInfoStub());
        //
        //        final InterpreterInfo info = new InterpreterInfo("3.7", TestDependent.PYTHON_EXE, new ArrayList<String>());
        //
        //        IEclipsePreferences preferences = new InMemoryEclipsePreferences();
        //        final PythonInterpreterManager manager = new PythonInterpreterManager(preferences);
        //        InterpreterManagersAPI.setPythonInterpreterManager(manager);
        //        manager.setInfos(new IInterpreterInfo[] { info }, null, null);

    }

    public void compareNodes(ISimpleNode parserNode, ISimpleNode cythonNode, LineColComparator lineColComparator)
            throws DifferException, Exception {
        SimpleNodeComparator simpleNodeComparator = new SimpleNodeComparator(lineColComparator);
        System.out.println("Internal:");
        System.out.println(parserNode);
        System.out.println("Cython:");
        System.out.println(cythonNode);
        simpleNodeComparator.compare((SimpleNode) parserNode, (SimpleNode) cythonNode);

        assertEquals(cythonNode.toString(), parserNode.toString());
    }

    public void testGenCythonFromCythonTests() throws Exception {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        // i.e.: This test is local-only...
        File cythonTestCompileDir = new File("X:\\cython\\");
        assertTrue(cythonTestCompileDir.isDirectory());
        FileUtils.visitDirectory(cythonTestCompileDir, true, (Path path) -> {
            String p = path.toString();
            if (p.endsWith(".py") || p.endsWith(".pyx") || p.endsWith(".pxd")) {
                System.out.println("Visiting: " + p);
                String s = FileUtils.getFileContents(path.toFile());
                try {
                    ParserInfo parserInfoCython = new ParserInfo(new Document(s), grammarVersionProvider);
                    ParseOutput cythonParseOutput = new GenCythonAstImpl(parserInfoCython).genCythonAst();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        });
    }

    public void testGenCythonAstCases() throws Exception {
        String[] cases = new String[] {
                "def method(a, *, b):pass",
                "@dec1\n@dec2\ndef method():pass",
                "@dec\ndef method():pass",
                "assert 1, 'ra'",
                "def method(a:int): return 1",
                "import a.b as c",
                "import a\n"
                        + "\n"
                        + "import b\n",
                "def method(*args, **kwargs):\n"
                        + "    return f(*args, **modify(kwargs))",
                "{tuple(call(n) for n in (1, 2) if n == 2)}",
                "{tuple(call(n) for n in (1, 2))}",
                "[a for b in c if d]",
                "{i: j for i, j in a}",
                "{a, *c, d, *[1,2], [3, 4]}",
                "[a, *c, d]",
                "*a, b = [1, 2, 4]",
                "def foo():\n"
                        + "  yield from bar",
                "a = lambda x:y",
                "a = call(foo, foo=bar, **xx.yy)",

                "foo[:, b:c, d:e, f:g] = []",
                "foo[:] = []",
                "try:\n"
                        + "  a = 10\n"
                        + "except TypeError as e:\n"
                        + "  raise e from b",
                "try:\n"
                        + "  a = 10\n"
                        + "except (Exception, TypeError) as exc:\n"
                        + "  raise",
                "try:\n"
                        + "  a = 10\n"
                        + "except TypeError:\n"
                        + "  raise",
                "try:\n"
                        + "  a = 10\n"
                        + "except TypeError as e:\n"
                        + "  raise e",
                "try:\n"
                        + "  a = 10\n"
                        + "except:\n"
                        + "  raise",
                "a = -b",
                "a = ~b",
                "a = +b",
                "a = not b",
                "try:\n  pass\nfinally:\n  pass",
                "b = ...",
                "del a",
                "with foo:\n  pass",
                "with nogil:\n  pass",
                "with foo as bar:\n  pass",
                "with foo:\n  pass",
                "b[1:2:3]",
                "b[1] = 10",
                "b[1]",
                "def method():\n    global b\n    b=10",
                "def method():\n    nonlocal b\n    b=10",
                "for i in range(10): break",
                "for i in range(10): continue",
                "3.14159",
                "b = a**2",
                "call(b=2, **kwargs)",
                "call(1, b, a=2, c=3, *args)",
                "call(1, *args)",
                "call(*args)",
                "call(1, a=2)",
                "call(1, a=1, b=2, *args, **kwargs)",
                "call(1)",
                "call(**args)",
                "call(a=10)",
                "a.b().c.d()",
                "get_env = os.environ.get",
                "import os\nimport bar\nimport foo",
                "from ..b import *",
                "from ..b import a",
                "from . import a",
                "from a import *",
                "from a import b",
                "from a.b import d as f",
                "from a import b as c",
                "import a",
                "1 | 2 == 0",
                "1 & 2 == 0",
                "1 ^ 2 == 0",
                "a = a + b",
                "a = a - b",
                "a = a * b",
                "a = a / b",
                "a = a @ b",
                "a = a % b",
                "a |= b",
                "a ^= b",
                "a &= b",
                "a += b",
                "a -= b",
                "a *= b",
                "a /= b",
                "a @= b",
                "a %= b",
                "while True:\n  a=10\n  b=20",
                "while True:\n  pass\nelse:\n  a=10\n  b=20",
                "1 if a else b",
                "if b:\n  a=1\nelif c:\n  a=2\nelse:\n  a=3",
                "if a: pass",
                "a = 10",
                "def method():pass",
                "def method(a):pass",
                "def method(a, b):pass",
                "def method(a=1):pass",
                "def method(a=1,b=2):pass",
                "def method():\n    a=10\n    b=20",
                "def method(a=None):pass",
                "def method(a, *b, **c):pass",
                "def method(a=1, *, b=2):pass",
                "def method(a=1, *, b:int=2):pass",
                "call()",
                "call(1, 2, b(True, False))",
                "call(u'nth')",
                "call(b'nth')",
                "call('nth')",
                "@dec()\ndef method():pass",
                "class A:pass",
                "class A(set, object):pass",
                "class A((set, object)):pass",
                "@dec\nclass A((set, object)):pass",
                "async def foo(): pass",
                "def foo():yield 1",
                "a > b",
                "a or b",
                "a or b and c",
                "a > b < c != d >= 3 <= (d == 4, a == 3 or (1 in () and 2 not in (), a is 1 and b is not 2))",
                "[]",
                "[1, 3]",
                "{}",
                "{1:2, 3:4}",
                "{1, 2}",
                "for a in b:pass",
                "for a in b:\n    a=1\nelse:    a=2",

        };
        for (String s : cases) {
            compareCase(s);
        }
    }

    public ParseOutput compareCase(String expected) throws DifferException, Exception {
        return this.compareCase(expected, expected);
    }

    public ParseOutput compareCase(String expected, String cython) throws DifferException, Exception {
        try {
            return compareCase(expected, cython, false);
        } catch (Throwable e) {
            final String msg = "Error with cython: " + cython;
            System.err.println(msg);
            throw new RuntimeException(msg, e);
        }
    }

    public ParseOutput compareCase(String expected, String cython, boolean checkCol) throws DifferException, Exception {
        // Suite types usually have a different start line number comparing our own with cython.
        return this.compareCase(expected, cython, new RegularLineComparator() {
            @Override
            public void compareLineCol(SimpleNode node, SimpleNode node2) throws DifferException {
                if (!(node instanceof Suite)) {
                    super.compareLineCol(node, node2);

                    if (checkCol) {
                        if (node instanceof Name || node instanceof NameTok) {
                            if (node.beginColumn != node2.beginColumn) {
                                throw new DifferException(
                                        StringUtils.format("Nodes beginColumn differ. (%s != %s) (%s -- %s)",
                                                node.beginColumn, node2.beginColumn, node, node2));
                            }
                        }
                    }
                }
            }
        });
    }

    public ParseOutput compareCase(String expected, String cython, LineColComparator lineColComparator)
            throws DifferException, Exception {
        ParserInfo parserInfoCython = new ParserInfo(new Document(cython), grammarVersionProvider);
        ParseOutput cythonParseOutput = new GenCythonAstImpl(parserInfoCython).genCythonAst();

        ParserInfo parserInfoInternal = new ParserInfo(new Document(expected), grammarVersionProvider);
        ParseOutput parseOutput = PyParser.reparseDocument(parserInfoInternal);
        if (cythonParseOutput.ast == null) {
            if (cythonParseOutput.error != null) {
                throw new RuntimeException(cythonParseOutput.error);
            }
            throw new RuntimeException("Error parsing: " + cython);
        }

        try {
            compareNodes(parseOutput.ast, cythonParseOutput.ast, lineColComparator);
        } catch (Throwable e) {
            System.err.println("Cython AST pretty-printed to: ");
            System.err.println(NodeUtils.printAst(null, (SimpleNode) cythonParseOutput.ast));
            throw e;
        }
        return cythonParseOutput;
    }

    public void testError() throws Exception {
        String cython = "a b c";
        ParserInfo parserInfoCython = new ParserInfo(new Document(cython), grammarVersionProvider);
        ParseOutput cythonParseOutput = new GenCythonAstImpl(parserInfoCython).genCythonAst();
        assertNotNull(cythonParseOutput.error);
        assertNull(cythonParseOutput.ast);
    }

    public void testAsync() throws Exception {
        String s = ""
                + "async def foo():\n"
                + "    async for a in []:\n"
                + "        a=10\n"
                + "        b=20\n"
                + "        await bar()\n"
                + "    else:\n"
                + "        c=30\n"
                + "        d=40\n"
                + "";
        compareCase(s);
    }

    public void compareWithAst(String code, String expectedAst) throws MisconfigurationException {
        ParserInfo parserInfo = new ParserInfo(new Document(code), grammarVersionProvider);
        ParseOutput cythonParseOutput = new GenCythonAstImpl(parserInfo).genCythonAst();
        assertEquals(expectedAst, cythonParseOutput.ast.toString());
    }

    public void compareWithAst(String code, String[] expectedAstArray) throws MisconfigurationException {
        ParserInfo parserInfo = new ParserInfo(new Document(code), grammarVersionProvider);
        ParseOutput cythonParseOutput = new GenCythonAstImpl(parserInfo).genCythonAst();
        String found = cythonParseOutput.ast.toString();
        for (String s : expectedAstArray) {
            if (s.equals(found)) {
                return;
            }
        }
        throw new AssertionError("Error: generated: " + found + "\n\nDoes not match any of the expected arrays.");
    }

    public void testGenCythonAstCornerCase1() throws Exception {
        compareWithAst("(f'{a}{{}}nth')",
                "Module[body=[Expr[value=Str[s=, type=SingleSingle, unicode=true, raw=false, binary=false, fstring=false, fstring_nodes=[Expr[value=Name[id=a, ctx=Load, reserved=false]], Expr[value=Str[s={}nth, type=SingleSingle, unicode=true, raw=false, binary=false, fstring=false, fstring_nodes=null]]]]]]]");
    }

    public void testGenCythonAstCornerCase2() throws Exception {
        compareCase("import a.b as a", "import a.b");
        compareCase("a = u'>'", "a = c'>'");

        compareCase(
                "\n"
                        + "def foo(int): pass",
                "cdef extern from *:\n" +
                        "    cdef void foo(int[]): pass\n\n",
                false);

        compareCase("b = None", "cimport b");

        compareCase("def const_args(a): pass", "cdef const_args(const int a): pass");

        // We don't resolve cimports, so, don't create imports (which would be unresolved).
        compareCase("b = None", "from mod1 cimport b");

        compareCase("intptr_t(ptr)", "<intptr_t>ptr");

        compareCase("a = None", "a = NULL");

        compareCase("class E:\n  z = 0\n", "cdef enum E:\n  z\n");

        compareCase("a = sizeof()\n", "a = sizeof(OtherStruct[4])\n");
        compareCase("a = sizeof()\n", "a = sizeof(int[23][34])\n");

        compareCase("MyStructP = int\n", "ctypedef unsigned int MyStructP\n");

        compareCase("MyStructP = MyStruct\n", "ctypedef MyStruct* MyStructP\n");

        compareCase("\nclass MyStruct:\n  a = 10\n", "cdef extern from *:\n  struct MyStruct:\n    int a = 10\n");

        compareCase("def foo(a): pass", "cdef void foo(int[] a): pass\n");
        compareCase("def foo(int): pass", "cdef void foo(int[]): pass\n"); // i.e.: we just have the type.

        compareCase("spam_counter = None", "cdef extern int spam_counter");
        compareCase("def foo(a): pass", "cdef int **foo(int* a): pass");

        compareCase("do(g)", "do(&g)");

        compareCase("('ab')", "(r'ab',)"); // there's no indication that it's a raw string...
        compareCase("'ab'", "('a' 'b')"); // "call('a' 'b')", // cython converts to 'ab' internally during parsing.
    }

    public void testGenCythonAstCornerCase3() throws Exception {
        String s = ""
                + "for a in b:\n"
                + "    a=1\n"
                + "else:\n" // internal starts suite here
                + "    a=2\n" // cython starts suite here
                + "    a=4";
        compareCase(s);
    }

    public void testGenCythonAstCornerCase4() throws Exception {
        compareWithAst("@my.dec\nclass A:pass",
                "Module[body=[ClassDef[name=NameTok[id=A, ctx=ClassName], bases=[], body=[Pass[]], decs=[decorators[func=Attribute[value=Name[id=my, ctx=Load, reserved=false], attr=NameTok[id=dec, ctx=Attrib], ctx=Load], args=[], keywords=[], starargs=null, kwargs=null, isCall=false]], keywords=[], starargs=null, kwargs=null]]]");

    }

    public void testGenCythonAstCornerCase6() throws Exception {
        compareWithAst("for i from 0 <= i < 10: pass",
                "Module[body=[For[target=Name[id=i, ctx=Store, reserved=false], iter=null, body=[Pass[]], orelse=null, async=false]]]");

    }

    public void testGenCythonAstCornerCase7() throws Exception {
        compareWithAst("print(10)",
                new String[] {

                        "Module[body=[Print[dest=null, values=[Num[n=10, type=Int, num=10]], nl=true]]]",
                        "Module[body=[Expr[value=Call[func=Name[id=print, ctx=Load, reserved=false], args=[Num[n=10, type=Int, num=10]], keywords=[], starargs=null, kwargs=null]]]]"
                });

    }

    public void testGenCythonAstCornerCase8() throws Exception {
        compareWithAst("with foo as bar, x as y:\n  pass",
                "Module[body=[With[with_item=[WithItem[context_expr=Name[id=foo, ctx=Load, reserved=false], optional_vars=Name[id=bar, ctx=Store, reserved=false]]], body=Suite[body=[With[with_item=[WithItem[context_expr=Name[id=x, ctx=Load, reserved=false], optional_vars=Name[id=y, ctx=Store, reserved=false]]], body=Suite[body=[Pass[]]], async=false]]], async=false]]]");

    }

    public void testGenCythonAstCornerCase9() throws Exception {
        compareWithAst("cdef TemplateTest1[int]* bbbbb = new TemplateTest1[int]()",
                "Module[body=[Assign[targets=[Name[id=bbbbb, ctx=Store, reserved=false]], value=Call[func=Name[id=TemplateTest1, ctx=Load, reserved=false], args=[], keywords=[], starargs=null, kwargs=null], type=null]]]");

    }

    public void testGenCythonAstCornerCase10() throws Exception {
        compareWithAst(""
                + "def f():\n" +
                "    cdef char **a_2d_char_ptr_ptr_array[10][20]\n" +
                "",
                "Module[body=[FunctionDef[decs=null, name=NameTok[id=f, ctx=FunctionName], args=arguments[args=[], vararg=null, kwarg=null, defaults=[], kwonlyargs=[], kw_defaults=[], annotation=[], varargannotation=null, kwargannotation=null, kwonlyargannotation=[]], returns=null, body=[], async=false]]]");
    }

    public void testGenCythonAstCornerCase11() throws Exception {
        compareWithAst(""
                + "ctypedef fused dtype_t_out:\n" +
                "    npy_uint8\n",
                "Module[body=[Assign[targets=[Name[id=dtype_t_out, ctx=Store, reserved=false]], value=Name[id=npy_uint8, ctx=Load, reserved=false], type=null]]]");
    }

    public void testGenCythonAstCornerCase12() throws Exception {
        compareWithAst("cdef extern from \"Python.h\":\n"
                + "  int method(FILE *, const char *)",
                "Module[body=[FunctionDef[decs=null, name=NameTok[id=method, ctx=FunctionName], args=arguments[args=[Name[id=FILE, ctx=Param, reserved=false], Name[id=char, ctx=Param, reserved=false]], vararg=null, kwarg=null, defaults=[null, null], kwonlyargs=[], kw_defaults=[], annotation=[null, null], varargannotation=null, kwargannotation=null, kwonlyargannotation=[]], returns=null, body=null, async=false]]]");
    }

    public void testGenCythonAstCornerCase13() throws Exception {
        compareWithAst("cdef extern from \"Python.h\":\n"
                + "  void remove(const T&)",
                "Module[body=[FunctionDef[decs=null, name=NameTok[id=remove, ctx=FunctionName], args=arguments[args=[Name[id=T, ctx=Param, reserved=false]], vararg=null, kwarg=null, defaults=[null], kwonlyargs=[], kw_defaults=[], annotation=[null], varargannotation=null, kwargannotation=null, kwonlyargannotation=[]], returns=null, body=null, async=false]]]");
    }

    public void testGenCythonAstCornerCase14() throws Exception {
        compareWithAst("2.0j",
                "Module[body=[Expr[value=Num[n=2.0, type=Comp, num=2.0]]]]");
    }

    public void testGenCythonAstCornerCase15() throws Exception {
        compareWithAst("def wrapper(*args, **kwargs):\n" +
                "    return f(*args, more=2, **{**kwargs, 'test': 1})\n",
                "Module[body=[FunctionDef[decs=null, name=NameTok[id=wrapper, ctx=FunctionName], args=arguments[args=[], vararg=NameTok[id=args, ctx=VarArg], kwarg=NameTok[id=kwargs, ctx=KwArg], defaults=[], kwonlyargs=[], kw_defaults=[], annotation=[], varargannotation=null, kwargannotation=null, kwonlyargannotation=[]], returns=null, body=[Return[value=Call[func=Name[id=f, ctx=Load, reserved=false], args=[], keywords=[keyword[arg=NameTok[id=more, ctx=KeywordName], value=Num[n=2, type=Int, num=2], afterstarargs=false]], starargs=Name[id=args, ctx=Load, reserved=false], kwargs=Dict[keys=[Name[id=kwargs, ctx=Load, reserved=false], Str[s=test, type=SingleSingle, unicode=false, raw=false, binary=false, fstring=false, fstring_nodes=null], Num[n=1, type=Int, num=1]], values=[]]]]], async=false]]]");
    }

    public void testGenCythonAstCornerCase16() throws Exception {
        compareWithAst("cdef fused memslice_fused:\n  float[:]",
                "Module[body=[Assign[targets=[Name[id=memslice_fused, ctx=Store, reserved=false]], value=Name[id=None, ctx=Load, reserved=true], type=null]]]");
    }

    public void testGenCythonAstCdef() throws Exception {
        String s = "def  bar(): pass\r\n";
        String cython = "cdef bar(): pass\r\n";
        compareCase(s, cython, true);
    }

    public void testGenCythonAstCdef2() throws Exception {
        String s = "def  bar(a, b): pass\r\n";
        String cython = "cdef bar(a, b): pass\r\n";
        compareCase(s, cython, true);
    }

    public void testGenCythonAstAttributes() throws Exception {
        String s = "my.bar.ra = my.foo.ra";
        compareCase(s, s, true);
    }

    public void testGenCythonAstMultipleAssigns() throws Exception {
        String s = "self.bar = bar = 10";
        compareCase(s, s, true);
    }

    public void testGenCythonTupleArg() throws Exception {
        String cython = "def func((a, b)):\n" +
                "    return a + b";

        String s = "def func( a, b ):\n" +
                "    return a + b";

        compareCase(s, cython, true);
    }

    public void testGenCythonArray() throws Exception {
        String cython = "cdef double[:, :] foobar = <double[:10, :10]> NULL";
        String s = "foobar = None";
        compareCase(s, cython, false);
    }

    public void testGenCythonAst() throws Exception {
        String cython = "class Foo(object):\n" +
                "\n" +
                "    def method(self, foo):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "cdef bar():\n" +
                "    return Foo()\n" +
                "\n" +
                "b = bar()\n" +
                "";
        String s = "class Foo(object):\n" +
                "\n" +
                "    def method(self, foo):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "def  bar():\n" +
                "    return Foo()\n" +
                "\n" +
                "b = bar()\n" +
                "";
        compareCase(s, cython, true);
    }

    public void testGenCythonAstClassCDef() throws Exception {
        String s = ""
                + "\n"
                + "class TemplateTest1:\n"
                + "    a = None\n"
                + "    def b(): pass";

        String cython = ""
                + "cdef extern from \"templates.h\":\n"
                + "  cdef cppclass TemplateTest1[T]:\n"
                + "    int a\n"
                + "    def b(): pass\n"
                + "";
        compareCase(s, cython, false);
    }

    public void testGenCythonAstClassCDef2() throws Exception {
        String s;
        String cython;
        ISimpleNode cythonAst;
        Module m;

        s = "class bar:\n    pass\r\n";
        cython = "cdef class bar:\n    pass\r\n";
        compareCase(s, cython);
        cythonAst = compareCase(s, cython).ast;
        m = (Module) cythonAst;
        ClassDef def = (ClassDef) m.body[0];
        assertEquals(12, def.name.beginColumn);

        s = "class bar(object):\n"
                + "    def method(self):\n"
                + "        pass";

        cython = "cdef class bar(object):\n"
                + "    cpdef def method(self):\n"
                + "        pass";
        compareCase(s, cython);

        s = "class bar(object):\n"
                + "    def method(self, a, b):\n"
                + "        pass";

        cython = "cdef class bar(object):\n"
                + "    cpdef def method(self, double a, int b) except *:\n"
                + "        pass";
        compareCase(s, cython);

        s = "class bar(object):\n"
                + "    x = 0; y = None\n"
                + "    def method(self, a, b):\n"
                + "        pass";

        cython = "cdef class bar(object):\n"
                + "    cdef int x = 0, y\n"
                + "    cpdef def method(self, double a, int b) except *:\n"
                + "        pass";
        compareCase(s, cython);
    }

    public void testGenCythonAstBasic() throws Exception {
        ParserInfo parserInfo = new ParserInfo(new Document("a = 10"), grammarVersionProvider);
        String output = new GenCythonAstImpl(parserInfo).genCythonJson();
        JsonValue value = JsonValue.readFrom(output);

        JsonValue body = value.asObject().get("ast").asObject().get("stats");
        Object expect1 = JsonValue.readFrom(
                "[                                      \n"
                        + "    {                                  \n"
                        + "        \"__node__\": \"SingleAssignment\",\n"
                        + "        \"line\": 1,                     \n"
                        + "        \"col\": 4,                      \n"
                        + "        \"lhs\": {                       \n"
                        + "            \"__node__\": \"Name\",        \n"
                        + "            \"line\": 1,                 \n"
                        + "            \"col\": 0,                  \n"
                        + "            \"name\": \"a\"                \n"
                        + "        },                             \n"
                        + "        \"rhs\": {                       \n"
                        + "            \"__node__\": \"Int\",         \n"
                        + "            \"line\": 1,                 \n"
                        + "            \"col\": 4,                  \n"
                        + "            \"is_c_literal\": \"None\",    \n"
                        + "            \"value\": \"10\",             \n"
                        + "            \"unsigned\": \"\",            \n"
                        + "            \"longness\": \"\",            \n"
                        + "            \"constant_result\": \"10\",   \n"
                        + "            \"type\": \"long\"             \n"
                        + "        },                             \n"
                        + "        \"first\": \"False\"               \n" // This is new in Cython 3.0
                        + "    }                                  \n"
                        + "]                                      ");
        Object expect2 = JsonValue.readFrom(
                "[\n" +
                        "        {\n" +
                        "            \"__node__\": \"SingleAssignment\",\n" +
                        "            \"line\": 1,\n" +
                        "            \"col\": 4,\n" +
                        "            \"lhs\": {\n" +
                        "                \"__node__\": \"Name\",\n" +
                        "                \"line\": 1,\n" +
                        "                \"col\": 0,\n" +
                        "                \"name\": \"a\"\n" +
                        "            },\n" +
                        "            \"rhs\": {\n" +
                        "                \"__node__\": \"Int\",\n" +
                        "                \"line\": 1,\n" +
                        "                \"col\": 4,\n" +
                        "                \"is_c_literal\": \"None\",\n" +
                        "                \"value\": \"10\",\n" +
                        "                \"unsigned\": \"\",\n" +
                        "                \"longness\": \"\",\n" +
                        "                \"constant_result\": \"10\",\n" +
                        "                \"type\": \"long\"\n" +
                        "            }\n" +
                        "        }\n" +
                        "    ]\n" +
                        "\n");

        if (!body.equals(expect1) && !body.equals(expect2)) {
            throw new AssertionError("The body json doesn't match what we expect:\n" + body);
        }

        assertEquals(
                "Module[body=[Assign[targets=[Name[id=a, ctx=Store, reserved=false]], value=Num[n=10, type=Int, num=10], type=null]]]",
                new GenCythonAstImpl(parserInfo).genCythonAst().ast.toString());

    }
}
