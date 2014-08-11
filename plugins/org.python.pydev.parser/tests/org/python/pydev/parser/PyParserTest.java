/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 27/08/2005
 */
package org.python.pydev.parser;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.Timer;

public class PyParserTest extends PyParserTestBase {

    private final boolean STRESS_TEST = false;

    public static void main(String[] args) {
        try {
            PyParserTest test = new PyParserTest();
            test.setUp();

            //            Timer timer = new Timer();
            //            test.parseFilesInDir(new File("D:/bin/Python27/Lib/site-packages/wx-2.8-msw-unicode"), true);
            //            for(int i=0;i<4;i++){
            //                test.parseFilesInDir(new File("D:/bin/Python27/Lib/"), false);
            //            }
            //            timer.printDiff();
            test.testOnCompleteLib();
            test.testOnWxPython();
            test.tearDown();

            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParserTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testTryReparse() throws BadLocationException {
        Document doc = new Document("");
        for (int i = 0; i < 5; i++) {
            doc.replace(0, 0, "this is a totally and completely not parseable doc\n");
        }

        PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(doc, IPythonNature.LATEST_GRAMMAR_VERSION);
        ParseOutput reparseDocument = PyParser.reparseDocument(parserInfo);
        assertTrue(reparseDocument.ast == null);
        assertTrue(reparseDocument.error != null);
    }

    public void testCorrectArgs() throws Throwable {
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                String s = "" +
                        "class Class1:         \n" +
                        "    def met1(self, a):\n" +
                        "        pass";
                SimpleNode node = parseLegalDocStr(s);
                Module m = (Module) node;
                ClassDef d = (ClassDef) m.body[0];
                FunctionDef f = (FunctionDef) d.body[0];
                assertEquals("self", ((Name) f.args.args[0]).id);
                assertEquals("a", ((Name) f.args.args[1]).id);
                return true;
            }
        });
    }

    public void testMultilineStr() throws Throwable {
        final String s = "" +
                "a = '''\n" +
                "really really big string\n" +
                "really really big string\n"
                +
                "really really big string\n" +
                "really really big string\n" +
                "really really big string\n"
                +
                "really really big string\n" +
                "really really big string\n" +
                "really really big string\n"
                +
                "really really big string\n" +
                "really really big string\n" +
                "really really big string\n"
                +
                "really really big string\n" +
                "really really big string\n" +
                "really really big string\n"
                +
                "really really big string\n" +
                "really really big string\n" +
                "really really big string\n"
                +
                "really really big string\n" +
                "really really big string\n" +
                "really really big string\n"
                +
                "really really big string\n" +
                "really really big string\n" +
                "'''";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });

    }

    public void testPassSame() throws Throwable {
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                String s = "" +
                        "pass\n" +
                        "pass";
                Module m = (Module) parseLegalDocStr(s);
                assertEquals(2, m.body.length);
                Pass p1 = (Pass) m.body[0];
                Pass p2 = (Pass) m.body[1];
                //must intern specials in the same pass.
                assertSame(((SpecialStr) p1.specialsBefore.get(0)).str, ((SpecialStr) p2.specialsBefore.get(0)).str);
                return true;
            }
        });

    }

    public void testErr() throws Throwable {
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                String s = "" +
                        "def m():\n" +
                        "    call(a,";
                Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
                Module m = (Module) tup.o1;
                assertEquals(1, m.body.length);
                FunctionDef f = (FunctionDef) m.body[0];
                assertEquals("m", NodeUtils.getRepresentationString(f));
                return true;
            }
        });

    }

    public void testEmptyBaseForClass() throws Throwable {
        final String s = "" +
                "class B2(): pass\n" +
                "\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                if (arg == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4) {
                    parseILegalDocSuccessfully(s);
                } else {
                    parseLegalDocStr(s);
                }
                return true;
            }
        });
    }

    public void testFor2() throws Throwable {
        final String s = "" +
                "[x for x in 1,2,3,4]\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                if (arg == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    //yeap, invalid in python 3.0
                    parseILegalDocStr(s);
                } else {
                    parseLegalDocStr(s);
                }
                return true;
            }
        });
    }

    public void testFor2a() throws Throwable {
        final String s = "" +
                "[x for x in 2,3,4 if x > 2]\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                if (arg == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    //yeap, invalid in python 3.0
                    parseILegalDocStr(s);
                } else {
                    parseLegalDocStr(s);
                }
                return true;
            }
        });
    }

    public void testFor3() throws Throwable {
        final String s = "" +
                "[x() for x in lambda: True, lambda: False if x() ] \n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                if (arg == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
                    //yeap, invalid in python 3.0
                    parseILegalDocStr(s);
                } else {
                    parseLegalDocStr(s);
                }
                return true;
            }
        });
    }

    public void testYield() throws Throwable {
        final String s = "" +
                "def m():\n" +
                "    yield 1";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testYield2() throws Throwable {
        final String s = "" +
                "class Generator:\n" +
                "    def __iter__(self): \n" +
                "        for a in range(10):\n"
                +
                "            yield foo(a)\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testDecorator() throws Throwable {
        final String s = "" +
                "class C:\n" +
                "    \n" +
                "    @staticmethod\n" +
                "    def m():\n" +
                "        pass\n"
                +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testDecorator2() throws Throwable {
        final String s = "" +
                "@funcattrs(status=\"experimental\", author=\"BDFL\")\n" +
                "@staticmethod\n"
                +
                "def longMethodNameForEffect(*args):\n" +
                "    pass\n" +
                "\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testDecorator4() throws Throwable {
        final String s = "" +
                "@funcattrs(1)\n" +
                "def longMethodNameForEffect(*args):\n" +
                "    pass\n" +
                "\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testDecorator5() throws Throwable {
        final String s = "" +
                "@funcattrs(a)\n" +
                "def longMethodNameForEffect(*args):\n" +
                "    funcattrs(1)\n" +
                "\n"
                +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testDecorator3() throws Throwable {
        final String s = "" +
                "@funcattrs(a, 1, status=\"experimental\", author=\"BDFL\", *args, **kwargs)\n"
                +
                "@staticmethod1\n" +
                "@staticmethod2(b)\n" +
                "def longMethodNameForEffect(*args):\n" +
                "    pass\n"
                +
                "\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testDecorator6() throws Throwable {
        final String s = "" +
                "@funcattrs(b for b in x)\n" +
                "def longMethodNameForEffect(*args):\n" +
                "    pass\n"
                +
                "\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testOnWxPython() throws Throwable {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        if (TestDependent.PYTHON_WXPYTHON_PACKAGES != null) {
            boolean recursive = STRESS_TEST;
            File file = new File(TestDependent.PYTHON_WXPYTHON_PACKAGES, "wxPython");
            Timer timer = new Timer();

            parseFilesInDir(file, recursive, false); //Don't generate ast
            timer.printDiff("Time to generate without AST");

            parseFilesInDir(file, recursive, true);
            timer.printDiff("Time to generate with AST");

            file = new File(TestDependent.PYTHON_WXPYTHON_PACKAGES, "wx");
            parseFilesInDir(file, recursive, false); //Don't generate ast
            timer.printDiff("Time to generate without AST");

            parseFilesInDir(file, recursive, true);
            timer.printDiff("Time to generate with AST");
        }
    }

    public void testOnCompleteLib() throws Throwable {
        File file = new File(TestDependent.PYTHON_LIB);
        boolean recursive = STRESS_TEST;
        Timer timer = new Timer();
        parseFilesInDir(file, recursive);
        timer.printDiff("Time to generate with AST");

        parseFilesInDir(file, recursive, false); //Don't generate ast
        timer.printDiff("Time to generate without AST");
    }

    private void parseFilesInDir(File file) {
        parseFilesInDir(file, false);
    }

    //    not removed completely because we may still want to debug it later...
    //    public void testOnCsv() throws Throwable {
    //        PyParser.USE_FAST_STREAM = false;
    //        String loc = TestDependent.PYTHON_LIB+"csv.py";
    //        String s = FileUtils.getFileContents(new File(loc));
    //        parseLegalDocStr(s);
    //
    //        PyParser.USE_FAST_STREAM = true;
    //        loc = TestDependent.PYTHON_LIB+"csv.py";
    //        s = FileUtils.getFileContents(new File(loc));
    //        parseLegalDocStr(s);
    //    }

    public void testOnCgiMod() throws Throwable {
        final String s = "dict((day, index) for index, daysRep in enumeratedDays for day in daysRep)";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testOnCgiMod2() throws Throwable {
        String loc = TestDependent.PYTHON_LIB +
                "cgi.py";
        String s = FileUtils.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }

    //    this should really give errors (but is not a priority)
    //    public void testErrOnFor() throws Throwable {
    //        //ok, it should throw errors in those cases (but that's not so urgent)
    //        String s = "foo(x for x in range(10), 100)\n";
    //        parseILegalDoc(new Document(s));
    //
    //        String s1 = "foo(100, x for x in range(10))\n";
    //        parseILegalDoc(new Document(s1));
    //
    //    }

    public void testOnTestGrammar() throws Throwable {
        // Fails because the "standard" test files are not where the tests expect.
        // TODO might be solvable by installing python source package?
        // TODO the loc here should use TestDependent.PYTHON_TEST_PACKAGES
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        String loc = TestDependent.PYTHON_LIB +
                "test/test_grammar.py";
        String s = FileUtils.getFileContents(new File(loc));
        parseLegalDocStr(s, "(file: test_grammar.py)");
    }

    public void testSimple() throws Throwable {
        final String s = "" +
                "if maxint == 10:\n" +
                "    for s in 'a':\n" +
                "        pass\n" +
                "else:\n"
                +
                "    pass\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testOnTestContextLib() throws Throwable {
        // Fails because the "standard" test files are not where the tests expect.
        // TODO might be solvable by installing python source package?
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON_TEST_PACKAGES != null) {
            String loc = TestDependent.PYTHON_TEST_PACKAGES +
                    "test_contextlib.py";
            String s = FileUtils.getFileContents(new File(loc));
            parseLegalDocStr(s, "(file: test_contextlib.py)");
        }
    }

    public void testOnCalendar() throws Throwable {
        String loc = TestDependent.PYTHON_LIB +
                "hmac.py";
        String s = FileUtils.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }

    public void testOnUnittestMod() throws Throwable {
        // fails on Python >= 2.7 because unittest became a dir instead of one file.
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        String loc = TestDependent.PYTHON_LIB +
                "unittest.py";
        String s = FileUtils.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }

    public void testOnCodecsMod() throws Throwable {
        String loc = TestDependent.PYTHON_LIB +
                "codecs.py";
        String s = FileUtils.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }

    public void testOnDocBaseHTTPServer() throws Throwable {
        String loc = TestDependent.PYTHON_LIB +
                "BaseHTTPServer.py";
        String s = FileUtils.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }

    public void testOnDocXMLRPCServerMod() throws Throwable {
        String loc = TestDependent.PYTHON_LIB +
                "DocXMLRPCServer.py";
        String s = FileUtils.getFileContents(new File(loc));
        parseLegalDocStr(s);
    }

    public void testNewImportParser() throws Throwable {
        final String s = "" +
                "from a import (b,\n" +
                "            c,\n" +
                "            d)\n" +
                "\n" +
                "\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testNewImportParser2() throws Throwable {
        final String s = "" +
                "from a import (b,\n" +
                "            c,\n" +
                "            )\n" +
                "\n" +
                "\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testNewImportParser3() throws Throwable {
        final String s = "" +
                "from a import (b,\n" +
                "            c,,\n" + //err
                "            )\n" +
                "\n" +
                "\n" +
                "";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseILegalDocStr(s);
                //        Tuple<SimpleNode, Throwable> tup = parseILegalDocSuccessfully(s);
                //        Module m = (Module) tup.o1;
                //        ImportFrom i = (ImportFrom) m.body[0];
                //        assertEquals("a", NodeUtils.getRepresentationString(i.module));
                return true;
            }
        });
    }

    public void testParser() throws Throwable {
        String s = "class C: pass";
        parseLegalDocStr(s);
    }

    public void testEndWithComment() throws Throwable {
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                String s = "class C: \n" +
                        "    pass\n" +
                        "#end\n" +
                        "";
                Module ast = (Module) parseLegalDocStr(s);
                ClassDef d = (ClassDef) ast.body[0];
                assertEquals(1, d.specialsAfter.size());
                commentType c = (commentType) d.specialsAfter.get(0);
                assertEquals("#end", c.id);
                return true;
            }
        });

    }

    public void testOnlyComment() throws Throwable {
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                String s = "#end\n" +
                        "\n" +
                        "";
                Module ast = (Module) parseLegalDocStr(s);
                assertEquals(1, ast.specialsBefore.size());
                commentType c = (commentType) ast.specialsBefore.get(0);
                assertEquals("#end", c.id);
                return true;
            }
        });

    }

    @Override
    public void testEmpty() throws Throwable {
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                String s = "";
                Module ast = (Module) parseLegalDocStr(s);
                assertNotNull(ast);
                return true;
            }
        });
    }

    public void testParser7() throws Throwable {
        String s = "" +
                "if a < (2, 2):\n" +
                "    False, True = 0, 1\n" +
                "\n" +
                "\n";
        parseLegalDocStr(s);
    }

    public void testParser8() throws Throwable {
        String s = "" +
                "if type(clsinfo) in (types.TupleType, types.ListType):\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "\n";
        parseLegalDocStr(s);
    }

    public void testParser2() throws Throwable {
        String s = "" +
                "td = dict()                                                            \n"
                +
                "                                                                       \n"
                +
                "for foo in sorted(val for val in td.itervalues() if val[0] == 's'):    \n"
                +
                "    print foo                                                          \n";

        parseLegalDocStr(s);
    }

    public void testParser13() throws Throwable {
        final String s = "plural = lambda : None";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });

    }

    public void testParser3() throws Throwable {
        String s = "print (x for x in y)";

        parseLegalDocStr(s);
    }

    public void testParser4() throws Throwable {
        String s = "print sum(x for x in y)";

        parseLegalDocStr(s);
    }

    public void testParser5() throws Throwable {
        String s = "print sum(x.b for x in y)";

        parseLegalDocStr(s);
    }

    public void testParser6() throws Throwable {
        String s = "" +
                "import re\n" +
                "def firstMatch(s,regexList):\n"
                +
                "    for match in (regex.search(s) for regex in regexList):\n" +
                "        if match: return match\n"
                +
                "\n" +
                "\n";
        parseLegalDocStr(s);
    }

    public void testParser9() throws Throwable {
        String s = "" +
                "a[1,]\n" +
                "a[1,2]\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n";
        parseLegalDocStr(s);
    }

    /**
     * l = [ "encode", "decode" ]
     *
     * expected beginCols at: 7 and 17
     */
    public void testParser10() throws Throwable {
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                String s = "" +
                        "l = [ \"encode\", \"decode\" ] \n" +
                        "\n";
                SimpleNode node = parseLegalDocStr(s);
                List<ASTEntry> strs = SequencialASTIteratorVisitor.create(node).getAsList(new Class[] { Str.class });
                assertEquals(7, strs.get(0).node.beginColumn);
                assertEquals(17, strs.get(1).node.beginColumn);
                return true;
            }
        });
    }

    public void testParser11() throws Throwable {
        final String s = "" +
                "if True:\n" +
                "    pass\n" +
                "elif True:\n" +
                "    pass\n" +
                "else:\n" +
                "    pass\n"
                +
                "\n" +
                "\n";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testParser12() throws Throwable {
        final String s = "" +
                "m1()\n" +
                "\n";
        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testParser14() throws Throwable {
        final String s = "" +
                "assert False\n" +
                "result = []\n" +
                "for text in header_values:\n" +
                "    pass\n";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testParser15() throws Throwable {
        final String s = "" +
                "def f():\n" +
                "    return \"(\" + (";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseILegalDocStr(s);
                return true;
            }
        });
    }

    public void testParser16() throws Throwable {
        final String s = "" +
                "def f():\n" +
                "    return \"(\" + ()";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer arg) {
                parseLegalDocStr(s);
                return true;
            }
        });
    }

    public void testParser17() throws Throwable {
        final String s = "" +
                "yield 1\n";

        checkWithAllGrammars(new ICallback<Boolean, Integer>() {

            public Boolean call(Integer grammar) {
                parseILegalDocSuccessfully(s);
                return true;
            }
        });
    }

    public void testParserAs1() throws Throwable {
        final String s = "" +
                "as = 1\n" +
                "print as\n" +
                "" +
                "with = 1\n" +
                "print with\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4);
        parseLegalDocStr(s);
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        parseLegalDocStr(s);
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6);
        parseILegalDocStr(s);
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7);
        parseILegalDocStr(s);
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        parseILegalDocStr(s);
    }

    public void testParserPrint() throws Throwable {
        String s = "" +
                "import os.print.os\n" +
                "print os.print.os\n" +
                "";

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4);
        parseLegalDocStr(s);
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        parseLegalDocStr(s);

        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6);
        parseILegalDocStr(s);
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7);
        parseILegalDocStr(s);
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        parseILegalDocStr(s);

        s = "" +
                "import os.print.os\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testThreadingInParser() throws Exception {
        // fails on Python >= 2.7 because unittest became a dir instead of one file.
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        String loc = TestDependent.PYTHON_LIB +
                "unittest.py";
        String s = FileUtils.getFileContents(new File(loc));

        final Integer[] calls = new Integer[] { 0 };
        final Boolean[] failedComparisson = new Boolean[] { false };

        ICallback<Object, Boolean> callback = new ICallback<Object, Boolean>() {

            public Object call(Boolean failTest) {
                synchronized (calls) {
                    calls[0] = calls[0] + 1;
                    if (failTest) {
                        failedComparisson[0] = true;
                    }
                    return null;
                }
            }

        };
        SimpleNode node = parseLegalDocStr(s);
        String expected = printNode(node);

        int expectedCalls = 70;
        Timer timer = new Timer();
        for (int j = 0; j < expectedCalls; j++) {
            startParseThread(s, callback, expected);
        }

        while (calls[0] < expectedCalls) {
            synchronized (this) {
                wait(5);
            }
        }
        timer.printDiff();
        assertTrue(!failedComparisson[0]);
    }

    private String printNode(SimpleNode node) {
        PrettyPrinterV2 prettyPrinterV2 = new PrettyPrinterV2(new PrettyPrinterPrefsV2("\n", "    ", versionProvider));
        try {
            return prettyPrinterV2.print(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //      final WriterEraser stringWriter = new WriterEraser();
        //      PrettyPrinterPrefs prettyPrinterPrefs = new PrettyPrinterPrefs("\n");
        //      prettyPrinterPrefs.setSpacesAfterComma(1);
        //      prettyPrinterPrefs.setSpacesBeforeComment(1);
        //      PrettyPrinter printer = new PrettyPrinter(prettyPrinterPrefs, stringWriter);
        //      try {
        //          node.accept(printer);
        //          return stringWriter.getBuffer().toString();
        //      } catch (Exception e) {
        //          throw new RuntimeException(e);
        //      }
    }

    private void startParseThread(final String contents, final ICallback<Object, Boolean> callback,
            final String expected) {

        new Thread() {
            @Override
            public void run() {
                try {
                    SimpleNode node = parseLegalDocStr(contents);
                    if (!printNode(node).equals(expected)) {
                        callback.call(true); //Comparison failed
                    } else {
                        callback.call(false);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    callback.call(true); //something bad happened... so, the test failed!
                }

            }
        }.start();

    }
}
