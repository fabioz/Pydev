/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 11, 2006
 */
package org.python.pydev.parser.prettyprinter;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.shared_core.callbacks.ICallback;

public class PrettyPrinterTest extends AbstractPrettyPrinterTestBase {

    public static void main(String[] args) {
        try {
            DEBUG = true;
            PrettyPrinterTest test = new PrettyPrinterTest();
            test.setUp();
            test.testDecorator2();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PrettyPrinterTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testNewSetConstruct() throws Throwable {
        final String s = "" +
                "mutable_set = {1,2,3,4,5}\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version > IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
                    checkPrettyPrintEqual(s, s);
                }
                return true;
            }
        });
    }

    public void testNewSetCompConstruct() throws Throwable {
        final String s = "" +
                "mutable_set = {x for x in xrange(10)}\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version > IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
                    checkPrettyPrintEqual(s, s);
                }
                return true;
            }
        });
    }

    public void testWithMultiple() throws Throwable {
        final String s = "" +
                "with a,b,c:\n" +
                "    pass\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version > IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
                    checkPrettyPrintEqual(s, s);
                }
                return true;
            }
        });
    }

    public void testNPEError() throws Throwable {
        final String s = "" +
                "def Foo(*args):\n" +
                "    pass\n" +
                "";

        IGrammarVersionProvider p = new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        };
        Module node = (Module) parseLegalDocStr(s);
        FunctionDef funcDef = (FunctionDef) node.body[0];
        String result = PrettyPrinterV2.printArguments(p, funcDef.args);
        assertEquals("*args", result);
    }

    public void testNPEError2() throws Throwable {
        final String s = "" +
                "def Foo(*, a):\n" +
                "    pass\n" +
                "";

        IGrammarVersionProvider p = new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
            }
        };
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        Module node = (Module) parseLegalDocStr(s);
        FunctionDef funcDef = (FunctionDef) node.body[0];
        String result = PrettyPrinterV2.printArguments(p, funcDef.args);
        assertEquals("*, a", result);
    }

    public void testNPEError3() throws Throwable {
        final String s = "" +
                "def Foo(**a):\n" +
                "    pass\n" +
                "";

        IGrammarVersionProvider p = new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0;
            }
        };
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        Module node = (Module) parseLegalDocStr(s);
        FunctionDef funcDef = (FunctionDef) node.body[0];
        String result = PrettyPrinterV2.printArguments(p, funcDef.args);
        assertEquals("**a", result);
    }

    public void testEllipsis() throws Throwable {
        final String s = "" +
                "...\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        checkPrettyPrintEqual(s, s);
    }

    public void testEllipsis2() throws Throwable {
        final String s = "" +
                "class Bar(object):\n" +
                "    ...\n" +
                "...\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        checkPrettyPrintEqual(s, s);
    }

    public void testEllipsis3() throws Throwable {
        final String s = "" +
                "from ... import foo\n" +
                "class Bar(object):\n" +
                "    ...\n" +
                "...\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        checkPrettyPrintEqual(s, s);
    }

    public void testEllipsis4() throws Throwable {
        final String s = "" +
                "lst[...]\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7);
        checkPrettyPrintEqual(s, s);
    }

    public void testEllipsis5() throws Throwable {
        final String s = "" +
                "from ... import foo\n" +
                "class Bar(object):\n" +
                "    lst[...]\n" +
                "    ...\n"
                +
                "...\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        checkPrettyPrintEqual(s, s);
    }

    public void testNewIf() throws Throwable {
        final String s = "" +
                "j = stop if (arg in gets) else start\n" +
                "";

        //Without specials, we don't know about the parenthesis.
        final String v2 = "" +
                "j = stop if arg in gets else start\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version != IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4) {
                    checkPrettyPrintEqual(s, s, v2);
                }
                return true;
            }
        });
    }

    public void testMethodDef() throws Throwable {
        final String s = "" +
                "def _dump_registry(cls,file=None):\n" +
                "    pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testLinesAfterMethod() throws Throwable {
        prefs.setLinesAfterMethod(1);
        final String s = "" +
                "def method():\n" +
                "    pass\n" +
                "a = 10" +
                "";

        final String expected = "" +
                "def method():\n" +
                "    pass\n" +
                "\n" +
                "a = 10\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected);
                return true;
            }
        });
    }

    public void testExec2() throws Throwable {
        final String s = "" +
                "exec ('a=1')\n" +
                "";

        final String v2 = "" +
                "exec 'a=1'\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    checkPrettyPrintEqual(s, "exec('a=1')\n");

                } else {
                    checkPrettyPrintEqual(s, s, v2);
                }
                return true;
            }
        });

    }

    public void testStarred() throws Throwable {
        final String s = "" +
                "a,*b,c = range(5)\n" +
                "a,*b.c,c = range(5)\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    checkPrettyPrintEqual(s, s);

                }
                return true;
            }
        });

    }

    public void testDictComp() throws Throwable {
        final String s = "" +
                "d = {i:i * 2 for i in range(3)}\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
                    checkPrettyPrintEqual(s, s);

                }
                return true;
            }
        });

    }

    public void testMethodDef4() throws Throwable {
        final String s = "" +
                "def _set_stopinfo(stoplineno=-1):\n" +
                "    pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testMethodDef2() throws Throwable {
        final String s = "" +
                "def _set_stopinfo(self,stopframe,returnframe,stoplineno=-1):\n"
                +
                "    if not sys.args[:1]:\n" +
                "        pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testMethodDef5() throws Throwable {
        final String s = "" +
                "def _set_stopinfo(stoplineno=not x[-1]):\n" +
                "    if not sys.args[:1]:\n"
                +
                "        pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testMethodDef3() throws Throwable {
        final String s = "" +
                "def Bastion(object,filter=lambda kk:kk[:1] != '_',name=None,bastionclass=Foo):\n"
                +
                "    pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testListComp5() throws Throwable {
        final String s = "data = [[1,2,3],[4,5,6]]\n" +
                "newdata = [[val * 2 for val in lst] for lst in data]\n";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testListComp6() throws Throwable {
        final String s = "" +
                "start,stop = (int(x) for x in spec.split('-'))\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testStrings() throws Throwable {
        final String s = "" +
                "\"test\"\n" +
                "'test'\n" +
                "'''test'''\n" +
                "u'''test'''\n" +
                "b'''test'''\n"
                +
                "r'''test'''\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6
                        || version == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
                    checkPrettyPrintEqual(s);
                }
                return true;
            }
        });
    }

    public void testNums() throws Throwable {
        final String s = "" +
                "0o700\n" +
                "0O700\n" +
                "0700\n" +
                "0x700\n" +
                "0X700\n" +
                "0b100\n" +
                "0B100\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6
                        || version == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
                    checkPrettyPrintEqual(s);
                }
                return true;

            }
        });
    }

    public void testClassDecorator() throws Throwable {
        final String s = "" +
                "@classdec\n" +
                "@classdec2\n" +
                "class A:\n" +
                "    pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    checkPrettyPrintEqual(s);
                }
                return true;
            }
        });
    }

    public void testFuncCallWithListComp() throws Throwable {
        final String s = "" +
                "any(cls.__subclasscheck__(c) for c in [subclass,subtype])\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version != IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4) {
                    checkPrettyPrintEqual(s);
                }
                return true;
            }
        });
    }

    public void testNewFuncCall() throws Throwable {
        final String s = "Call(1,2,3,kkk=22,*(4,5,6),keyword=13,**kwargs)\n";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    checkPrettyPrintEqual(s);
                }
                return true;
            }
        });
    }

    public void testExceptAs() throws Throwable {
        final String s = "" +
                "try:\n" +
                "    a = 10\n" +
                "except RuntimeError as x:\n" +
                "    pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7) {
                    checkPrettyPrintEqual(s);
                }
                return true;
            }
        });
    }

    public void testIfs() throws Throwable {
        final String s = "" +
                "def method1():\n" +
                "    if idx > 2:\n" +
                "        pass\n" +
                "    else:\n"
                +
                "        pass\n" +
                "    if idx == 5:\n" +
                "        pass\n";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });

    }

    public void testTryFinallyBeginNode() throws Exception {
        doTryFinallyBeginNode(IPythonNature.GRAMMAR_PYTHON_VERSION_2_4);
        doTryFinallyBeginNode(IPythonNature.GRAMMAR_PYTHON_VERSION_2_5);
    }

    public void doTryFinallyBeginNode(int version) throws Exception {
        String str = "" +
                "try:\n" +
                "    a = 1\n" +
                "finally:\n" +
                "    pass\n" +
                "";
        SimpleNode node = checkPrettyPrintEqual(str);
        Module m = (Module) node;
        SimpleNode f = m.body[0];
        assertEquals(1, f.beginLine);
    }

    public void testComments() throws Exception {
        String s = "" +
                "class MyMeta(type):\n" +
                "    def __str__(cls):\n"
                +
                "        return \"Beautiful class '%s'\" % cls.__name__\n" +
                "class MyClass:\n"
                +
                "    __metaclass__ = MyMeta\n" +
                "print type(foox)\n" +
                "# after print type\n"
                +
                "class A(object):# on-line\n" +
                "    # foo test\n" +
                "    def met(self):\n" +
                "        print 'A'\n"
                +
                "";

        String v3 = "" +
                "class MyMeta(type):\n" +
                "    def __str__(cls):\n"
                +
                "        return \"Beautiful class '%s'\" % cls.__name__\n" +
                "class MyClass:\n"
                +
                "    __metaclass__ = MyMeta\n" +
                "print type(foox)# after print type\n"
                +
                "class A(object):# on-line\n" +
                "# foo test\n" +
                "    def met(self):\n" +
                "        print 'A'\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);

    }

    public void testComment5() throws Exception {
        String s = "" +
                "class CoolApproach(object):\n" +
                "    # this tests also a tuple \"special case\"\n"
                +
                "    def foodeco(**arg5):\n" +
                "        pass\n" +
                "";
        String v3 = "" +
                "class CoolApproach(object):# this tests also a tuple \"special case\"\n"
                +
                "    def foodeco(**arg5):\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);

    }

    public void testDecoration() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    @foodeco(('arg_3',),2,a=2,b=3)\n"
                +
                "    def __init__(self,arg_1,(arg_2,arg_3),arg_4,arg_5):\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(str);
    }

    public void testComments6() throws Exception {
        String s = "" +
                "class FooApproach(CoolApproach):\n"
                +
                "    def __init__(self,arg_1,(arg_2,arg_3),*arg_4,**arg_5):\n"
                +
                "        # .. at this point all parameters except for 'arg_3' have been\n"
                +
                "        # copied to object attributes\n" +
                "        pass\n" +
                "";

        String v3 = ""
                +
                "class FooApproach(CoolApproach):\n"
                +
                "    def __init__(self,arg_1,(arg_2,arg_3),*arg_4,**arg_5):# .. at this point all parameters except for 'arg_3' have been\n"
                +
                "    # copied to object attributes\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);

    }

    public void testComprehension() throws Exception {
        String s = "compre4list = [A ** 2 for B in (1,4,6) if C % 2 == 1 if D % 3 == 2]# on-line\n";
        checkPrettyPrintEqual(s);
    }

    public void test25If() throws Exception {
        String str = "a = 1 if True else 2\n";
        checkPrettyPrintEqual(str);
    }

    public void test25Import() throws Exception {
        String str = "from . import foo\n";
        checkPrettyPrintEqual(str);
    }

    public void test25Import2() throws Exception {
        String str = "from ..bar import foo\n";
        checkPrettyPrintEqual(str);
    }

    public void test25Import3() throws Exception {
        String str = "from ... import foo\n";
        checkPrettyPrintEqual(str);
    }

    public void test25With() throws Exception {
        String str = "" +
                "from __future__ import with_statement\n" +
                "with a:\n" +
                "    print a\n" +
                "";
        checkPrettyPrintEqual(str);
    }

    public void test25With2() throws Exception {
        String str = "" +
                "from __future__ import with_statement\n" +
                "with a as b:\n" +
                "    print b\n" +
                "";
        checkPrettyPrintEqual(str);
    }

    public void test25With3() throws Exception {
        String str = "" +
                "from __future__ import with_statement\n" +
                "def m1():\n" +
                "    with a as b:\n"
                +
                "        print b\n" +
                "";
        checkPrettyPrintEqual(str);
    }

    public void test25With4() throws Exception {
        String str = "" +
                "from __future__ import with_statement\n" +
                "with a:\n" +
                "    callIt1()\n"
                +
                "    callIt2()\n" +
                "";
        checkPrettyPrintEqual(str);
    }

    public void testGlobal() throws Exception {
        String s = "" +
                "global foo\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testImport() throws Exception {
        String s = "" +
                "import foo\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testImport6() throws Exception {
        String s = "" +
                "#foo\n" +
                "from hashlib import md5\n" +
                "new = md5\n" +
                "";

        String v3 = "" +
                "from hashlib import md5#foo\n" +
                "new = md5\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);
    }

    public void testKwArgs2() throws Exception {
        String s = "" +
                "HTTPS(host,None,**(x509 or {}))\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testImport5() throws Exception {
        String s = "" +
                "import foo,bar\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testLambda3() throws Exception {
        String s = "" +
                "lambda a:(1 + 2)\n" +
                "";

        String v2 = "" +
                "lambda a:1 + 2\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testLambda4() throws Exception {
        String s = "" +
                "lambda d='':digestmod.new(d)\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testComment4() throws Exception {
        String s = "" +
                "class AAA:\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "#--- barrr\n" +
                "a = 10\n"
                +
                "#--- fooo\n" +
                "";

        String v3 = "" +
                "class AAA:\n" +
                "    def m1(self):\n" +
                "        pass#--- barrr\n" +
                "a = 10#--- fooo\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);

    }

    public void testComment3() throws Exception {
        String s = "" +
                "class Foo:\n" +
                "    pass\n" +
                "#--- barrr\n" +
                "a = 10\n" +
                "#--- fooo\n" +
                "";

        String v3 = "" +
                "class Foo:\n" +
                "    pass#--- barrr\n" +
                "a = 10#--- fooo\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);
    }

    public void testLambda2() throws Exception {
        String s = "" +
                "a = lambda:None\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDict3() throws Exception {
        String s = "" +
                "d = {#comm1\n" +
                "    1:2}\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testFuncAndComment2() throws Exception {
        String s = "" +
                "class Foo:\n" +
                "    def func1(self):\n" +
                "        pass\n" +
                "    # ------ Head elements\n"
                +
                "    def func2(self):\n" +
                "        pass\n" +
                "";

        String v3 = "" +
                "class Foo:\n" +
                "    def func1(self):\n" +
                "        pass# ------ Head elements\n"
                +
                "    def func2(self):\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);
    }

    public void testFuncAndComment() throws Exception {
        String s = "" +
                "class Foo:\n" +
                "    def func1(self):pass\n" +
                "    # ------ Head elements\n"
                +
                "    def func2(self):pass\n" +
                "";

        String expected = "" +
                "class Foo:\n" +
                "    def func1(self):\n" +
                "        pass\n"
                +
                "    # ------ Head elements\n" +
                "    def func2(self):\n" +
                "        pass\n" +
                "";

        String v3 = "" +
                "class Foo:\n" +
                "    def func1(self):\n" +
                "        pass# ------ Head elements\n"
                +
                "    def func2(self):\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s, expected, expected, v3);
    }

    public void testSubscript4() throws Exception {
        String s = "" +
                "print a[b:c()]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAssign3() throws Exception {
        String s = "" +
                "a = b = 0\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testComment1() throws Exception {
        String s = "" +
                "del a[-1]#comment\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testArgs() throws Exception {
        String s = "" +
                "def func():\n" +
                "    return a(*b)\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testArgs2() throws Exception {
        String s = "" +
                "def func():\n" +
                "    return a(*(b))\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testListComp4() throws Exception {
        String s = "" +
                "print [e for e in group if e[0] in a]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testReturn3() throws Exception {
        String s = "" +
                "if a:\n" +
                "    return foo(other)#comment\n" +
                "shouldround = 1\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAssert() throws Exception {
        String s = "" +
                "assert a not in b\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testStr2() throws Exception {
        String s = "" +
                "r\"foo\"\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testStr() throws Exception {
        String s = "" +
                "a = (r\"a\"#comm1\n" +
                "    r'\"b\"'#comm2\n" +
                ")\n" +
                "";
        String v2 = "" +
                "a = (r\"a\"#comm1\n" +
                "    r'\"b\"')#comm2\n" +
                "" +
                "";

        String v3 = "" +
                "a = (r\"a\"#comm1\n" +
                "    r'\"b\"')\n" +
                "    #comm2\n" +
                "" +
                "";
        checkPrettyPrintEqual(s, s, v2, v3);
    }

    public void testAdd() throws Exception {
        String s = "" +
                "m += 'a'\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testWildImport() throws Exception {
        String s = "" +
                "from a import *\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testRaise() throws Exception {
        String s = "" +
                "try:\n" +
                "    pass\n" +
                "except:\n" +
                "    raise SystemError,'err'\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testLambda() throws Exception {
        String s = "" +
                "print lambda n:n\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testListComp3() throws Exception {
        String s = "" +
                "print [s2 for s1 in b for s2 in a]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testList2() throws Exception {
        String s = "" +
                "print [(a,b)]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testList3() throws Exception {
        String s = "" +
                "all = [#comm1\n" +
                "    'encode','decode',]\n" +
                "";
        String v2 = "" +
                "all = [#comm1\n" +
                "    'encode','decode']\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testListComp2() throws Exception {
        String s = "" +
                "for (raw,cooked) in foo:\n" +
                "    pass\n" +
                "";
        String v2 = "" +
                "for raw,cooked in foo:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testKwArgs() throws Exception {
        String s = "" +
                "def a(**kwargs):\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept9() throws Exception {
        String s = "" +
                "def run():\n" +
                "    try:\n" +
                "        exec cmd\n" +
                "    except BdbQuit:\n"
                +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testSubscript3() throws Exception {
        String s = "" +
                "for a in b[:]:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testEndComments() throws Exception {
        String s = "" +
                "import foo\n" +
                "#end\n" +
                "";

        String v3 = "" +
                "import foo#end\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);
    }

    public void testExec() throws Exception {
        String s = "exec cmd in globals,locals\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept8() throws Exception {
        String s = "" +
                "try:\n" +
                "    try:\n" +
                "        pass\n" +
                "    except BdbQuit:\n" +
                "        pass\n"
                +
                "finally:\n" +
                "    self.quitting = 1\n" +
                "    sys.settrace(None)\n" +
                "";

        String v2 = "" +
                "try:\n" +
                "    pass\n" +
                "except BdbQuit:\n" +
                "    pass\n" +
                "finally:\n"
                +
                "    self.quitting = 1\n" +
                "    sys.settrace(None)\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testDecorator() throws Exception {
        String s = "" +
                "@decorator1\n" +
                "def m1():\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDecorator3() throws Exception {
        String s = "" +
                "@decorator1\n" +
                "@decorator2\n" +
                "def m1():\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDecorator2() throws Exception {
        String s = "" +
                "@decorator1(1,*args,**kwargs)\n" +
                "def m1():\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testComment() throws Exception {
        String s = "" +
                "# comment1\n" +
                "# comment2\n" +
                "# comment3\n" +
                "# comment4\n" +
                "'''str'''\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testStarArgs() throws Exception {
        String s = "" +
                "def recv(self,*args):\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testListComp() throws Exception {
        String s = "" +
                "print [x for x in tbinfo]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testSub() throws Exception {
        String s = "" +
                "print tbinfo[-1]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDel() throws Exception {
        String s = "" +
                "del foo\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testPar2() throws Exception {
        String s = "" +
                "def log(self,message):\n" +
                "    sys.stderr.write('log: %s' % str(message))\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testPar() throws Exception {
        String s = "" +
                "print (not connected)\n" +
                "";
        String v2 = "" +
                "print not connected\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testSimpleFunc() throws Exception {
        String s = "" +
                "def a():\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testReturn2() throws Exception {
        String s = "" +
                "def writable():\n" +
                "    return (not connected) or len(foo)\n" +
                "";
        String v2 = "" +
                "def writable():\n" +
                "    return not connected or len(foo)\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testSubs() throws Exception {
        String s = "" +
                "print num_sent[:512]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testNot() throws Exception {
        String s = "" +
                "def recv(self,buffer_size):\n" +
                "    data = self.socket.recv(buffer_size)\n"
                +
                "    if not data:\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testIfAnd() throws Exception {
        String s = "" +
                "if aaa and bbb:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAnd() throws Exception {
        String s = "" +
                "def listen(self,num):\n" +
                "    self.accepting = True\n"
                +
                "    if os.name == 'nt' and num > 5:\n" +
                "        num = 1\n"
                +
                "    return self.socket.listen(num)\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept7() throws Exception {
        String s = "" +
                "try:\n" +
                "    pass\n" +
                "except select.error,err:\n" +
                "    if False:\n" +
                "        raise\n"
                +
                "    else:\n" +
                "        return\n" +
                "";

        String v2 = "" +
                "try:\n" +
                "    pass\n" +
                "except select.error as err:\n" +
                "    if False:\n"
                +
                "        raise\n" +
                "    else:\n" +
                "        return\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testIf4() throws Exception {
        String s = "" +
                "if map:\n" +
                "    if True:\n" +
                "        time.sleep(timeout)\n" +
                "    else:\n"
                +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testIf3() throws Exception {
        String s = "" +
                "if aaa or bbb:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testEq() throws Exception {
        String s = "" +
                "if [] == r == w == e:\n" +
                "    time.sleep(timeout)\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testImportAs() throws Exception {
        String s = "" +
                "import foo as bla\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testImportAs2() throws Exception {
        String s = "" +
                "from a import foo as bla\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testIf2() throws Exception {
        String s = "" +
                "def readwrite():\n" +
                "    if True:\n" +
                "        a.b()\n" +
                "    if False:\n"
                +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testBreak() throws Exception {
        String s = "" +
                "for a in b:\n" +
                "    if True:\n" +
                "        break\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testContinue() throws Exception {
        String s = "" +
                "for a in b:\n" +
                "    if True:\n" +
                "        continue\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testBreak2() throws Exception {
        String s = "" +
                "for a in b:\n" +
                "    if True:\n" +
                "        break#comment\n" +
                "";
        String v2 = "" +
                "for a in b:\n" +
                "    if True:\n" +
                "        #comment\n" +
                "        break\n" +
                "";
        String v3 = s;
        checkPrettyPrintEqual(s, s, v2, v3);
    }

    public void testBreak3() throws Exception {
        String s = "" +
                "for a in b:\n" +
                "    if True:\n" +
                "        #comment1\n" +
                "        break#comment2\n" +
                "";

        String v2 = "" +
                "for a in b:\n" +
                "    if True:\n" +
                "        #comment1\n" +
                "        #comment2\n"
                +
                "        break\n" +
                "";

        String v3 = "" +
                "for a in b:\n" +
                "    if True:#comment1\n" +
                "        break#comment2\n" +
                "";

        checkPrettyPrintEqual(s, s, v2, v3);
    }

    public void testReturn() throws Exception {
        String s = "" +
                "def a():\n" +
                "    return 0\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testImport2() throws Exception {
        String s = "" +
                "import foo.bla#comment\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testImport3() throws Exception {
        String s = "" +
                "from foo.bla import bla1#comment\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testImport4() throws Exception {
        String s = "" +
                "from foo.bla import bla1\n" +
                "import foo\n" +
                "from bla import (a,b,c)\n";

        String v2 = "" +
                "from foo.bla import bla1\n" +
                "import foo\n" +
                "from bla import a,b,c\n";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testFor() throws Exception {
        String s = "" +
                "for a in b:\n" +
                "    print a\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testForElse() throws Exception {
        String s = "" +
                "for a in b:\n" +
                "    print a\n" +
                "else:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testWhile() throws Exception {
        String s = "" +
                "while True:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testWhile2() throws Exception {
        String s = "" +
                "while ((a + 1 < 0)):#comment\n" +
                "    pass\n" +
                "";
        String v2 = "" +
                "while (a + 1 < 0):#comment\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testWhileElse() throws Exception {
        String s = "" +
                "while True:\n" +
                "    pass\n" +
                "else:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExceptRaise() throws Exception {
        String s = "" +
                "try:\n" +
                "    print 'foo'\n" +
                "except:\n" +
                "    raise\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept() throws Exception {
        String s = "" +
                "try:\n" +
                "    print 'foo'\n" +
                "except:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept2() throws Exception {
        String s = "" +
                "try:\n" +
                "    socket_map\n" +
                "except NameError:\n" +
                "    socket_map = {}\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept3() throws Exception {
        String s = "" +
                "try:\n" +
                "    print 'foo'\n" +
                "except (NameError,e):\n" +
                "    print 'err'\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept4() throws Exception {
        String s = "" +
                "try:\n" +
                "    print 'foo'\n" +
                "except (NameError,e):\n" +
                "    print 'err'\n" +
                "else:\n"
                +
                "    print 'else'\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept5() throws Exception {
        String s = "" +
                "try:\n" +
                "    print 'foo'\n" +
                "except (NameError,e):\n" +
                "    print 'name'\n"
                +
                "except (TypeError,e2):\n" +
                "    print 'type'\n" +
                "else:\n" +
                "    print 'else'\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testTryExcept6() throws Exception {
        String s = "" +
                "def read(obj):\n" +
                "    try:\n" +
                "        obj.handle_read_event()\n" +
                "    except:\n"
                +
                "        obj.handle_error()\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testCall() throws Exception {
        String s = "" +
                "callIt(1)\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testCall2() throws Exception {
        String s = "" +
                "callIt(1#param1\n" +
                ")\n" +
                "";
        String v2 = "" +
                "callIt(1)#param1\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testCall3() throws Exception {
        String s = "" +
                "callIt(a=2)\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testCall4() throws Throwable {
        final String s = "" +
                "callIt(a=2,*args,**kwargs)\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testCall5() throws Exception {
        String s = "" +
                "m1(a,#d1\n" +
                "    b,#d2\n" +
                "    c#d3\n" +
                ")\n" +
                "";
        String v2 = "" +
                "m1(a,#d1\n" +
                "    b,#d2\n" +
                "    c)#d3\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testIf() throws Exception {
        String s = "" +
                "if i % target == 0:\n" +
                "    pass\n" +
                "";
        checkPrettyPrintEqual(s);

    }

    public void testIfElse() throws Exception {
        String s = "" +
                "if True:\n" +
                "    if foo:\n" +
                "        pass\n" +
                "    else:\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testListDict() throws Exception {
        String s = "" +
                "a = [1,#this is 1\n" +
                "    2]\n" +
                "a = {1:'foo'}\n" +
                "";
        checkPrettyPrintEqual(s);

    }

    public void testTupleDict() throws Exception {
        String s = "" +
                "a = (1,#this is 1\n" +
                "    2)\n" +
                "a = {1:'foo'}\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDict2() throws Exception {
        String s = "" +
                "a = {1:2,#this is 1\n" +
                "    2:2}\n" +
                "a = {1:'foo'}\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testVarious() throws Exception {
        String s = "" +
                "class Foo:\n" +
                "    def __init__(self,a,b):\n" +
                "        print self#comment0\n"
                +
                "        a,\n" +
                "        b\n" +
                "    def met1(self,a):#ok comment1\n" +
                "        a,\n"
                +
                "        b\n" +
                "        class Inner(object):\n" +
                "            pass\n" +
                "        self.met1(a)\n"
                +
                "print 'ok'\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testYield() throws Exception {
        String s = "" +
                "def foo():\n" +
                "    yield 10\n" +
                "print 'foo'\n" +
                "a = 3\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testYield2() throws Exception {
        String s = "" +
                "def foo():\n" +
                "    yield (10)#comment1\n" +
                "    print 'foo'\n" +
                "";

        String v2 = "" +
                "def foo():\n" +
                "    yield 10#comment1\n" +
                "    print 'foo'\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testYield4() throws Exception {
        String s = "" +
                "def foo():\n" +
                "    yield ((a + b) / 2)#comment1\n" +
                "    print 'foo'\n" +
                "";

        String v2 = "" +
                "def foo():\n" +
                "    yield (a + b) / 2#comment1\n" +
                "    print 'foo'\n" +
                "";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testMultipleBool() throws Exception {
        String s = "X or Y and X and Y or X\n";
        checkPrettyPrintEqual(s);
    }

    public void testFuncComment() throws Exception {
        String s = "" +
                "def foo():\n" +
                "    #comment0\n" +
                "    print 'foo'\n" +
                "";

        String v3 = "" +
                "def foo():#comment0\n" +
                "    print 'foo'\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);
    }

    public void testPrint() throws Exception {
        String s = "" +
                "print >> a,'foo'\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testPrintComment() throws Exception {
        String s = "" +
                "def test():#comm1\n" +
                "    print >> (a,#comm2\n" +
                "        'foo')#comm3\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAttr() throws Exception {
        String s = "" +
                "print a.b\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAttr2() throws Exception {
        String s = "" +
                "print a.b.c.d\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAttr3() throws Exception {
        String s = "" +
                "print a.d()\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAttr4() throws Exception {
        String s = "" +
                "hub.fun#comment\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAttrCall() throws Exception {
        String s = "" +
                "print a.d().e(1 + 2)\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testSubscript() throws Exception {
        String s = "" +
                "print a[0]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testSubscript2() throws Exception {
        String s = "" +
                "[0]\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDefaults() throws Exception {
        String s = "" +
                "def defaults(hi=None):\n" +
                "    if False:\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);

    }

    public void testDefaults2() throws Exception {
        String s = "" +
                "def defaults(a,x,lo=foo,hi=None):\n" +
                "    if hi is None:\n" +
                "        hi = a\n" +
                "";
        checkPrettyPrintEqual(s);

    }

    public void testNoComments() throws Exception {
        String s = "" +
                "class Class1:\n" +
                "    def met1(self,a):\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDocStrings() throws Exception {
        String s = "" +
                "class Class1:\n" +
                "    '''docstring1'''\n" +
                "    a = '''str1'''\n"
                +
                "    def met1(self,a):\n" +
                "        '''docstring2\n" +
                "        foo\n" +
                "        '''\n"
                +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDocStrings2() throws Exception {
        String s = "" +
                "class Class1:\n" +
                "    \"\"\"docstring1\"\"\"\n" +
                "    a = 'str1'\n"
                +
                "    def met1(self,a):\n" +
                "        \"docstring2\"\n" +
                "        ur'unicoderaw'\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testDocStrings3() throws Throwable {
        final String s = "" +
                "class Class1:\n" +
                "    def met1(self,a):\n" +
                "        ur'unicoderaw' + 'foo'\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version < IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    checkPrettyPrintEqual(s);
                }
                return true;
            }
        });
    }

    public void testDict() throws Exception {
        String s = "" +
                "if a:\n" +
                "    a = {a:1,b:2,c:3}\n";
        checkPrettyPrintEqual(s);
    }

    public void testList() throws Exception {
        String s = "" +
                "if a:\n" +
                "    a = [a,b,c]\n";
        checkPrettyPrintEqual(s);
    }

    public void testTuple() throws Exception {
        String s = "" +
                "if a:\n" +
                "    a = (a,b,c)\n";
        String v2 = "" +
                "if a:\n" +
                "    a = a,b,c\n";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testTuple2() throws Throwable {
        final String s = "" +
                "if a:\n" +
                "    a = (a,b,#comment\n" +
                "        c)\n";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testIfElse0() throws Exception {
        String s = "" +
                "if a:\n" +
                "    a = 1\n" +
                "elif b:\n" +
                "    b = 2\n" +
                "elif c:\n" +
                "    c = 3#foo\n";
        checkPrettyPrintEqual(s);
    }

    public void testIfElse1() throws Exception {
        String s = "" +
                "if a:\n" +
                "    a = 1\n" +
                "elif b:\n" +
                "    b = 2\n" +
                "elif c:\n" +
                "    c = 3\n"
                +
                "else:\n" +
                "    d = 4\n";
        checkPrettyPrintEqual(s);
    }

    public void testIfElse2() throws Exception {
        String s = "" +
                "if a:\n" +
                "    a = 1#comment1\n" +
                "elif b:\n" +
                "    b = 2#comment2\n" +
                "elif c:\n"
                +
                "    c = 3#comment3\n" +
                "else:\n" +
                "    d = 4#comment4\n";
        checkPrettyPrintEqual(s);
    }

    public void testIfElse3() throws Exception {
        String s = "#commentbefore\n" + //1
                "if a:#commentIf\n" + //2
                "    a = 1\n" + //3
                "elif b:#commentElif\n" + //4
                "    b = 2\n" + //5
                "elif c:\n" + //6
                "    c = 3\n" + //7
                "else:#commentElse\n" + //8
                "    d = 4\n" + //9
                "outOfIf = True\n"; //10

        String v3 = "#commentbefore\n" + //1
                "if a:#commentIf\n" + //2
                "    a = 1\n" + //3
                "elif b:#commentElif\n" + //4
                "    b = 2\n" + //5
                "elif c:\n" + //6
                "    c = 3\n" + //7
                "else:\n" + //8
                "    d = 4#commentElse\n" + //9
                "outOfIf = True\n"; //10

        checkPrettyPrintEqual(s, s, s, v3);
    }

    public void testIfElse4() throws Exception {
        String s = "if a:#commentIf\n" +
                "    c = 3\n" +
                "else:\n" +
                "    if b:\n" +
                "        pass\n";

        String v2 = "if a:#commentIf\n" +
                "    c = 3\n" +
                "elif b:\n" +
                "    pass\n";
        checkPrettyPrintEqual(s, s, v2);
    }

    public void testIfElse5() throws Exception {
        String s = "if a:#commentIf\n" +
                "    c = 3\n" +
                "else:\n" +
                "    if b:\n" +
                "        pass\n" +
                "    if c:\n"
                +
                "        pass\n";

        checkPrettyPrintEqual(s);
    }

    public void testCommentAndIf() throws Throwable {
        final String s = "" +
                "def initiate_send():\n" +
                "    if 10:\n" +
                "        # try to send the buffer\n"
                +
                "        try:\n" +
                "            num_sent = 10\n" +
                "        except:\n" +
                "            pass\n" +
                "";

        final String v3 = "" +
                "def initiate_send():\n" +
                "    if 10:# try to send the buffer\n" +
                "        try:\n"
                +
                "            num_sent = 10\n" +
                "        except:\n" +
                "            pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s, s, v3);
                return true;
            }
        });
    }

    public void testPlus() throws Exception {
        String s = "" +
                "a = 1 + 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testMinus() throws Exception {
        String s = "" +
                "a = 1 - 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testPow() throws Exception {
        String s = "" +
                "a = 1 ** 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testLShift() throws Exception {
        String s = "" +
                "a = 1 << 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testRShift() throws Exception {
        String s = "" +
                "a = 1 >> 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testBitOr() throws Exception {
        String s = "" +
                "a = 1 | 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testBitXOr() throws Exception {
        String s = "" +
                "a = 1 ^ 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testBitAnd() throws Exception {
        String s = "" +
                "a = 1 & 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testFloorDiv() throws Throwable {
        final String s = "" +
                "a = 1 // 1\n";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testNoComments2() throws Exception {
        prefs.setSpacesAfterComma(1);
        String s = "" +
                "class Class1(obj1, obj2):\n" +
                "    def met1(self, a, b):\n" +
                "        pass\n" +
                "";
        checkPrettyPrintEqual(s);
    }

    public void testAssign() throws Exception {
        String s = "" +
                "a = 1\n";
        checkPrettyPrintEqual(s);
    }

    public void testAssign2() throws Exception {
        String s = "" +
                "a = 1#comment\n";
        checkPrettyPrintEqual(s);
    }

    public void testComments1() throws Exception {
        String s = "#comment00\n" +
                "class Class1:#comment0\n" +
                "    #comment1\n" +
                "    def met1(self,a):#comment2\n"
                +
                "        pass#comment3\n" +
                "";
        String v3 = "#comment00\n" +
                "class Class1:#comment0\n" +
                "#comment1\n" + //this is because we mess with columns too.
                "    def met1(self,a):#comment2\n" +
                "        pass#comment3\n" +
                "";
        checkPrettyPrintEqual(s, s, s, v3);
    }

    public void testComments2() throws Throwable {
        final String s = "" +
                "class Foo(object):#test comment\n" +
                "    def m1(self,a,#c1\n" +
                "        b):#c2\n"
                +
                "        pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });

    }

    public void testComments3() throws Exception {
        String s = "" +
                "# comment before\n" +
                "i = 0\n" +
                "while (i < 2):# while test comment on-line\n"
                +
                "    print 'under 5'\n" +
                "    i += 1# augmented assignment on-line\n"
                +
                "    # this comment disappears\n" +
                "else:# else on-line\n" +
                "    # comment inside else\n"
                +
                "    print 'bigger'# print on-line\n" +
                "    # comment on else end\n"
                +
                "# after the second body (but actually in the module node)!\n" +
                "";

        String v2 = "" +
                "# comment before\n" +
                "i = 0\n" +
                "while i < 2:# while test comment on-line\n"
                +
                "    print 'under 5'\n" +
                "    i += 1# augmented assignment on-line\n"
                +
                "    # this comment disappears\n" +
                "else:# else on-line\n" +
                "    # comment inside else\n"
                +
                "    print 'bigger'# print on-line\n" +
                "    # comment on else end\n"
                +
                "# after the second body (but actually in the module node)!\n" +
                "";

        String v3 = "" +
                "# comment before\n" +
                "i = 0\n" +
                "while i < 2:# while test comment on-line\n"
                +
                "    print 'under 5'\n" +
                "    i += 1# augmented assignment on-line\n"
                +
                "# this comment disappears\n" +
                "else:\n" +
                "    print 'bigger'# print on-line\n"
                +
                "# else on-line\n" +
                "# comment inside else\n" +
                "# comment on else end\n"
                +
                "# after the second body (but actually in the module node)!\n" +
                "";
        checkPrettyPrintEqual(s, s, v2, v3);
    }

    public void testVarious2() throws Throwable {
        final String s = "" +
                "if self._file.getname() != 'FORM':\n"
                +
                "    raise Error('file does not start with FORM id')\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });

    }

    public void testVarious3() throws Throwable {
        final String s = "" +
                "def writeframes(self, data):\n" +
                "    self.writeframesraw(data)\n"
                +
                "    if self._nframeswritten != self._nframes or \\\n"
                +
                "          self._datalength != self._datawritten:\n" +
                "        self._patchheader()\n" +
                "";
        final String expected = "" +
                "def writeframes(self,data):\n" +
                "    self.writeframesraw(data)\n"
                +
                "    if self._nframeswritten != self._nframes or self._datalength != self._datawritten:\n"
                +
                "        self._patchheader()\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected);
                return true;
            }
        });

    }

    public void testVarious4() throws Throwable {
        final String s = "" +
                "if map:\n" +
                "    r = []; w = []; e = []\n" +
                "";
        final String expected = "" +
                "if map:\n" +
                "    r = []\n" +
                "    w = []\n" +
                "    e = []\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected);
                return true;
            }
        });
    }

    public void testVarious5() throws Throwable {
        final String s = "" +
                "imap[self._fileno] = self\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious6() throws Throwable {
        final String s = "" +
                "def accept(self):\n" +
                "    try:\n" +
                "        conn,addr = self.socket.accept()\n"
                +
                "        return conn,addr\n" +
                "    except socket.error:\n" +
                "        pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious7() throws Throwable {
        final String s = "" +
                "def deleteMe(self):\n" +
                "    index = (self.file,self.line)\n"
                +
                "    self.bpbynumber[self.number] = None# No longer in list\n"
                +
                "    self.bplist[index].remove(self)\n" +
                "";

        final String v2 = "" +
                "def deleteMe(self):\n" +
                "    index = self.file,self.line\n"
                +
                "    self.bpbynumber[self.number] = None# No longer in list\n"
                +
                "    self.bplist[index].remove(self)\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s, v2);
                return true;
            }
        });
    }

    public void testVarious8() throws Throwable {
        final String s = "" +
                "month_abbr = _localized_month('%b')\n" +
                "# Constants for weekdays\n"
                +
                "(MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY) = range(7)\n" +
                "";

        final String v2 = "" +
                "month_abbr = _localized_month('%b')\n" +
                "# Constants for weekdays\n"
                +
                "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY = range(7)\n" +
                "";

        final String v3 = "" +
                "month_abbr = _localized_month('%b')# Constants for weekdays\n"
                +
                "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY = range(7)\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s, v2, v3);
                return true;
            }
        });
    }

    public void testVarious9() throws Throwable {
        final String s = "" +
                "def _ilog(x,M,L=8):\n" +
                "    y = x - M\n" +
                "    R = 0\n" +
                "    while (R <= L or \n"
                +
                "        R > L):\n" +
                "        y = call((M * y) << 1,\n" +
                "            M,K)\n" +
                "        R += 1\n"
                +
                "";

        final String v2 = "" +
                "def _ilog(x,M,L=8):\n" +
                "    y = x - M\n" +
                "    R = 0\n"
                +
                "    while R <= L or R > L:\n" +
                "        y = call((M * y) << 1,\n" +
                "            M,K)\n"
                +
                "        R += 1\n" +
                "";

        final String v3 = "" +
                "def _ilog(x,M,L=8):\n" +
                "    y = x - M\n" +
                "    R = 0\n"
                +
                "    while R <= L or R > L:\n" +
                "        y = call((M * y) << 1,M,K)\n" +
                "        R += 1\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s, v2, v3);
                return true;
            }
        });
    }

    public void testVarious10() throws Throwable {
        final String s = "" +
                "num_lines[side] += 1\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious11() throws Throwable {
        final String s = "" +
                "self.ac_in_buffer = self.ac_in_buffer[index + terminator_len:]\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious12() throws Throwable {
        final String s = "" +
                "def ismethoddescriptor(object):\n" +
                "    return (hasattr(object, \"__get__\")\n"
                +
                "            and not hasattr(object, \"__set__\")# else it's a data descriptor\n"
                +
                "            #comment\n" +
                "            and not ismethod(object)# mutual exclusion\n"
                +
                "            and not isfunction(object))\n" +
                "";

        final String expected = "" +
                "def ismethoddescriptor(object):\n"
                +
                "    return (hasattr(object,\"__get__\") and \n"
                +
                "        not hasattr(object,\"__set__\") and # else it's a data descriptor\n" +
                "        #comment\n"
                +
                "        not ismethod(object) and # mutual exclusion\n" +
                "        not isfunction(object))\n" +
                "";

        final String v3 = ""
                +
                "def ismethoddescriptor(object):\n"
                +
                "    return (hasattr(object,\"__get__\") and not hasattr(object,\"__set__\")# else it's a data descriptor\n"
                +
                "        #comment\n" +
                "        and not ismethod(object)# mutual exclusion\n"
                +
                "        and not isfunction(object))\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected, expected, v3);
                return true;
            }
        });
    }

    public void testVarious13() throws Throwable {
        final String s = "" +
                "def remove_folder(self,folder):\n"
                +
                "    path = os.path.join(self._path,'.' + folder)\n"
                +
                "    for entry in os.listdir(os.path.join(path,'new')) + \\\n"
                +
                "        os.listdir(os.path.join(path,'cur')):\n" +
                "        pass\n" +
                "";

        final String expected = "" +
                "def remove_folder(self,folder):\n"
                +
                "    path = os.path.join(self._path,'.' + folder)\n"
                +
                "    for entry in os.listdir(os.path.join(path,'new')) + os.listdir(os.path.join(path,'cur')):\n"
                +
                "        pass\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected);
                return true;
            }
        });
    }

    public void testVarious14() throws Throwable {
        final String s = "" +
                "def retrfile(self, file, type):\n" +
                "    if type in ('A'): cmd = 'TYPE A'; isdir = 1\n"
                +
                "    else: cmd = 'TYPE B'; isdir = 0\n" +
                "";

        final String expected = "" +
                "def retrfile(self,file,type):\n" +
                "    if type in ('A'):\n"
                +
                "        cmd = 'TYPE A'\n" +
                "        isdir = 1\n" +
                "    else:\n" +
                "        cmd = 'TYPE B'\n"
                +
                "        isdir = 0\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected);
                return true;
            }
        });
    }

    public void testVarious15() throws Throwable {
        final String s = "" +
                "def visiblename(name, all=None):\n"
                +
                "    if name in ('__builtins__', '__doc__', '__file__', '__path__',\n"
                +
                "                '__module__', '__name__', '__slots__'): return 0\n"
                +
                "    # Private names are hidden, but special names are displayed.\n"
                +
                "    if name.startswith('__') and name.endswith('__'): return 1\n" +
                "";

        final String expected = "" +
                "def visiblename(name,all=None):\n"
                +
                "    if name in ('__builtins__','__doc__','__file__','__path__',\n"
                +
                "        '__module__','__name__','__slots__'):\n" +
                "        return 0\n"
                +
                "    # Private names are hidden, but special names are displayed.\n"
                +
                "    if name.startswith('__') and name.endswith('__'):\n" +
                "        return 1\n" +
                "";

        final String v3 = ""
                +
                "def visiblename(name,all=None):\n"
                +
                "    if name in ('__builtins__','__doc__','__file__','__path__','__module__','__name__','__slots__'):\n"
                +
                "        return 0# Private names are hidden, but special names are displayed.\n"
                +
                "    if name.startswith('__') and name.endswith('__'):\n" +
                "        return 1\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected, expected, v3);
                return true;
            }
        });
    }

    public void testVarious16() throws Throwable {
        final String s = "" +
                "def m(a,):pass;pass\n" +
                "";

        final String expected = "" +
                "def m(a,):\n" +
                "    pass\n" +
                "    pass\n" +
                "";

        final String v2 = "" +
                "def m(a):\n" +
                "    pass\n" +
                "    pass\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected, v2);
                return true;
            }
        });
    }

    public void testVarious17() throws Throwable {
        final String s = "" +
                "def m(a,\n" +
                "    b,\n" +
                "    c=False,\n" +
                "    ):\n" +
                "    pass\n" +
                "";

        final String expected = "" +
                "def m(a,\n" +
                "    b,\n" +
                "    c=False,\n" +
                "    ):\n" +
                "    pass\n" +
                "";

        final String v2 = "" +
                "def m(a,\n" +
                "    b,\n" +
                "    c=False):\n" +
                "    pass\n" +
                "";
        final String v3 = "" +
                "def m(a,b,c=False):\n" +
                "    pass\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected, v2, v3);
                return true;
            }
        });
    }

    public void testVarious18() throws Throwable {
        final String s = "" +
                "def b32encode(s):\n" +
                "    parts.extend([_b32tab[c1 >> 11],         # bits 1 - 5\n"
                +
                "                  _b32tab[c3 & 0x1f],        # bits 36 - 40 (1 - 5)\n" +
                "                  ])\n"
                +
                "";

        final String expected = "" +
                "def b32encode(s):\n" +
                "    parts.extend([_b32tab[c1 >> 11],# bits 1 - 5\n"
                +
                "            _b32tab[c3 & 0x1f],# bits 36 - 40 (1 - 5)\n" +
                "            ])\n" +
                "";

        final String v2 = "" +
                "def b32encode(s):\n" +
                "    parts.extend([_b32tab[c1 >> 11],# bits 1 - 5\n"
                +
                "            _b32tab[c3 & 0x1f]])# bits 36 - 40 (1 - 5)\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected, v2);
                return true;
            }
        });
    }

    public void testVarious19() throws Throwable {
        final String s = "" +
                "def m1():\n" +
                "    try: return int(v)\n" +
                "    except ValueError:\n"
                +
                "        try: return float(v)\n" +
                "        except ValueError: pass\n" +
                "";

        final String expected = "" +
                "def m1():\n" +
                "    try:\n" +
                "        return int(v)\n"
                +
                "    except ValueError:\n" +
                "        try:\n" +
                "            return float(v)\n"
                +
                "        except ValueError:\n" +
                "            pass\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected);
                return true;
            }
        });
    }

    public void testVarious20() throws Throwable {
        final String s = "" +
                "_b32rev = dict([(v, long(k)) for k, v in _b32alphabet.items()])\n" +
                "";

        final String expected = "" +
                "_b32rev = dict([(v,long(k)) for (k,v) in _b32alphabet.items()])\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected);
                return true;
            }
        });
    }

    public void testVarious21() throws Throwable {
        final String s = "" +
                "'a%s%s' % (1,2)\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious22() throws Throwable {
        final String s = "" +
                "if raw:\n" +
                "    return [(option,d[option]) for \n" +
                "        option in options]\n"
                +
                "";

        final String v3 = "" +
                "if raw:\n" +
                "    return [(option,d[option]) for option in options]\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s, s, v3);
                return true;
            }
        });
    }

    public void testVarious23() throws Throwable {
        final String s = "" +
                "assert nr_junk_chars > 0,(\n" +
                "    'split_header_words bug: %s, %s, %s' % \n"
                +
                "    (orig_text,text,pairs))\n" +
                "";

        final String v3 = ""
                +
                "assert nr_junk_chars > 0,('split_header_words bug: %s, %s, %s' % (orig_text,text,pairs))\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s, s, v3);
                return true;
            }
        });
    }

    public void testVarious24() throws Throwable {
        final String s = "" +
                "a = ()\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious25() throws Throwable {
        final String s = "" +
                "assert n in (2,3,4,5)\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious26() throws Throwable {
        final String s = "" +
                "def handle(self,context,*args):\n"
                +
                "    return (0,(0,),'n')#Passed to something which uses a tuple.\n" +
                "";

        final String v2 = "" +
                "def handle(self,context,*args):\n"
                +
                "    return 0,(0,),'n'#Passed to something which uses a tuple.\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s, v2);
                return true;
            }
        });
    }

    public void testVarious27() throws Throwable {
        final String s = "" +
                "def handle(self,context,*args):\n" +
                "    return (Infsign[sign],) * 2\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious28() throws Throwable {
        final String s = "" +
                "assert False,(\"unknown outcome\",outcome)\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious29() throws Throwable {
        final String s = "" +
                "assert 0,'Could not find method in self.functions and no '\\\n"
                +
                "'instance installed'\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious30() throws Throwable {
        final String s = "" +
                "if k == 'max-age':\n" +
                "    max_age_set = True\n" +
                "    try:\n"
                +
                "        v = int(v)\n" +
                "    except ValueError:\n"
                +
                "        _debug('   missing or invalid (non-numeric) value for '\n"
                +
                "            'max-age attribute')\n" +
                "        bad_cookie = True\n" +
                "        break\n"
                +
                "    # convert RFC 2965 Max-Age to seconds since epoch\n"
                +
                "    # XXX Strictly you're supposed to follow RFC 2616\n"
                +
                "    #   age-calculation rules.  Remember that zero Max-Age is a\n"
                +
                "    #   is a request to discard (old and new) cookie, though.\n" +
                "    k = 'expires'\n" +
                "";

        final String v3 = "" +
                "if k == 'max-age':\n" +
                "    max_age_set = True\n" +
                "    try:\n"
                +
                "        v = int(v)\n" +
                "    except ValueError:\n"
                +
                "        _debug('   missing or invalid (non-numeric) value for '\n"
                +
                "            'max-age attribute')\n" +
                "        bad_cookie = True\n"
                +
                "        break# convert RFC 2965 Max-Age to seconds since epoch\n"
                +
                "        # XXX Strictly you're supposed to follow RFC 2616\n"
                +
                "        #   age-calculation rules.  Remember that zero Max-Age is a\n"
                +
                "        #   is a request to discard (old and new) cookie, though.\n" +
                "    k = 'expires'\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s, s, v3);
                return true;
            }
        });
    }

    public void testVarious31() throws Throwable {
        final String s = ""
                +
                "assert not _active,\"Active pipes when test starts \" + repr([c.cmd for c in _active])\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s);
                return true;
            }
        });
    }

    public void testVarious32() throws Throwable {
        final String s = "" +
                "try:\n" +
                "    pass\n" +
                "except:\n"
                +
                "    raise IOError,('socket error',msg),sys.exc_info()[2]\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                if (version < IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    checkPrettyPrintEqual(s);
                }
                return true;
            }
        });
    }

    public void testVarious33() throws Throwable {
        final String s = "" +
                "def method(f):\n" +
                "    if True:\n" +
                "        pass\n" +
                "\n" +
                "class Obj(object):\n"
                +
                "    __slots__ = (\n" +
                "        # comment1\n" +
                "        'name1',\n" +
                "        # comment2\n"
                +
                "        'name2',\n" +
                "    )\n" +
                "";

        final String expected = "" +
                "def method(f):\n" +
                "    if True:\n" +
                "        pass\n" +
                "class Obj(object):\n"
                +
                "    __slots__ = (\n" +
                "        # comment1\n" +
                "        'name1',\n" +
                "        # comment2\n"
                +
                "        'name2',\n" +
                "        )\n" +
                "";

        final String v2 = "" +
                "def method(f):\n" +
                "    if True:\n" +
                "        pass\n" +
                "class Obj(object):\n"
                +
                "    __slots__ = (# comment1\n" +
                "        'name1',\n" +
                "        # comment2\n"
                +
                "        'name2')\n" +
                "";

        final String v3 = "" +
                "def method(f):\n" +
                "    if True:\n" +
                "        pass\n" +
                "class Obj(object):\n"
                +
                "    __slots__ = (# comment1\n" +
                "        'name1',# comment2\n" +
                "        'name2')\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, expected, v2, v3);
                return true;
            }
        });
    }

    public void testVarious34() throws Throwable {
        final String s = "" +
                "def Method():\n" +
                "    try:\n" +
                "        pass\n" +
                "    except:\n"
                +
                "        if data:\n" +
                "            pass\n" +
                "        else:\n" +
                "            pass\n"
                +
                "        pass\n" +
                "\n" +
                "#comment\n" +
                "";

        final String v2 = "" +
                "def Method():\n" +
                "    try:\n" +
                "        pass\n" +
                "    except:\n"
                +
                "        if data:\n" +
                "            pass\n" +
                "        else:\n" +
                "            pass\n"
                +
                "        pass\n" +
                "#comment\n" +
                "";

        final String v3 = "" +
                "def Method():\n" +
                "    try:\n" +
                "        pass\n" +
                "    except:\n"
                +
                "        if data:\n" +
                "            pass\n" +
                "        else:\n" +
                "            pass\n"
                +
                "        pass#comment\n" +
                "";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, v2, v2, v3);
                return true;
            }
        });
    }

    public void testPrintOnlyArgs() throws Throwable {
        final String s = "" +
                "def Method(a,b,c=10,d=20,*args,**kwargs):\n" +
                "    pass\n" +
                "";

        Module node = (Module) parseLegalDocStr(s);
        FunctionDef funcDef = (FunctionDef) node.body[0];
        assertEquals("a, b, c=10, d=20, *args, **kwargs", PrettyPrinterV2.printArguments(versionProvider, funcDef.args));

    }

    public void testPrintOnlyArgs2() throws Throwable {
        final String s = "" +
                "def Method((a, b), c):\n" +
                "    pass\n" +
                "";

        Module node = (Module) parseLegalDocStr(s);
        FunctionDef funcDef = (FunctionDef) node.body[0];
        //yes, just making sure it's not supported.
        assertEquals("(a, b), c", PrettyPrinterV2.printArguments(versionProvider, funcDef.args));

    }

    public void testPrintMultipleKwargsInClassDef() throws Throwable {
        final String s = "" +
                "class A(meta=B,foo=C):\n" +
                "    pass\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        checkPrettyPrintEqual(s);

    }

    public void testNewSetEndingWithComma() throws Throwable {
        String s = "s = {1,}\n";
        String expected = "s = {1}\n"; //yes, when creating a copy we loose the specials (and end without the comma).

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        checkPrettyPrintEqual(s, s, expected);

    }

    public void testRaiseFrom() throws Throwable {
        String s = "def my():\n"
                + "    raise call() from None\n";
        String expected = s;

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        checkPrettyPrintEqual(s, s, expected);

    }

    public void testArgs3() throws Throwable {
        String expected = "" +
                "def test(arg,attribute,a=10,b=20,*args,**kwargs):\n"
                +
                "    return Parent.test(arg,attribute,a=a,b=b,*args,**kwargs)\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7);
        checkPrettyPrintEqual(expected, expected);

    }

    public void testCalledDecorator() throws Throwable {
        final String s = "" +
                "class foo:\n" +
                "    @decorator()\n" +
                "    def method(self):\n" +
                "        pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer version) {
                checkPrettyPrintEqual(s, s);
                return true;
            }
        });
    }
}
