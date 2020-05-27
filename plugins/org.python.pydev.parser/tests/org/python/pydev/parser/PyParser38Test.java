/**
 * Copyright (c) 2019 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.util.List;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class PyParser38Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser38Test test = new PyParser38Test();
            test.setUp();
            test.testPositionalArgs4();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser38Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_8);
    }

    public void testWalrusOperatorInIf() {
        String s = "" +
                "if a := test() > 20:\n" +
                "    pass\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testRegularIf() {
        String s = "" +
                "if a > 2:\n" +
                "    pass\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testWalrusOperatorInElif() {
        String s = "" +
                "if a > 2:\n" +
                "    pass\n" +
                "elif a := test() > 20:\n" +
                "    pass\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testWalrusOperatorInGlobal() {
        String s = "" +
                "(a:=1)\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testWalrusOperatorInLstComp() {
        String s = "" +
                "[y := f(x), y**2, y**3]\n" +
                "";
        parseLegalDocStr(s);
    }

    public void testWalrusOperatorMultiple() throws Exception {
        String[] multiple = new String[] {
                "(a := 1)",
                "(a := a)",
                "if (match := pattern.search(data)) is None: pass",
                "[y := f(x), y**2, y**3]",
                "filtered_data = [y for x in data if (y := f(x)) is None]",
                "(y := f(x))",
                "y0 = (y1 := f(x))",
                "foo(x=(y := f(x)))",
                "def foo(answer=42): pass",
                "def foo(answer=(p := 42)): pass",
                "def foo(answer: (p := 42) = 5): pass",
                "lambda: (x := 1)",
                "(x := lambda: 1)",
                "(x := lambda: (y := 1))",
                "lambda line: (m := re.match(pattern, line)) and m.group(1)",
                "x = (y := 0)",
                "(z:=(y:=(x:=0)))",
                "(info := (name, phone, *rest))",
                "(x:=1,2)",
                "(total := total + tax)",
                "len(lines := f.readlines())",
                "foo(x := 3, cat='vector')",
                "foo(cat=(category := 'vector'))",
                "if any(len(longline := l) >= 100 for l in lines): print(longline)",
                "if env_base := os.environ.get('PYTHONUSERBASE', None): return env_base",
                "if self._is_special and (ans := self._check_nans(context=context)): return ans",
                "foo(b := 2, a=1)",
                "foo(b := 2, a=1)",
                "foo((b := 2), a=1)",
                "foo(c=(b := 2), a=1)",
                "while match := pattern.search(f.read()): pass"
        };
        for (String s : multiple) {
            try {
                parseLegalDocStr(s);
            } catch (Throwable e) {
                throw new Exception("Error parsing: " + s, e);
            }
        }
    }

    public void testAnnotation() {
        parseLegalDocStr("x: Tuple[int, int] = 1, 2");
        parseLegalDocStr(""
                + "def f():\n"
                + "    x: int = yield");
    }

    public void testPositionalArgs() {
        parseLegalDocStr(""
                + "def positional_only_arg(a, /):"
                + "    pass");
    }

    public void testPositionalArgs2() {
        parseLegalDocStr(""
                + "def all_markers(a, b, /, c, d, *, e, f):"
                + "    pass");
    }

    public void testPositionalArgs3() {
        parseLegalDocStr(""
                + "def all_markers_with_defaults(a, b=1, /, c=2, d=3, *, e=4, f=5):"
                + "    pass");
    }

    public void testPositionalArgs4() {
        parseLegalDocStr("a = lambda a, b=1, /, c=2, d=3, *, e=4, f=5: None");
    }

    private void checkStr(String s, String expected, boolean binary, boolean fstring, boolean unicode, boolean raw) {
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(parseLegalDocStr(s));
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

    public void testIterableUnpack() {
        parseLegalDocStr("def parse():\n    return x, *y");
        parseLegalDocStr("def parse():\n    return (x, *y)");
        parseLegalDocStr("def parse():\n    return");
        parseLegalDocStr("def parse():\n    return ()");
    }

    public void testIterableUnpackYield() {
        parseLegalDocStr("def parse():\n    yield");
        parseLegalDocStr("def parse():\n    yield x, *y");
        parseLegalDocStr("def parse():\n    yield (x, *y)");
        parseLegalDocStr("def parse():\n    yield ()");
    }

}
