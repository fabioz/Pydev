/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.io.File;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Set;
import org.python.pydev.parser.jython.ast.SetComp;
import org.python.pydev.parser.jython.ast.Starred;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.io.FileUtils;

public class PyParser30Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser30Test test = new PyParser30Test();
            test.setUp();
            test.testYieldFrom();
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

    public void testUnicodeAcceptedAgain() {
        String s = "" +
                "a = u'error'\n" + //3.3 accepts it again.
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
                "class IOBase(object, *args, metaclass=abc.ABCMeta):\n" +
                "    pass\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testMetaClass4() {
        String s = "" +
                "class B(*[x for x in [object]]):\n" +
                "    pass\n" +
                "";
        parseLegalDocStr(s);
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
                "def __init__(self, dirname, factory=None, create=True):\n"
                +
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

    public void testFuncCall4() {
        String s = "" +
                "Call('a', file=file)\n" +
                "\n" +
                "\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testFuncCall5() {
        String s = "" +
                "Call(1,2,3, *(4,5,6), keyword=13)\n" +
                "\n" +
                "\n" +
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
                "abstracts = {name\n"
                +
                "             for name, value in namespace.items()\n" +
                "             if value==1}\n"
                +
                "print(abstracts)\n" +
                "\n" +
                "";
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign a0 = (Assign) m.body[0];
        Assign a1 = (Assign) m.body[1];
        assertTrue(a0.value instanceof Dict);
        SetComp setComp = (SetComp) a1.value;

        assertEquals("name", ((Name) setComp.elt).id);
    }

    public void testSetCreation() {
        String s = "" +
                "namespace = {1, 2, 3, 4}\n" +
                "";
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign a0 = (Assign) m.body[0];
        assertTrue(a0.value instanceof Set);
        assertEquals("Assign[targets=[Name[id=namespace, ctx=Store, reserved=false]], value="
                +
                "Set[elts=[Num[n=1, type=Int, num=1], Num[n=2, type=Int, num=2], "
                +
                "Num[n=3, type=Int, num=3], Num[n=4, type=Int, num=4]]]]", a0.toString());
    }

    public void testDictComprehension() {
        String s = "" +
                "namespace = {'a':1, 'b':2, 'c':1, 'd':1}\n" +
                "abstracts = {name: value\n"
                +
                "             for name, value in namespace.items()\n" +
                "             if value==1}\n"
                +
                "print(abstracts)\n" +
                "\n" +
                "";
        SimpleNode ast = parseLegalDocStr(s);
        Module m = (Module) ast;
        Assign a0 = (Assign) m.body[0];
        Assign a1 = (Assign) m.body[1];
        assertTrue(a0.value instanceof Dict);
        DictComp dictComp = (DictComp) a1.value;

        assertEquals("name", ((Name) dictComp.key).id);
        assertEquals("value", ((Name) dictComp.value).id);
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

    public void testMultiWithStmt() {
        String s = "" +
                "with 1 as b, 2 as c:pass\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testRaiseFrom() {
        String s = "" +
                "try:\n" +
                "    print(a)\n" +
                "except Exception as e:\n" +
                "    raise SyntaxError() from e"
                +
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
                "        nonlocal a\n"
                +
                "        global x\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testNonLocalAndShortcuts2() {
        String s = "" +
                "def m1():\n" +
                "    a = 20\n" +
                "    def m2():\n" +
                "        nonlocal a\n"
                +
                "        global x\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testListComp() {
        String s = "" +
                "def m1():\n" +
                "    return any(cls.__subclasscheck__(c) for c in {subclass, subtype})\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testMisc2() {
        String s = "" +
                "class ABCMeta(type):\n" +
                "    _abc_invalidation_counter = 0\n"
                +
                "    def __new__(mcls, name, bases, namespace):\n"
                +
                "        cls = super().__new__(mcls, name, bases, namespace)\n"
                +
                "        # Compute set of abstract method names\n" +
                "        abstracts = {name\n"
                +
                "                     for name, value in namespace.items()\n"
                +
                "                     if getattr(value, '__isabstractmethod__', False)}\n" +
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
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testExecInvalid() {
        String s = "" +
                "exec 'foo'\n" +
                "";
        parseILegalDocStr(s);
        parseILegalDocStrWithoutTree(s);
    }

    public void testSetComprehension2() {
        String s = "" +
                "{x + 1 for x in s}\n" +
                "";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testComprehensions() {
        String s = "" +
                "s = {1, 2, 3}\n" +
                "print(s)\n" +
                "s = {x + 1 for x in s}\n" +
                "print(s)\n"
                +
                "s = {x : x * 2 for x in s}\n" +
                "print(s)\n" +
                "";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testUnicodeIdentifiers() {
        String s = "" +
                "class ��(object):\n" +
                "    pass\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testUnicodeIdentifiers2() {
        String contents = FileUtils.getFileContents(new File(TestDependent.TEST_PYDEV_PARSER_PLUGIN_LOC
                +
                "/tests/org/python/pydev/parser/pep3131test.py"));

        parseLegalDocStr(contents);
        parseLegalDocStrWithoutTree(contents);
    }

    public void testUnpacking() {
        String s = "a, *b, c = range(5)";

        Module ast = (Module) parseLegalDocStr(s);
        Assign assign = (Assign) ast.body[0];
        Tuple tup = (Tuple) assign.targets[0];
        Starred starred = (Starred) tup.elts[1];
        Name name = (Name) starred.value;
        assertEquals("b", name.id);
        assertEquals(Name.Store, name.ctx);
        parseLegalDocStrWithoutTree(s);
    }

    public void testUnpacking2() {
        String s = "a, *b.b, c = range(5)";

        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testCall() {
        String s = "fubar(*list, x=4)";

        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testUnpackingIn() {
        String s = "for a,b,*rest in list: pass";

        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testLib() throws Exception {
        if (TestDependent.PYTHON_30_LIB != null) {
            parseFilesInDir(new File(TestDependent.PYTHON_30_LIB), false);
        }
    }

    public void testBinNumber() {
        String s = "" +
                "0b00010\n" +
                "0B00010\n" +
                "0b00010L\n" +
                "0B00010l\n" +
                "";

        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testLongParseError() {
        String s = "" +
                "0L\n" +
                "";

        parseILegalDocStr(s);
    }

    public void testEllipsis() {
        String s = "" +
                "..." +
                "";

        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testEllipsis2() {
        String s = "" +
                "from ... import a\n" +
                "..." +
                "";

        Module node = (Module) parseLegalDocStr(s);
        ImportFrom f = (ImportFrom) node.body[0];
        assertEquals(f.level, 3);
        NameTok n = (NameTok) f.module;
        assertEquals(n.id, "");
        parseLegalDocStrWithoutTree(s);
    }

    public void testKeywordArgumentsInClassDeclaration() {
        String s = "" +
                "class A(meta=B, foo=C):pass\n" +
                "";

        Module node = (Module) parseLegalDocStr(s);
        ClassDef c = (ClassDef) node.body[0];
        assertEquals(2, c.keywords.length);

        parseLegalDocStrWithoutTree(s);
    }

    public void testNewSetConstructEndingWithComma() {
        String s = "" +
                "s = { 1, }\n" +
                "";

        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testYieldFrom() {
        String s = "" +
                "def m1():\n" +
                "    yield from a" +
                "";

        Module ast = (Module) parseLegalDocStr(s);
        FunctionDef f = (FunctionDef) ast.body[0];
        Expr e = (Expr) f.body[0];
        Yield y = (Yield) e.value;
        assertTrue("Expected yield to be a yield from.", y.yield_from);

        parseLegalDocStrWithoutTree(s);
    }

    public void testYield() {
        String s = "" +
                "def m1():\n" +
                "    yield" +
                "";

        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testYield2() {
        String s = "" +
                "def m1():\n" +
                "    yield a,b" +
                "";

        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    @Override
    public void testEmpty() throws Throwable {
        String s = "";

        parseLegalDocStr(s);
    }

    public void testIllegal() throws Exception {
        String s = ""
                + "a = dict(\n"
                + " foo.bar = 1\n"
                + ")\n"
                + "";
        Throwable parseILegalDocStrError = parseILegalDocStr(s);
        assertTrue(!parseILegalDocStrError.toString().contains("ClassCastException"));
    }

    public void testListRemainder() throws Exception {
        String s = ""
                + "(first, middle, *last) = lst"
                + "";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAsync() throws Exception {
        String s = ""
                + "async def m1():\n"
                + "    pass";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAsync1() throws Exception {
        String s = ""
                + "@param\n"
                + "async def m1():\n"
                + "    pass";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAsync2() throws Exception {
        String s = ""
                + "async with a:\n"
                + "    pass";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAsync3() throws Exception {
        String s = ""
                + "async with a:\n"
                + "    pass";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAwait() throws Exception {
        String s = ""
                + "async with a:\n"
                + "    b = await foo()";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testDotOperator() throws Exception {
        String s = ""
                + "a = a @ a"
                + "";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testDotOperator2() throws Exception {
        String s = ""
                + "a @= a"
                + "";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAcceptKwargsOnClass() throws Exception {
        String s = ""
                + "class F(**args):\n"
                + "    pass"
                + "";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAcceptKwargsAsParam() throws Exception {
        String s = ""
                + "dict(**{'1':1})\n"
                + "";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAsyncNotKeyword() throws Exception {
        String s = ""
                + "class async(object):\n"
                + "    pass";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }

    public void testAwaitNotKeyword() throws Exception {
        String s = ""
                + "class await(object):\n"
                + "    pass";
        parseLegalDocStr(s);
        parseLegalDocStrWithoutTree(s);
    }
}
