/**
 * Copyright (c) 2019 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;

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

    public void testUString() throws Exception {
        //INPUT, EXPECTED
        String[][] tests = new String[][] {
                //1
                { "u'{foo}'", "{foo}" },
                { "U'{foo}'", "{foo}" },
                { "ur'{foo}'", "{foo}" },
                { "uR'{foo}'", "{foo}" },
                { "Ur'{foo}'", "{foo}" },
                { "UR'{foo}'", "{foo}" },
                //2
                { "u'''{foo}'''", "{foo}" },
                { "U'''{foo}'''", "{foo}" },
                { "ur'''{foo}'''", "{foo}" },
                { "uR'''{foo}'''", "{foo}" },
                { "Ur'''{foo}'''", "{foo}" },
                { "UR'''{foo}'''", "{foo}" },
                //3
                { "u\"{foo}\"", "{foo}" },
                { "U\"{foo}\"", "{foo}" },
                { "ur\"{foo}\"", "{foo}" },
                { "uR\"{foo}\"", "{foo}" },
                { "Ur\"{foo}\"", "{foo}" },
                { "UR\"{foo}\"", "{foo}" },
                //4
                { "u\"\"\"{foo}\"\"\"", "{foo}" },
                { "U\"\"\"{foo}\"\"\"", "{foo}" },
                { "ur\"\"\"{foo}\"\"\"", "{foo}" },
                { "uR\"\"\"{foo}\"\"\"", "{foo}" },
                { "Ur\"\"\"{foo}\"\"\"", "{foo}" },
                { "UR\"\"\"{foo}\"\"\"", "{foo}" },
                //5
                { "u'foo'", "foo" },
                { "U'foo'", "foo" },
                { "ur'foo'", "foo" },
                { "uR'foo'", "foo" },
                { "Ur'foo'", "foo" },
                { "UR'foo'", "foo" },
                //6
                { "u'''foo'''", "foo" },
                { "U'''foo'''", "foo" },
                { "ur'''foo'''", "foo" },
                { "uR'''foo'''", "foo" },
                { "Ur'''foo'''", "foo" },
                { "UR'''foo'''", "foo" },
                //7
                { "u\"foo\"", "foo" },
                { "U\"foo\"", "foo" },
                { "ur\"foo\"", "foo" },
                { "uR\"foo\"", "foo" },
                { "Ur\"foo\"", "foo" },
                { "UR\"foo\"", "foo" },
                //8
                { "u\"\"\"foo\"\"\"", "foo" },
                { "U\"\"\"foo\"\"\"", "foo" },
                { "ur\"\"\"foo\"\"\"", "foo" },
                { "uR\"\"\"foo\"\"\"", "foo" },
                { "Ur\"\"\"foo\"\"\"", "foo" },
                { "UR\"\"\"foo\"\"\"", "foo" },
                //9
                { "u'foo{boo}'", "foo{boo}" },
                { "U'foo{boo}'", "foo{boo}" },
                { "ur'foo{boo}'", "foo{boo}" },
                { "uR'foo{boo}'", "foo{boo}" },
                { "Ur'foo{boo}'", "foo{boo}" },
                { "UR'foo{boo}'", "foo{boo}" },
                //10
                { "u'''foo{boo}'''", "foo{boo}" },
                { "U'''foo{boo}'''", "foo{boo}" },
                { "ur'''foo{boo}'''", "foo{boo}" },
                { "uR'''foo{boo}'''", "foo{boo}" },
                { "Ur'''foo{boo}'''", "foo{boo}" },
                { "UR'''foo{boo}'''", "foo{boo}" },
                //11
                { "u\"foo{boo}\"", "foo{boo}" },
                { "U\"foo{boo}\"", "foo{boo}" },
                { "ur\"foo{boo}\"", "foo{boo}" },
                { "uR\"foo{boo}\"", "foo{boo}" },
                { "Ur\"foo{boo}\"", "foo{boo}" },
                { "UR\"foo{boo}\"", "foo{boo}" },
                //12
                { "u\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "U\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "ur\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "uR\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "Ur\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "UR\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                //13
                { "u''", "" },
                { "U''", "" },
                { "ur''", "" },
                { "uR''", "" },
                { "Ur''", "" },
                { "UR''", "" },
                //14
                { "u''''''", "" },
                { "U''''''", "" },
                { "ur''''''", "" },
                { "uR''''''", "" },
                { "Ur''''''", "" },
                { "UR''''''", "" },
                //15
                { "u\"\"", "" },
                { "U\"\"", "" },
                { "ur\"\"", "" },
                { "uR\"\"", "" },
                { "Ur\"\"", "" },
                { "UR\"\"", "" },
                //16
                { "u\"\"\"\"\"\"", "" },
                { "U\"\"\"\"\"\"", "" },
                { "ur\"\"\"\"\"\"", "" },
                { "uR\"\"\"\"\"\"", "" },
                { "Ur\"\"\"\"\"\"", "" },
                { "UR\"\"\"\"\"\"", "" },
        };

        String output;
        String s;

        for (int i = 0; i < tests.length; i++) {
            try {
                output = parseLegalDocStr(tests[i][0]).toString();
                s = output.substring(output.indexOf("s=") + 2, output.indexOf(", type="));
                assertEquals(tests[i][1], s);
            } catch (Throwable e) {
                throw new Exception("Error parsing: " + tests[i][0], e);
            }
        }
    }

    public void testBString() throws Exception {
        //INPUT, EXPECTED
        String[][] tests = new String[][] {
                //1
                { "b'{foo}'", "b'{foo}'" },
                { "B'{foo}'", "b'{foo}'" },
                { "br'{foo}'", "b'{foo}'" },
                { "bR'{foo}'", "b'{foo}'" },
                { "Br'{foo}'", "b'{foo}'" },
                { "BR'{foo}'", "b'{foo}'" },
                //2
                { "b'''{foo}'''", "b'{foo}'" },
                { "B'''{foo}'''", "b'{foo}'" },
                { "br'''{foo}'''", "b'{foo}'" },
                { "bR'''{foo}'''", "b'{foo}'" },
                { "Br'''{foo}'''", "b'{foo}'" },
                { "BR'''{foo}'''", "b'{foo}'" },
                //3
                { "b\"{foo}\"", "b'{foo}'" },
                { "B\"{foo}\"", "b'{foo}'" },
                { "br\"{foo}\"", "b'{foo}'" },
                { "bR\"{foo}\"", "b'{foo}'" },
                { "Br\"{foo}\"", "b'{foo}'" },
                { "BR\"{foo}\"", "b'{foo}'" },
                //4
                { "b\"\"\"{foo}\"\"\"", "b'{foo}'" },
                { "B\"\"\"{foo}\"\"\"", "b'{foo}'" },
                { "br\"\"\"{foo}\"\"\"", "b'{foo}'" },
                { "bR\"\"\"{foo}\"\"\"", "b'{foo}'" },
                { "Br\"\"\"{foo}\"\"\"", "b'{foo}'" },
                { "BR\"\"\"{foo}\"\"\"", "b'{foo}'" },
                //5
                { "b'foo'", "b'foo'" },
                { "B'foo'", "b'foo'" },
                { "br'foo'", "b'foo'" },
                { "bR'foo'", "b'foo'" },
                { "Br'foo'", "b'foo'" },
                { "BR'foo'", "b'foo'" },
                //6
                { "b'''foo'''", "b'foo'" },
                { "B'''foo'''", "b'foo'" },
                { "br'''foo'''", "b'foo'" },
                { "bR'''foo'''", "b'foo'" },
                { "Br'''foo'''", "b'foo'" },
                { "BR'''foo'''", "b'foo'" },
                //7
                { "b\"foo\"", "b'foo'" },
                { "B\"foo\"", "b'foo'" },
                { "br\"foo\"", "b'foo'" },
                { "bR\"foo\"", "b'foo'" },
                { "Br\"foo\"", "b'foo'" },
                { "BR\"foo\"", "b'foo'" },
                //8
                { "b\"\"\"foo\"\"\"", "b'foo'" },
                { "B\"\"\"foo\"\"\"", "b'foo'" },
                { "br\"\"\"foo\"\"\"", "b'foo'" },
                { "bR\"\"\"foo\"\"\"", "b'foo'" },
                { "Br\"\"\"foo\"\"\"", "b'foo'" },
                { "BR\"\"\"foo\"\"\"", "b'foo'" },
                //9
                { "b'foo{boo}'", "b'foo{boo}'" },
                { "B'foo{boo}'", "b'foo{boo}'" },
                { "br'foo{boo}'", "b'foo{boo}'" },
                { "bR'foo{boo}'", "b'foo{boo}'" },
                { "Br'foo{boo}'", "b'foo{boo}'" },
                { "BR'foo{boo}'", "b'foo{boo}'" },
                //10
                { "b'''foo{boo}'''", "b'foo{boo}'" },
                { "B'''foo{boo}'''", "b'foo{boo}'" },
                { "br'''foo{boo}'''", "b'foo{boo}'" },
                { "bR'''foo{boo}'''", "b'foo{boo}'" },
                { "Br'''foo{boo}'''", "b'foo{boo}'" },
                { "BR'''foo{boo}'''", "b'foo{boo}'" },
                //11
                { "b\"foo{boo}\"", "b'foo{boo}'" },
                { "B\"foo{boo}\"", "b'foo{boo}'" },
                { "br\"foo{boo}\"", "b'foo{boo}'" },
                { "bR\"foo{boo}\"", "b'foo{boo}'" },
                { "Br\"foo{boo}\"", "b'foo{boo}'" },
                { "BR\"foo{boo}\"", "b'foo{boo}'" },
                //12
                { "b\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                { "B\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                { "br\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                { "bR\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                { "Br\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                { "BR\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                //13
                { "b''", "b''" },
                { "B''", "b''" },
                { "br''", "b''" },
                { "bR''", "b''" },
                { "Br''", "b''" },
                { "BR''", "b''" },
                //14
                { "b''''''", "b''" },
                { "B''''''", "b''" },
                { "br''''''", "b''" },
                { "bR''''''", "b''" },
                { "Br''''''", "b''" },
                { "BR''''''", "b''" },
                //15
                { "b\"\"", "b''" },
                { "B\"\"", "b''" },
                { "br\"\"", "b''" },
                { "bR\"\"", "b''" },
                { "Br\"\"", "b''" },
                { "BR\"\"", "b''" },
                //16
                { "b\"\"\"\"\"\"", "b''" },
                { "B\"\"\"\"\"\"", "b''" },
                { "br\"\"\"\"\"\"", "b''" },
                { "bR\"\"\"\"\"\"", "b''" },
                { "Br\"\"\"\"\"\"", "b''" },
                { "BR\"\"\"\"\"\"", "b''" },
        };

        String output;
        String s;

        for (int i = 0; i < tests.length; i++) {
            try {
                output = parseLegalDocStr(tests[i][0]).toString();
                s = output.substring(output.indexOf("s=") + 2, output.indexOf(", type="));
                assertEquals(tests[i][1], s);
            } catch (Throwable e) {
                throw new Exception("Error parsing: " + tests[i][0], e);
            }
        }
    }

    public void testFString() throws Exception {
        //INPUT, EXPECTED
        String[][] tests = new String[][] {
                //1
                { "f'{foo}'", "{foo}" },
                { "F'{foo}'", "{foo}" },
                { "fr'{foo}'", "{foo}" },
                { "fR'{foo}'", "{foo}" },
                { "Fr'{foo}'", "{foo}" },
                { "FR'{foo}'", "{foo}" },
                //2
                { "f'''{foo}'''", "{foo}" },
                { "F'''{foo}'''", "{foo}" },
                { "fr'''{foo}'''", "{foo}" },
                { "fR'''{foo}'''", "{foo}" },
                { "Fr'''{foo}'''", "{foo}" },
                { "FR'''{foo}'''", "{foo}" },
                //3
                { "f\"{foo}\"", "{foo}" },
                { "F\"{foo}\"", "{foo}" },
                { "fr\"{foo}\"", "{foo}" },
                { "fR\"{foo}\"", "{foo}" },
                { "Fr\"{foo}\"", "{foo}" },
                { "FR\"{foo}\"", "{foo}" },
                //4
                { "f\"\"\"{foo}\"\"\"", "{foo}" },
                { "F\"\"\"{foo}\"\"\"", "{foo}" },
                { "fr\"\"\"{foo}\"\"\"", "{foo}" },
                { "fR\"\"\"{foo}\"\"\"", "{foo}" },
                { "Fr\"\"\"{foo}\"\"\"", "{foo}" },
                { "FR\"\"\"{foo}\"\"\"", "{foo}" },
                //5
                { "f'foo'", "foo" },
                { "F'foo'", "foo" },
                { "fr'foo'", "foo" },
                { "fR'foo'", "foo" },
                { "Fr'foo'", "foo" },
                { "FR'foo'", "foo" },
                //6
                { "f'''foo'''", "foo" },
                { "F'''foo'''", "foo" },
                { "fr'''foo'''", "foo" },
                { "fR'''foo'''", "foo" },
                { "Fr'''foo'''", "foo" },
                { "FR'''foo'''", "foo" },
                //7
                { "f\"foo\"", "foo" },
                { "F\"foo\"", "foo" },
                { "fr\"foo\"", "foo" },
                { "fR\"foo\"", "foo" },
                { "Fr\"foo\"", "foo" },
                { "FR\"foo\"", "foo" },
                //8
                { "f\"\"\"foo\"\"\"", "foo" },
                { "F\"\"\"foo\"\"\"", "foo" },
                { "fr\"\"\"foo\"\"\"", "foo" },
                { "fR\"\"\"foo\"\"\"", "foo" },
                { "Fr\"\"\"foo\"\"\"", "foo" },
                { "FR\"\"\"foo\"\"\"", "foo" },
                //9
                { "f'foo{boo}'", "foo{boo}" },
                { "F'foo{boo}'", "foo{boo}" },
                { "fr'foo{boo}'", "foo{boo}" },
                { "fR'foo{boo}'", "foo{boo}" },
                { "Fr'foo{boo}'", "foo{boo}" },
                { "FR'foo{boo}'", "foo{boo}" },
                //10
                { "f'''foo{boo}'''", "foo{boo}" },
                { "F'''foo{boo}'''", "foo{boo}" },
                { "fr'''foo{boo}'''", "foo{boo}" },
                { "fR'''foo{boo}'''", "foo{boo}" },
                { "Fr'''foo{boo}'''", "foo{boo}" },
                { "FR'''foo{boo}'''", "foo{boo}" },
                //11
                { "f\"foo{boo}\"", "foo{boo}" },
                { "F\"foo{boo}\"", "foo{boo}" },
                { "fr\"foo{boo}\"", "foo{boo}" },
                { "fR\"foo{boo}\"", "foo{boo}" },
                { "Fr\"foo{boo}\"", "foo{boo}" },
                { "FR\"foo{boo}\"", "foo{boo}" },
                //12
                { "f\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "F\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "fr\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "fR\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "Fr\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "FR\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                //13
                { "f''", "" },
                { "F''", "" },
                { "fr''", "" },
                { "fR''", "" },
                { "Fr''", "" },
                { "FR''", "" },
                //14
                { "f''''''", "" },
                { "F''''''", "" },
                { "fr''''''", "" },
                { "fR''''''", "" },
                { "Fr''''''", "" },
                { "FR''''''", "" },
                //15
                { "f\"\"", "" },
                { "F\"\"", "" },
                { "fr\"\"", "" },
                { "fR\"\"", "" },
                { "Fr\"\"", "" },
                { "FR\"\"", "" },
                //16
                { "f\"\"\"\"\"\"", "" },
                { "F\"\"\"\"\"\"", "" },
                { "fr\"\"\"\"\"\"", "" },
                { "fR\"\"\"\"\"\"", "" },
                { "Fr\"\"\"\"\"\"", "" },
                { "FR\"\"\"\"\"\"", "" },
        };

        String output;
        String s;

        for (int i = 0; i < tests.length; i++) {
            try {
                output = parseLegalDocStr(tests[i][0]).toString();
                s = output.substring(output.indexOf("s=") + 2, output.indexOf(", type="));
                assertEquals(tests[i][1], s);
            } catch (Throwable e) {
                throw new Exception("Error parsing: " + tests[i][0], e);
            }
        }
    }

    // -- this should give error -- //
    public void testUStringStartingR() throws Exception {
        //INPUT, EXPECTED
        String[] tests = new String[] {
                //1
                "ru'{foo}'",
                "rU'{foo}'",
                "Ru'{foo}'",
                "RU'{foo}'",
                //2
                "ru'''{foo}'''",
                "rU'''{foo}'''",
                "Ru'''{foo}'''",
                "RU'''{foo}'''",
                //3
                "ru\"{foo}\"",
                "rU\"{foo}\"",
                "Ru\"{foo}\"",
                "RU\"{foo}\"",
                //4
                "ru\"\"\"{foo}\"\"\"",
                "rU\"\"\"{foo}\"\"\"",
                "Ru\"\"\"{foo}\"\"\"",
                "RU\"\"\"{foo}\"\"\"",
                //5
                "ru'foo'",
                "rU'foo'",
                "Ru'foo'",
                "RU'foo'",
                //6
                "ru'''foo'''",
                "rU'''foo'''",
                "Ru'''foo'''",
                "RU'''foo'''",
                //7
                "ru\"foo\"",
                "rU\"foo\"",
                "Ru\"foo\"",
                "RU\"foo\"",
                //8
                "ru\"\"\"foo\"\"\"",
                "rU\"\"\"foo\"\"\"",
                "Ru\"\"\"foo\"\"\"",
                "RU\"\"\"foo\"\"\"",
                //9
                "ru'foo{boo}'",
                "rU'foo{boo}'",
                "Ru'foo{boo}'",
                "RU'foo{boo}'",
                //10
                "ru'''foo{boo}'''",
                "rU'''foo{boo}'''",
                "Ru'''foo{boo}'''",
                "RU'''foo{boo}'''",
                //11
                "ru\"foo{boo}\"",
                "rU\"foo{boo}\"",
                "Ru\"foo{boo}\"",
                "RU\"foo{boo}\"",
                //12
                "ru\"\"\"foo{boo}\"\"\"",
                "rU\"\"\"foo{boo}\"\"\"",
                "Ru\"\"\"foo{boo}\"\"\"",
                "RU\"\"\"foo{boo}\"\"\"",
                //13
                "ru''",
                "rU''",
                "Ru''",
                "RU''",
                //14
                "ru''''''",
                "rU''''''",
                "Ru''''''",
                "RU''''''",
                //15
                "ru\"\"",
                "rU\"\"",
                "Ru\"\"",
                "RU\"\"",
                //16
                "ru\"\"\"\"\"\"",
                "rU\"\"\"\"\"\"",
                "Ru\"\"\"\"\"\"",
                "RU\"\"\"\"\"\"",
        };

        for (int i = 0; i < tests.length; i++) {
            try {
                parseLegalDocStr(tests[i]);
            } catch (Throwable e) {
                throw new Exception("Error parsing: " + tests[i], e);
            }
        }
    }

    public void testBStringStartingR() throws Exception {
        //INPUT, EXPECTED
        String[][] tests = new String[][] {
                //1
                { "rb'{foo}'", "b'{foo}'" },
                { "rB'{foo}'", "b'{foo}'" },
                { "Rb'{foo}'", "b'{foo}'" },
                { "RB'{foo}'", "b'{foo}'" },
                //2
                { "rb'''{foo}'''", "b'{foo}'" },
                { "rB'''{foo}'''", "b'{foo}'" },
                { "Rb'''{foo}'''", "b'{foo}'" },
                { "RB'''{foo}'''", "b'{foo}'" },
                //3
                { "rb\"{foo}\"", "b'{foo}'" },
                { "rB\"{foo}\"", "b'{foo}'" },
                { "Rb\"{foo}\"", "b'{foo}'" },
                { "RB\"{foo}\"", "b'{foo}'" },
                //4
                { "rb\"\"\"{foo}\"\"\"", "b'{foo}'" },
                { "rB\"\"\"{foo}\"\"\"", "b'{foo}'" },
                { "Rb\"\"\"{foo}\"\"\"", "b'{foo}'" },
                { "RB\"\"\"{foo}\"\"\"", "b'{foo}'" },
                //5
                { "rb'foo'", "b'foo'" },
                { "rB'foo'", "b'foo'" },
                { "Rb'foo'", "b'foo'" },
                { "RB'foo'", "b'foo'" },
                //6
                { "rb'''foo'''", "b'foo'" },
                { "rB'''foo'''", "b'foo'" },
                { "Rb'''foo'''", "b'foo'" },
                { "RB'''foo'''", "b'foo'" },
                //7
                { "rb\"foo\"", "b'foo'" },
                { "rB\"foo\"", "b'foo'" },
                { "Rb\"foo\"", "b'foo'" },
                { "RB\"foo\"", "b'foo'" },
                //8
                { "rb\"\"\"foo\"\"\"", "b'foo'" },
                { "rB\"\"\"foo\"\"\"", "b'foo'" },
                { "Rb\"\"\"foo\"\"\"", "b'foo'" },
                { "RB\"\"\"foo\"\"\"", "b'foo'" },
                //9
                { "rb'foo{boo}'", "b'foo{boo}'" },
                { "rB'foo{boo}'", "b'foo{boo}'" },
                { "Rb'foo{boo}'", "b'foo{boo}'" },
                { "RB'foo{boo}'", "b'foo{boo}'" },
                //10
                { "rb'''foo{boo}'''", "b'foo{boo}'" },
                { "rB'''foo{boo}'''", "b'foo{boo}'" },
                { "Rb'''foo{boo}'''", "b'foo{boo}'" },
                { "RB'''foo{boo}'''", "b'foo{boo}'" },
                //11
                { "rb\"foo{boo}\"", "b'foo{boo}'" },
                { "rB\"foo{boo}\"", "b'foo{boo}'" },
                { "Rb\"foo{boo}\"", "b'foo{boo}'" },
                { "RB\"foo{boo}\"", "b'foo{boo}'" },
                //12
                { "rb\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                { "rB\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                { "Rb\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                { "RB\"\"\"foo{boo}\"\"\"", "b'foo{boo}'" },
                //13
                { "rb''", "b''" },
                { "rB''", "b''" },
                { "Rb''", "b''" },
                { "RB''", "b''" },
                //14
                { "rb''''''", "b''" },
                { "rB''''''", "b''" },
                { "Rb''''''", "b''" },
                { "RB''''''", "b''" },
                //15
                { "rb\"\"", "b''" },
                { "rB\"\"", "b''" },
                { "Rb\"\"", "b''" },
                { "RB\"\"", "b''" },
                //16
                { "rb\"\"\"\"\"\"", "b''" },
                { "rB\"\"\"\"\"\"", "b''" },
                { "Rb\"\"\"\"\"\"", "b''" },
                { "RB\"\"\"\"\"\"", "b''" },
        };

        String output;
        String s;

        for (int i = 0; i < tests.length; i++) {
            try {
                output = parseLegalDocStr(tests[i][0]).toString();
                s = output.substring(output.indexOf("s=") + 2, output.indexOf(", type="));
                assertEquals(tests[i][1], s);
            } catch (Throwable e) {
                throw new Exception("Error parsing: " + tests[i][0], e);
            }
        }
    }

    public void testFStringStartingR() throws Exception {
        //INPUT, EXPECTED
        String[][] tests = new String[][] {
                //1
                { "rf'{foo}'", "{foo}" },
                { "rF'{foo}'", "{foo}" },
                { "Rf'{foo}'", "{foo}" },
                { "RF'{foo}'", "{foo}" },
                //2
                { "rf'''{foo}'''", "{foo}" },
                { "rF'''{foo}'''", "{foo}" },
                { "Rf'''{foo}'''", "{foo}" },
                { "RF'''{foo}'''", "{foo}" },
                //3
                { "rf\"{foo}\"", "{foo}" },
                { "rF\"{foo}\"", "{foo}" },
                { "Rf\"{foo}\"", "{foo}" },
                { "RF\"{foo}\"", "{foo}" },
                //4
                { "rf\"\"\"{foo}\"\"\"", "{foo}" },
                { "rF\"\"\"{foo}\"\"\"", "{foo}" },
                { "Rf\"\"\"{foo}\"\"\"", "{foo}" },
                { "RF\"\"\"{foo}\"\"\"", "{foo}" },
                //5
                { "rf'foo'", "foo" },
                { "rF'foo'", "foo" },
                { "Rf'foo'", "foo" },
                { "RF'foo'", "foo" },
                //6
                { "rf'''foo'''", "foo" },
                { "rF'''foo'''", "foo" },
                { "Rf'''foo'''", "foo" },
                { "RF'''foo'''", "foo" },
                //7
                { "rf\"foo\"", "foo" },
                { "rF\"foo\"", "foo" },
                { "Rf\"foo\"", "foo" },
                { "RF\"foo\"", "foo" },
                //8
                { "rf\"\"\"foo\"\"\"", "foo" },
                { "rF\"\"\"foo\"\"\"", "foo" },
                { "Rf\"\"\"foo\"\"\"", "foo" },
                { "RF\"\"\"foo\"\"\"", "foo" },
                //9
                { "rf'foo{boo}'", "foo{boo}" },
                { "rF'foo{boo}'", "foo{boo}" },
                { "Rf'foo{boo}'", "foo{boo}" },
                { "RF'foo{boo}'", "foo{boo}" },
                //10
                { "rf'''foo{boo}'''", "foo{boo}" },
                { "rF'''foo{boo}'''", "foo{boo}" },
                { "Rf'''foo{boo}'''", "foo{boo}" },
                { "RF'''foo{boo}'''", "foo{boo}" },
                //11
                { "rf\"foo{boo}\"", "foo{boo}" },
                { "rF\"foo{boo}\"", "foo{boo}" },
                { "Rf\"foo{boo}\"", "foo{boo}" },
                { "RF\"foo{boo}\"", "foo{boo}" },
                //12
                { "rf\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "rF\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "Rf\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                { "RF\"\"\"foo{boo}\"\"\"", "foo{boo}" },
                //13
                { "rf''", "" },
                { "rF''", "" },
                { "Rf''", "" },
                { "RF''", "" },
                //14
                { "rf''''''", "" },
                { "rF''''''", "" },
                { "Rf''''''", "" },
                { "RF''''''", "" },
                //15
                { "rf\"\"", "" },
                { "rF\"\"", "" },
                { "Rf\"\"", "" },
                { "RF\"\"", "" },
                //16
                { "rf\"\"\"\"\"\"", "" },
                { "rF\"\"\"\"\"\"", "" },
                { "Rf\"\"\"\"\"\"", "" },
                { "RF\"\"\"\"\"\"", "" },
        };

        String output;
        String s;

        for (int i = 0; i < tests.length; i++) {
            try {
                output = parseLegalDocStr(tests[i][0]).toString();
                s = output.substring(output.indexOf("s=") + 2, output.indexOf(", type="));
                assertEquals(tests[i][1], s);
            } catch (Throwable e) {
                throw new Exception("Error parsing: " + tests[i][0], e);
            }
        }
    }

}
