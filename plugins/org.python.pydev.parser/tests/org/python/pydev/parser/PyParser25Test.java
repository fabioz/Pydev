/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 1, 2006
 * @author Fabio
 */
package org.python.pydev.parser;

import org.eclipse.jface.text.Document;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.WithItem;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * Test for parsing python 2.5
 * @author Fabio
 */
public class PyParser25Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser25Test test = new PyParser25Test();
            test.setUp();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser25Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testForWithCondExp() {
        checkWithAllGrammars((grammarVersion) -> {
            String s = ""
                    +
                    "verify([ x(False) for x in (lambda x: False if x else True, lambda x: True if x else False) if x(False) ] == [True])\n"
                    +
                    "";
            parseLegalDocStr(s);
            return true;
        });
    }

    /**
     * This test checks the new conditional expression.
     */
    public void testConditionalExp1() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "a = 1 if True else 2\n";
            parseLegalDocStr(str);
            return true;
        });
    }

    /**
     * This test checks the new conditional expression.
     */
    public void testNewYield() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "def counter (maximum):\n" +
                    "    i = 0\n" +
                    "    while i < maximum:\n"
                    +
                    "        val = (yield i)\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });

    }

    /**
     * This test checks the new conditional expression.
     */
    public void testEmptyYield() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "def whee():\n" +
                    "    yield\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });

    }

    /**
     * This test checks the new relative import
     */
    public void testNewRelativeImport() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "from . import foo\n";
            Module mod = (Module) parseLegalDocStr(str);
            ImportFrom f = (ImportFrom) mod.body[0];
            assertEquals(1, f.level);
            assertEquals("", ((NameTok) f.module).id);
            return true;
        });

    }

    public void testNewRelativeImport2() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "from ..bar import foo\n";
            Module mod = (Module) parseLegalDocStr(str);
            ImportFrom f = (ImportFrom) mod.body[0];
            assertEquals(2, f.level);
            assertEquals("bar", ((NameTok) f.module).id);
            return true;
        });

    }

    public void testNewRelativeImport3() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "from ... import foo\n";
            Module mod = (Module) parseLegalDocStr(str);
            ImportFrom f = (ImportFrom) mod.body[0];
            assertEquals(3, f.level);
            assertEquals("", ((NameTok) f.module).id);
            return true;
        });

    }

    public void testNewRelativeImport4() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "from ...bar import foo\n";
            Module mod = (Module) parseLegalDocStr(str);
            ImportFrom f = (ImportFrom) mod.body[0];
            assertEquals(3, f.level);
            assertEquals("bar", ((NameTok) f.module).id);
            return true;
        });

    }

    public void testImportFails() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "from import foo\n";
            parseILegalDoc(new Document(str));
            return true;
        });

    }

    public void testNewWithStmt() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "from __future__ import with_statement\n" +
                    "with foo:\n" +
                    "    print('bla')\n" +
                    "";
            //we'll actually treat this as a try..finally with a body with try..except..else
            Module mod = (Module) parseLegalDocStr(str);
            assertEquals(2, mod.body.length);
            assertTrue(mod.body[1] instanceof With);
            With w = (With) mod.body[1];
            assertTrue(((WithItem) w.with_item[0]).optional_vars == null);
            return true;
        });

    }

    public void testNewWithStmt2() {

        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "from __future__ import with_statement\n" +
                    "with foo as x:\n" +
                    "    print('bla')\n" +
                    "";
            //we'll actually treat this as a try..finally with a body with try..except..else
            Module mod = (Module) parseLegalDocStr(str);
            assertEquals(2, mod.body.length);
            assertTrue(mod.body[1] instanceof With);
            With w = (With) mod.body[1];
            assertTrue(((WithItem) w.with_item[0]).optional_vars != null);
            return true;
        });

    }

    public void testNewWithStmt3() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "from __future__ import division, with_statement\n" +
                    "with foo as x:\n"
                    +
                    "    print('bla')\n" +
                    "";
            //we'll actually treat this as a try..finally with a body with try..except..else
            Module mod = (Module) parseLegalDocStr(str);
            assertEquals(2, mod.body.length);
            assertTrue(mod.body[1] instanceof With);
            With w = (With) mod.body[1];
            assertTrue(((WithItem) w.with_item[0]).optional_vars != null);
            return true;
        });

    }

    public void testNewWithStmt4() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "from __future__ import (division, with_statement)\n" +
                    "with foo as x:\n"
                    +
                    "    print('bla')\n" +
                    "";
            Module mod = (Module) parseLegalDocStr(str);
            assertEquals(2, mod.body.length);
            assertTrue(mod.body[1] instanceof With);
            With w = (With) mod.body[1];
            assertTrue(((WithItem) w.with_item[0]).optional_vars != null);
            return true;
        });

    }

    public void testNewWithStmt5() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "\n" +
                    "from __future__ import with_statement\n" +
                    "\n" +
                    "def fun():\n"
                    +
                    "   with open('somepath') as f:\n" +
                    "       return f.read()\n" +
                    "";
            Module mod = (Module) parseLegalDocStr(str);
            assertEquals(2, mod.body.length);
            return true;
        });

    }

    public void testNewTryFinally() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "try:\n" +
                    "    'try'\n" +
                    "except:\n" +
                    "    'except'\n" +
                    "else:\n" +
                    "    'else'\n"
                    +
                    "finally:\n" +
                    "    'finally'\n" +
                    "\n" +
                    "";
            //we'll actually treat this as a try..finally with a body with try..except..else
            Module mod = (Module) parseLegalDocStr(str);
            assertEquals(1, mod.body.length);
            TryFinally f = (TryFinally) mod.body[0];

            assertEquals(1, f.body.length);
            TryExcept exc = (TryExcept) f.body[0];
            assertTrue(exc.orelse != null);
            assertEquals(1, exc.handlers.length);
            return true;
        });

    }

    /**
     * This test checks that the old version still gives an error
     */
    public void testWith() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "with foo:\n" +
                    "    print('bla')\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });

    }

    /**
     * This test checks that the old version still gives an error
     */
    public void testWith2() {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "from __future__ import with_statement\n" + //1
                    "\n" +
                    "\n" +
                    "class OBJ:\n" + //4
                    "    def Install(self):\n" +
                    "        pass\n" +
                    "\n" +
                    "class Test:\n" + //8
                    "\n" +
                    "\n" +
                    "    def test1(self):\n" + //11
                    "        print('here')\n" +
                    "\n" +
                    "\n" +
                    "    def test2(self):\n" + //15
                    "        with a:\n" + //16
                    "            pass\n" + //17
                    "";
            SimpleNode ast = parseLegalDocStr(str);
            stmtType[] body = ((Module) ast).body;
            assertEquals(3, body.length);
            ClassDef test = (ClassDef) body[2];
            body = test.body;
            assertEquals(2, body.length);
            assertEquals(8, test.beginLine);
            return true;
        });

    }

    public void testSuiteLineNumber() throws Exception {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "class Process:\n" +
                    "\n" +
                    "    def Foo(self):\n" +
                    "        if a == 1:\n"
                    +
                    "            pass\n" +
                    "        elif a == 1:\n" +
                    "            pass\n" +
                    "\n" +
                    "";
            SimpleNode ast = parseLegalDocStr(str);
            stmtType[] body = ((Module) ast).body;
            assertEquals(1, body.length);
            ClassDef classFound = (ClassDef) body[0];
            body = classFound.body;
            assertEquals(1, body.length);
            FunctionDef func = (FunctionDef) body[0];
            If ifFound = (If) func.body[0];
            assertEquals(6, ifFound.orelse.beginLine);
            return true;
        });

    }

    public void testJythonParsing2() throws Exception {
        checkWithAllGrammars((grammarVersion) -> {
            String str = "" +
                    "import os.print.os\n" +
                    "print(os.print.os)\n" +
                    "";
            parseLegalDocStr(str);
            return true;
        });

    }

    @Override
    public void testEmpty() throws Throwable {
        checkWithAllGrammars((grammarVersion) -> {
            String s = "";

            parseLegalDocStr(s);
            return true;
        });

    }

}
