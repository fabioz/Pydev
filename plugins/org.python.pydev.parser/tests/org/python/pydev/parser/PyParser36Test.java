/**
 * Copyright (c) 2016 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.io.File;
import java.util.List;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.grammar36.PythonGrammar36;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;
import org.python.pydev.shared_core.io.FileUtils;

public class PyParser36Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser36Test test = new PyParser36Test();
            test.setUp();
            test.testCommaAfterKwArgs1();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser36Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_6);
    }

    public void testCommaAfterKwArgs() {
        String s = "" +
                "def foo(**kwargs):\n" +
                "    pass\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testCommaAfterKwArgs1() {
        String s = "" +
                "x=lambda a,**kwargs,:None" +
                "";
        parseLegalDocStr(s);
    }

    public void testAnnotatedVar() {
        String s = "" +
                "x: int = 10" +
                "";
        parseLegalDocStr(s);
    }

    public void testTestGrammar36() {
        String contents = FileUtils.getFileContents(new File(TestDependent.TEST_PYDEV_PARSER_PLUGIN_LOC +
                "/tests/org/python/pydev/parser/python_test_grammar_36.py"));

        parseLegalDocStr(contents);
        parseLegalDocStrWithoutTree(contents);
    }

    public void testAsyncComprehension() {
        String s = "" +
                "async def f():\n" +
                "    [i async for b in c]" +
                "";
        parseLegalDocStr(s);
    }

    public void testIntSeparators() {
        String s = "" +
                "a = 1_000_000\n" +
                "b = 0x00_ff_aa\n" +
                "c = 0b001_0010_1111\n" +
                "d = 0o00_44_55\n" +
                "f = 1_34_3.45_45\n" +
                "f = 1_34_3.45_45e12_23\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testFString() {
        String s = "" +
                "a = f'some{string}'\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testEvalInput() throws ParseException {
        String s = "" +
                "call(20, 20), 20\n" +
                "";
        FastCharStream in = new FastCharStream(s.toCharArray());
        PythonGrammar36 grammar = new PythonGrammar36(true, in);
        Expr eval_input = grammar.eval_input();
        assertEquals(
                "Expr[value=Tuple[elts=[Call[func=Name[id=call, ctx=Load, reserved=false], args=[Num[n=20, type=Int, num=20], Num[n=20, type=Int, num=20]], keywords=[], starargs=null, kwargs=null], Num[n=20, type=Int, num=20]], ctx=Load, endsWithComma=false]]",
                eval_input.toString());

    }

    public void testAsync() {
        String s = "" +
                "async def test2(session):\n" +
                "    async with session:\n" +
                "        async for _msg in session:\n" +
                "            pass\n" +
                "\n" +
                "    # broken grammar\n" +
                "    await session\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testFString1() {
        String s = "" +
                "def fn():\n" +
                "    if True:\n" +
                "        return f\"\"\n" +
                "" +
                "";
        parseLegalDocStr(s);
    }

    private void checkStr(String s, String expected, boolean binary, boolean fstring, boolean unicode, boolean raw) {
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(parseLegalDocStr(s), true);
        List<ASTEntry> asList = visitor.getAsList(Str.class);
        assertEquals(asList.size(), 1);
        ASTEntry entry = asList.iterator().next();
        Str str = (Str) entry.node;
        assertEquals(binary, str.binary);
        assertEquals(fstring, str.fstring);
        assertEquals(unicode, str.unicode);
        assertEquals(raw, str.raw);
        assertEquals(expected, str.s);
    }

    public void testStr() throws Exception {
        checkStr("''", "", false, false, false, false);
        checkStr("'s'", "s", false, false, false, false);
        checkStr("r''", "", false, false, false, true);
        checkStr("r's'", "s", false, false, false, true);

        checkStr("b''", "", true, false, false, false);
        checkStr("b's'", "s", true, false, false, false);
        checkStr("br''", "", true, false, false, true);
        checkStr("br's'", "s", true, false, false, true);
        checkStr("rb''", "", true, false, false, true);
        checkStr("rb's'", "s", true, false, false, true);

        checkStr("f''", "", false, true, false, false);
        checkStr("f's'", "s", false, true, false, false);
        checkStr("fr''", "", false, true, false, true);
        checkStr("fr's'", "s", false, true, false, true);
        checkStr("rf''", "", false, true, false, true);
        checkStr("rf's'", "s", false, true, false, true);
    }

}
