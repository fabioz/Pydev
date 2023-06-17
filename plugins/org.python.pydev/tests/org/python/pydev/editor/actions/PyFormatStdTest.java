/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.formatter.PyFormatter;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.shared_core.string.StringUtils;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PyFormatStdTest extends TestCase {

    private FormatStd std;

    private static boolean DEBUG = false;

    public static void main(String[] args) {
        try {
            PyFormatStdTest n = new PyFormatStdTest();
            n.setUp();
            //            DEBUG = true;
            n.testWhitespacesInComment2();
            n.tearDown();

            junit.textui.TestRunner.run(PyFormatStdTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        std = new FormatStd();
        std.operatorsWithSpace = true;
    }

    public void testNoCloseList() {

        std.operatorsWithSpace = true;
        std.assignWithSpaceInsideParens = true;
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = "" +
                "constants = [\n" +
                "    (qt.Qt.Key_Escape, ''), \n" +
                "    (qt.Qt.Key_Tab, '\t'), \n" +
                "\n";

        String s1 = "" +
                "constants = [\n" +
                "    (qt.Qt.Key_Escape, ''),\n" +
                "    (qt.Qt.Key_Tab, '\t'),\n" +
                "\n";

        checkFormatResults(s, s1);
    }

    public void testFormatImports() {

        std.assignWithSpaceInsideParens = true;
        std.spaceAfterComma = true;

        String s = "" +
                "from a import (b, \n" +
                "    c)\n" +
                "\n";

        String s1 = "" +
                "from a import (b,\n" +
                "    c)\n" +
                "\n";

        checkFormatResults(s, s1);
    }

    public void testDontDisturbWildImport() {
        std.operatorsWithSpace = true;
        std.assignWithSpaceInsideParens = true;
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        checkFormatResults("from x import *\n\n");
    }

    public void testDontDisturbWildImport2() {
        std.operatorsWithSpace = true;
        std.assignWithSpaceInsideParens = true;
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        checkFormatResults("import *");
    }

    public void testSpacesOnCall() {
        std.operatorsWithSpace = true;
        std.assignWithSpaceInsideParens = true;
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String original = "" +
                "raise RuntimeError( \n" +
                "    'text'\n" +
                "    % format )\n" +
                "";

        String expected = "" +
                "raise RuntimeError(\n" +
                "    'text'\n" +
                "    % format)\n" +
                "";
        checkFormatResults(original, expected);
    }

    public void testDontDisturbVarArgsAndKwArgs() {
        std.operatorsWithSpace = true;
        std.assignWithSpaceInsideParens = true;
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = "" +
                "def a(  a,b, *args, **kwargs  ):\n" +
                "    call( *args, **kwargs)\n";

        String s1 = "" +
                "def a(a, b, *args, **kwargs):\n" +
                "    call(*args, **kwargs)\n";

        checkFormatResults(s, s1);
    }

    public void testFormatComma() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = "" +
                "def a(  a,b  ):\n" +
                "    pass   \n";

        String s1 = "" +
                "def a(a, b):\n" +
                "    pass   \n";

        checkFormatResults(s, s1);

        std.spaceAfterComma = false;

        String s2 = "" +
                "def a(a,b):\n" +
                "    pass   \n";

        checkFormatResults(s, s2);

    }

    public void testNoFormatCommaOnNewLine() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = "" +
                "def a(a,\n" +
                "      b):\n" +
                "    pass\n";

        String s1 = "" +
                "def a(a,\n" +
                "      b):\n" +
                "    pass\n";

        checkFormatResults(s, s1);
    }

    public void testFormatEscapedQuotes() {
        std.spaceAfterComma = false;
        std.parametersWithSpace = false;

        String s = "" +
                "foo(bar(\"\\\"\"))";

        checkFormatResults(s);

        s = "" +
                "foo(bar('''\\''''))";
        checkFormatResults(s);
    }

    public void testFormatPar() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "" +
                "def a():\n" +
                "    pass   \n";

        String s1 = "" +
                "def a():\n" +
                "    pass   \n";

        checkFormatResults(s, s1);
    }

    public void testFormatComma2() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = "" +
                "def a( a,   b):\n" +
                "    pass   \n";

        String s1 = "" +
                "def a(a, b):\n" +
                "    pass   \n";

        checkFormatResults(s, s1);

        std.spaceAfterComma = false;

        String s2 = "" +
                "def a(a,b):\n" +
                "    pass   \n";

        checkFormatResults(s, s2);
    }

    public void testFormatCommaParams() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "" +
                "def a(a,   b):\n" +
                "    pass   \n";

        String s1 = "" +
                "def a( a, b ):\n" +
                "    pass   \n";

        checkFormatResults(s, s1);

        std.spaceAfterComma = false;

        String s2 = "" +
                "def a( a,b ):\n" +
                "    pass   \n";

        checkFormatResults(s, s2);
    }

    public void testFormatUnaryParams() {
        std.spaceAfterComma = true;
        std.operatorsWithSpace = true;
        std.assignWithSpaceInsideParens = false;

        String s = "" +
                "def a(a,  b = - 1 ):\n" +
                "    pass   \n";

        String s1 = "" +
                "def a(a, b=-1):\n" +
                "    pass   \n";

        checkFormatResults(s, s1);

        std.assignWithSpaceInsideParens = true;
        String s2 = "" +
                "def a(a, b = -1):\n" +
                "    pass   \n";

        checkFormatResults(s, s2);
    }

    public void testFormatMatMul() {
        std.spaceAfterComma = true;
        std.operatorsWithSpace = true;
        std.assignWithSpaceInsideParens = false;

        String s = "" +
                "a@2\n" +
                "a@=2\n" +
                "@method" +
                "";

        String s1 = "" +
                "a @ 2\n" +
                "a @= 2\n" +
                "@method" +
                "";

        checkFormatResults(s, s1);
    }

    public void testFormatMatMul2() {
        std.spaceAfterComma = true;
        std.operatorsWithSpace = true;
        std.assignWithSpaceInsideParens = false;

        String s = "" +
                "(a\n"
                + "@\n"
                + "2)\n" +
                "";

        checkFormatResults(s, s);
    }

    public void testFormatInnerParams() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "def a(a,   b):\n" +
                "    return ( (a+b) + ( a+b ) )   \n";

        String s1 = "" +
                "def a(a, b):\n" +
                "    return ((a + b) + (a + b))   \n";

        checkFormatResults(s, s1);
        checkFormatResults(s.replace('+', '*'), s1.replace('+', '*'));

        std.parametersWithSpace = true;
        String s2 = "" +
                "def a( a, b ):\n" +
                "    return ( ( a + b ) + ( a + b ) )   \n";

        checkFormatResults(s, s2);
        checkFormatResults(s.replace('+', '*'), s2.replace('+', '*'));
    }

    public void testFormatInnerParams2() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;
        std.operatorsWithSpace = false;

        String s = "" +
                "def a(a,   b):\n" +
                "    return ( callA() + callB(b+b) )   \n";

        String s1 = "" +
                "def a( a, b ):\n" +
                "    return ( callA()+callB( b+b ) )   \n";

        checkFormatResults(s, s1);
        checkFormatResults(s.replace('+', '*'), s1.replace('+', '*'));
    }

    public void testFormatNotInsideStrings() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "" +
                "a = ''' test()\n" +
                "nothing changes() ((aa) )\n" +
                "'''";

        checkFormatResults(s);

        s = "" +
                "a = ''' test()\n" +
                "nothing changes() ((aa) )\n" +
                "";

        checkFormatResults(s);

        s = "" +
                "a = ' test()'\n" +
                "'nothing changes() ((aa) )'\n" +
                "";

        checkFormatResults(s);

        s = "" +
                "a = ' test()'\n" +
                "'nothing changes() ((aa) )\n" +
                "";

        checkFormatResults(s);
    }

    public void testFormatNotInsideComments() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "" +
                "#a = ''' test()\n" +
                "#nothing changes() ((aa) )\n" +
                "#'''";

        checkFormatResults(s);
    }

    public void testFormatNotInsideComments5() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;
        std.operatorsWithSpace = true;

        String s = "" +
                "''' test()\n" +
                "nothing 'changes() ((aa) )\n" +
                "'''\n" +
                "thisChanges(a+b + (a+b))";

        String s2 = "" +
                "''' test()\n" +
                "nothing 'changes() ((aa) )\n" +
                "'''\n" +
                "thisChanges( a + b + ( a + b ) )";

        checkFormatResults(s, s2);
        checkFormatResults(s.replace('+', '*'), s2.replace('+', '*'));

        //unfinished comment
        s = "" +
                "''' test()\n" +
                "nothing 'changes() ((aa) )\n" +
                "''\n" +
                "thisDoesNotChange()";

        checkFormatResults(s);

        //unfinished comment at end of string
        s = "" +
                "''' test()\n" +
                "nothing 'changes() ((aa) )\n" +
                "''";

        checkFormatResults(s);
    }

    public void testFormatNotInsideComments2() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "methodname.split( '(' )";

        checkFormatResults(s);
    }

    public void testFormatNotInsideComments3() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "methodname.split( #'(' \n" +
                " )";

        checkFormatResults(s);
    }

    public void testFormatNotInsideStrings2() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "r = re.compile( \"(?P<latitude>\\d*\\.\\d*)\" )";

        checkFormatResults(s);
    }

    public void testFormatNotLinesOnlyWithParentesis() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;

        String s = "" +
                "methodCall( a,\n" +
                "            b \n" +
                "           ) ";

        checkFormatResults(s);
    }

    public void testCommaOnParens() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;

        String s = "" +
                "methodCall(a,b,c))\n";

        checkFormatResults(s, "methodCall(a, b, c))\n");
    }

    public void testOperators() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "i=i+1\n" +
                "submitted +=1\n" +
                "x = x*2 - 1\n" +
                "hypot2 = x*x + y*y\n"
                +
                "c = (a+b) * (a-b)\n" +
                "";

        String s1 = "" +
                "i = i + 1\n" +
                "submitted += 1\n" +
                "x = x * 2 - 1\n" +
                "hypot2 = x * x + y * y\n"
                +
                "c = (a + b) * (a - b)\n" +
                "";

        checkFormatResults(s, s1);
        checkFormatResults(s.replace('+', '*'), s1.replace('+', '*'));

    }

    public void testEqualsWithSpaceInFunctionCall() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.assignWithSpaceInsideParens = true;
        std.operatorsWithSpace = true;

        String s = "" +
                "a(xxx=10)\n" +
                "call(yyy = 20)\n";

        String s1 = "" +
                "a(xxx = 10)\n" +
                "call(yyy = 20)\n";

        checkFormatResults(s, s1);

        s1 = "" +
                "a(xxx = 10)\n" +
                "call(yyy = 20)\n";
        checkFormatResults(StringUtils.replaceAll(s, "=", "!="),
                StringUtils.replaceAll(s1, "=", "!="));
        checkFormatResults(StringUtils.replaceAll(s, "=", "<="),
                StringUtils.replaceAll(s1, "=", "<="));

        std.assignWithSpaceInsideParens = false;
        s1 = "" +
                "a(xxx=10)\n" +
                "call(yyy=20)\n";

        checkFormatResults(s, s1);

        s1 = "" +
                "a(xxx = 10)\n" +
                "call(yyy = 20)\n";
        checkFormatResults(StringUtils.replaceAll(s, "=", "!="),
                StringUtils.replaceAll(s1, "=", "!="));
        checkFormatResults(StringUtils.replaceAll(s, "=", "<="),
                StringUtils.replaceAll(s1, "=", "<="));
    }

    public void testNotValidCode() {
        //should not crash in these tests
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = false;

        checkFormatResults("=", "=");
        checkFormatResults("==", "==");
        checkFormatResults("!", "!");
        checkFormatResults("!=", "!=");
    }

    public void testCompare() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "a==10\n" +
                "b== 20\n" +
                "c    =  30\n" +
                "d ==+1\n" +
                "e !=+1\n" +
                "e //=+1\n" +
                "";

        String s1 = "" +
                "a == 10\n" +
                "b == 20\n" +
                "c = 30\n" +
                "d == +1\n" +
                "e != +1\n" +
                "e //= +1\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testSimpleOperator() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "86000+10\n" +
                "";

        String s1 = "" +
                "86000 + 10\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testSimpleOperator2() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "+1\n" + //don't change if it's not summing anything (it's a sign for the number: unary operator)
                "";

        String s1 = "" +
                "+1\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testSimpleOperator3() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "call(+1)\n" + //don't change if it's not summing anything (it's a sign for the number: unary operator)
                "";

        String s1 = "" +
                "call(+1)\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testSimpleOperator4() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "call(1+1+2+(-1+(-1+1)))\n" + //don't change if it's not summing anything (it's a sign for the number: unary operator)
                "";

        String s1 = "" +
                "call(1 + 1 + 2 + (-1 + (-1 + 1)))\n" +
                "";

        checkFormatResults(s, s1);
        checkFormatResults(s.replace('+', '*'), s1.replace('+', '*'));
    }

    public void testSimpleOperator5() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "expected_id = int(key.split('(')[ - 1][: - 1])\n" + //don't change if it's not summing anything (it's a sign for the number: unary operator)
                "";

        String s1 = "" +
                "expected_id = int(key.split('(')[ -1][:-1])\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testSimpleOperator6() {
        std.operatorsWithSpace = true;

        String s = "" +
                "if a>-10:print a\n" + //don't change if it's not summing anything (it's a sign for the number: unary operator)
                "";

        String s1 = "" +
                "if a > -10:print a\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testSimpleOperator7() {
        std.operatorsWithSpace = true;

        String s = "" +
                "if a>--10:print a\n" + //don't change if it's not summing anything (it's a sign for the number: unary operator)
                "";

        String s1 = "" +
                "if a > - -10:print a\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testSimpleOperator8() {
        std.operatorsWithSpace = true;

        String s = "" +
                "if a-10:print a *-10\n" + //don't change if it's not summing anything (it's a sign for the number: unary operator)
                "";

        String s1 = "" +
                "if a - 10:print a * -10\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testCorrectExponentials() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "a = 1e-6\n" + //operators should not have space
                "b = 1e+6\n" +
                "c = 1e3 + 6\n" +
                "d = 1e-3 - 6\n" +
                "e = 1+3 - 6\n" +
                "";

        String s1 = "" +
                "a = 1e-6\n" + //operators should not have space
                "b = 1e+6\n" +
                "c = 1e3 + 6\n" +
                "d = 1e-3 - 6\n" +
                "e = 1 + 3 - 6\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testUnaryOpWithSpace() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s, s1;

        s = "" +
                "def test_formatter_unary():\n" +
                "   c = +1\n" +
                "   d = - 1\n" +
                "   e = (a, +2)\n"
                +
                "   e = (a, + 2)\n" +
                "   e = 1e - 3\n" +
                "   e = 1E - 3\n" +
                "   pass\n" +
                "";

        s1 = "" +
                "def test_formatter_unary():\n" +
                "   c = +1\n" +
                "   d = -1\n" +
                "   e = (a, +2)\n"
                +
                "   e = (a, +2)\n" +
                "   e = 1e-3\n" +
                "   e = 1E-3\n" +
                "   pass\n" +
                "";
        checkFormatResults(s, s1);

        s1 = "" +
                "def test_formatter_unary():\n" +
                "   e = 1e-3\n" +
                "   e = 1E-3\n" +
                "";

        s = "" +
                "def test_formatter_unary():\n" +
                "   e = 1e - 3\n" +
                "   e = 1E - 3\n" +
                "";

        checkFormatResults(s, s1);

    }

    public void testTrimAndNewLineEOL() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;
        std.addNewLineAtEndOfFile = true;
        std.trimLines = true;

        String s = "" +
                "a=10  \n" +
                "b= 20  \n" +
                "c    =  30  ";

        String s1 = "" +
                "a = 10\n" +
                "b = 20\n" +
                "c = 30\n";

        checkFormatResults(s, s1);
    }

    public void testTrimAndNewLineEOL2() {
        std.trimMultilineLiterals = false;
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;
        std.addNewLineAtEndOfFile = true;
        std.trimLines = true;

        String s = "" +
                "#a=10  \n" +
                "#b= 20  \n" +
                "'''c    =  30 \n" +
                "   \n" +
                "''' \n" +
                "c =   30   ";

        String s1 = "" +
                "#a=10\n" +
                "#b= 20\n" +
                "'''c    =  30 \n" +
                "   \n" +
                "'''\n" +
                "c = 30\n";

        checkFormatResults(s, s1);
    }

    public void testFormatSelection() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;
        std.addNewLineAtEndOfFile = true;
        std.trimLines = true;

        final PyFormatAction pyFormatStd = new PyFormatAction();
        String s = "" +
                "a  =  10  \n" +
                "a  =  10  " +
                "";
        String expected = "" +
                "a = 10\n" +
                "a  =  10  \n" +
                "";
        Document doc = new Document(s);

        int[] regionsForSave = new int[] { 0 };
        PyFormatter.formatSelection(doc, regionsForSave, null, new PySelection(doc), std);
        assertEquals(expected, doc.get());
    }

    public void testFormatSelection2() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;
        std.addNewLineAtEndOfFile = true;
        std.trimLines = true;

        final PyFormatAction pyFormatStd = new PyFormatAction();
        String s = "" +
                "a,b,c\n" +
                "a  =  10  " +
                "";
        String expected = "" +
                "a, b, c\n" +
                "a  =  10  \n" +
                "";
        Document doc = new Document(s);

        int[] regionsForSave = new int[] { 0 };
        PyFormatter.formatSelection(doc, regionsForSave, null, new PySelection(doc), std);
        assertEquals(expected, doc.get());
    }

    public void testFormatSelectionNotOnStrings() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = true;
        std.operatorsWithSpace = true;
        std.addNewLineAtEndOfFile = true;
        std.trimLines = true;

        final PyFormatAction pyFormatStd = new PyFormatAction();
        final String s = "" +
                "'a,b,c'\n" +
                "a  =  10\n" +
                "";
        Document doc = new Document(s);

        int[] regionsForSave = new int[] { 0 };
        PyFormatter.formatSelection(doc, regionsForSave, null, new PySelection(doc), std);
        assertEquals(s, doc.get());
    }

    public void testTrimAndNewLineEOL3() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;
        std.addNewLineAtEndOfFile = true;
        std.trimLines = true;

        checkFormatResults("c =  30", "c = 30\n");
        checkFormatResults("c =  30\n", "c = 30\n");
        checkFormatResults("c =  30 ", "c = 30\n");
        checkFormatResults("c =  30  ", "c = 30\n");
        checkFormatResults("c =  30\n ", "c = 30\n");
        checkFormatResults("c =  30\n\n ", "c = 30\n\n");
        checkFormatResults("c =  30\n\n \t ", "c = 30\n\n");
        checkFormatResults("c =  30 \t\n\n \t ", "c = 30\n\n");

        checkFormatResults("c = 30\n", "c = 30\n");
        checkFormatResults("", "");
        checkFormatResults("  \t  ", "");
    }

    public void testTrimAndNewLineEOL3a() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;
        std.addNewLineAtEndOfFile = true;
        std.trimLines = true;

        checkFormatResults("c = 30", "c = 30\n");
    }

    public void testEqualsWithSpace() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "a=10\n" +
                "b= 20\n" +
                "c    =  30";

        String s1 = "" +
                "a = 10\n" +
                "b = 20\n" +
                "c = 30";

        checkFormatResults(s, s1);
        checkFormatResults(StringUtils.replaceAll(s, "=", "!="),
                StringUtils.replaceAll(s1, "=", "!="));
        checkFormatResults(StringUtils.replaceAll(s, "=", "<="),
                StringUtils.replaceAll(s1, "=", "<="));

        s = "" +
                "a=\\n10\n" +
                "b= 20\n" +
                "c    =  30";

        s1 = "" +
                "a = \\n10\n" +
                "b = 20\n" +
                "c = 30";

        checkFormatResults(s, s1);
        checkFormatResults(StringUtils.replaceAll(s, "=", "!="),
                StringUtils.replaceAll(s1, "=", "!="));
        checkFormatResults(StringUtils.replaceAll(s, "=", "<="),
                StringUtils.replaceAll(s1, "=", "<="));

        s = "" +
                "a=10\n" +
                "b= 20\n" +
                "c    =  30";

        s1 = "" +
                "a=10\n" +
                "b=20\n" +
                "c=30";

        std.operatorsWithSpace = false;
        checkFormatResults(s, s1);
        checkFormatResults(StringUtils.replaceAll(s, "=", ">="),
                StringUtils.replaceAll(s1, "=", ">="));
        checkFormatResults(StringUtils.replaceAll(s, "=", "+="),
                StringUtils.replaceAll(s1, "=", "+="));

        s = "" +
                "a=\\n" +
                "10\n" +
                "b= 20\n" +
                "c    =  30";

        s1 = "" +
                "a=\\n" +
                "10\n" +
                "b=20\n" +
                "c=30";

        checkFormatResults(s, s1);
    }

    /**
     * Checks the results with the default passed and then with '\r' and '\n' considering
     * that the result of formatting the input string will be the same as the input.
     *
     * @param s the string to be checked (and also the expected output)
     */
    private void checkFormatResults(String s) {
        checkFormatResults(s, s);
    }

    /**
     * Checks the results with the default passed and then with '\r' and '\n'
     * @param s the string to be checked
     * @param expected the result of making the formatting in the string
     */
    private void checkFormatResults(String s, String expected) {
        //default check (defined with \n)
        try {
            Document doc = new Document(s);
            PyFormatter.formatAll(null, doc, null, true, std, false, true);
            String formatStr = doc.get();

            if (DEBUG) {
                System.out.println(">>" + s.replace(' ', '.') +
                        "<<");
                System.out.println(">>" + formatStr.replace(' ', '.') +
                        "<<");
            }
            if (!s.contains("\n")) {
                expected = StringUtils.replaceAll(expected, "\n",
                        PySelection.getDelimiter(new Document()));
            }
            assertEquals(expected, formatStr);
            if (!s.contains("\n")) {
                return;
            }

            //second check (defined with \r)
            String s2 = s.replace('\n', '\r');
            String expected2 = expected.replace('\n', '\r');

            doc = new Document(s);
            PyFormatter.formatAll(null, doc, null, true, std, false, true);
            formatStr = doc.get();
            assertEquals(expected, formatStr);

            //third check (defined with \r\n)
            s = StringUtils.replaceAll(s, "\r", "\r\n");
            expected = StringUtils.replaceAll(expected, "\r", "\r\n");

            doc = new Document(s);
            PyFormatter.formatAll(null, doc, null, true, std, false, true);
            formatStr = doc.get();
            assertEquals(expected, formatStr);

            formatStr = PyFormatter.formatStrAutopep8OrPyDev(null, null, new Document(s2), std, "\r", false, true,
                    null);
            if (expected2.endsWith("\r") && !formatStr.endsWith("\r")) {
                expected2 = expected2.substring(0, expected2.length() - 1);
            }
            assertEquals(expected2, formatStr);

            //third check (defined with \r\n)
            String s3 = StringUtils.replaceAll(s, "\n", "\r\n");
            String expected3 = StringUtils.replaceAll(expected, "\n", "\r\n");

            formatStr = PyFormatter.formatStrAutopep8OrPyDev(null, null, new Document(s3), std, "\r\n", false, true,
                    null);
            if (expected3.endsWith("\r\n") && !formatStr.endsWith("\r\n")) {
                expected3 = expected3.substring(0, expected3.length() - 2);
            }
            assertEquals(expected3, formatStr);

            //now, same thing with different API
            doc = new Document();
            doc.set(s);
            PyFormatter.formatAll(null, doc, null, true, std, false, true);
            assertEquals(expected, doc.get());
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);
        }
    }

    public void testDontLooseComma() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "call(aa, (1, 2)\n" +
                "";

        String s1 = "" +
                "call(aa, (1, 2)\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testKeepWhitespaces() {
        std.spaceAfterComma = true;
        std.parametersWithSpace = false;
        std.operatorsWithSpace = true;

        String s = "" +
                "t1 = ( 1, )\n" +
                "t2 = ( -1, )\n" +
                "l1 = [ 1 ]\n" +
                "l2 = [ -1 ]\n" +
                "d1 = { 1:0 }\n"
                +
                "d2 = { -1:0 }\n" +
                "";

        String s1 = "" +
                "t1 = (1,)\n" +
                "t2 = (-1,)\n" +
                "l1 = [ 1 ]\n" +
                "l2 = [ -1 ]\n" +
                "d1 = { 1:0 }\n"
                +
                "d2 = { -1:0 }\n" +
                "";

        checkFormatResults(s, s1);
    }

    public void testKeepTab() throws Exception {
        String s = "" +
                "tmp = (\n" +
                "\t)";
        checkFormatResults(s, s);
    }

    public void testNumber() throws Exception {
        String s = "" +
                "return -1";
        checkFormatResults(s, s);
    }

    public void testNumber2() throws Exception {
        String s = "" +
                "call(a + 1e+10)";
        checkFormatResults(s, s);
    }

    public void testNumber3() throws Exception {
        String s = "" +
                "call(0.0,1.E+13,1.0,0.0)";
        checkFormatResults(s, s);
    }

    public void testNumber4() throws Exception {
        String s = "" +
                "1 + 2 + +3 - -3";
        checkFormatResults(s, s);
    }

    public void testNumber5() throws Exception {
        String s = "" +
                "def a():\n" +
                "    return -1";
        checkFormatResults(s, s);
    }

    public void testNumber6() throws Exception {
        String s = "" +
                "def a(a=10 + +20):\n" +
                "    return -1";
        checkFormatResults(s, s);
    }

    public void testNumber7() throws Exception {
        String s = "" +
                "def a(a=10 + +20):\n" +
                "    return a + -1 - (-3)";
        checkFormatResults(s, s);
        checkFormatResults(s.replace('+', '*'), s.replace('+', '*'));
    }

    public void testNumber8() throws Exception {
        String s = "" +
                " -1 - (-3)";
        checkFormatResults(s, s);
    }

    public void testOperator() throws Exception {
        String s = "" +
                "a=('#'*3)";
        std.operatorsWithSpace = true;
        checkFormatResults(s, "a = ('#' * 3)");
    }

    public void testOperator2() throws Exception {
        String s = "" +
                "a=(('#')*3)";
        std.operatorsWithSpace = true;
        checkFormatResults(s, "a = (('#') * 3)");
    }

    public void testOperator3() throws Exception {
        String s = "" +
                "in_index = is_reversed and -3 or -2";
        std.operatorsWithSpace = true;
        checkFormatResults(s, "in_index = is_reversed and -3 or -2");
    }

    public void testOperator4() throws Exception {
        String s = "" +
                "in_index=is_reversed and -3 or -2";
        std.operatorsWithSpace = false;
        checkFormatResults(s, "in_index=is_reversed and-3 or-2");
    }

    public void testOperator5() throws Exception {
        String s = "" +
                "a[:2] += 4";
        std.operatorsWithSpace = true;
        checkFormatResults(s, "a[:2] += 4");
    }

    public void testOperator6() throws Exception {
        String s = "" +
                "a[:2] *= 4";
        std.operatorsWithSpace = true;
        checkFormatResults(s, "a[:2] *= 4");
    }

    public void testOperator7() throws Exception {
        String s = "" +
                "[2]*4";
        std.operatorsWithSpace = true;
        checkFormatResults(s, "[2] * 4");
    }

    public void testLambdaWithArgs() throws Exception {
        String s = "" +
                "a = lambda *args:10";
        std.operatorsWithSpace = true;
        checkFormatResults(s, s);
    }

    public void testLambdaWithArgs2() throws Exception {
        String s = "" +
                "a = lambda a,*args:10";
        std.operatorsWithSpace = true;
        checkFormatResults(s, s);
    }

    public void testTrimComments() throws Exception {
        String s = "#comment ";
        std.trimLines = true;
        checkFormatResults(s, "#comment");
    }

    public void testTrimComments2() throws Exception {
        String s = "a = [a, #comment \nb]";
        String expected = "a = [a, #comment\nb]";
        std.trimLines = true;
        checkFormatResults(s, expected);
    }

    public void testLambdaWithArgs3() throws Exception {
        String s = "" +
                "foo = lambda *args,**kwargs: True";
        std.operatorsWithSpace = true;
        checkFormatResults(s, s);
    }

    public void testFormatError() throws Exception {
        String s = "" +
                "class Bar:\n" +
                "    def Foo:\n" +
                "        ra( )\n" +
                "        a.Get(self, ra( )\n" +
                "";
        final PyFormatAction pyFormatStd = new PyFormatAction();
        try {
            PyFormatter.formatAll(null, new Document(s), null, false, std, true, true);
            fail("Expecting exception!");
        } catch (Exception e) {
        }
        String expected = "" +
                "class Bar:\n" +
                "\n" +
                "    def Foo:\n" +
                "        ra()\n" +
                "        a.Get(self, ra()\n" +
                "";

        std.manageBlankLines = true;
        checkFormatResults(s, expected);
    }

    public void testTrimMultilineLiterals() throws Exception {
        String input = "" +
                "class Foo:\n" +
                "    '''Class docstring   '''\n" +
                "    def __init__(self):\n" +
                "        '''   \n" +
                "        Method docstring   \n" +
                "        \n" +
                "        with multiple lines   \n" +
                "        '''\n" +
                "        print 'Some information   '\n" +
                "        print '''More\n" +
                "              information   \n" +
                "              '''\n";
        String expected = "" +
                "class Foo:\n" +
                "    '''Class docstring   '''\n" +
                "\n" +
                "    def __init__(self):\n" +
                "        '''\n" +
                "        Method docstring\n" +
                "\n" +
                "        with multiple lines\n" +
                "        '''\n" +
                "        print 'Some information   '\n" +
                "        print '''More\n" +
                "              information\n" +
                "              '''\n";

        std.trimMultilineLiterals = true;
        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testWhitespacesBeforeCommentsUnchanged() throws Exception {
        std.spacesBeforeComment = FormatStd.DONT_HANDLE_SPACES;
        String input = "a = 10#comment";
        String expected = "a = 10#comment";
        checkFormatResults(input, expected);
    }

    public void testWhitespacesBeforeCommentsUnchanged2() throws Exception {
        std.spacesBeforeComment = FormatStd.DONT_HANDLE_SPACES;
        String input = "a = 10,  #comment";
        String expected = "a = 10,  #comment";
        checkFormatResults(input, expected);
    }

    public void testWhitespacesBeforeCommentsUnchanged3() throws Exception {
        std.spacesBeforeComment = 0;
        String input = "a = 10\n    #comment";
        checkFormatResults(input, input);
    }

    public void testWhitespacesBeforeCommentsUnchanged4() throws Exception {
        std.spacesBeforeComment = 0;
        String input = "    #comment";
        checkFormatResults(input, input);
    }

    public void testWhitespacesBeforeCommentsChanged() throws Exception {
        std.spacesBeforeComment = 2;
        String input = "a = 10#comment";
        String expected = "a = 10  #comment";
        checkFormatResults(input, expected);
    }

    public void testWhitespacesBeforeCommentsChanged2() throws Exception {
        std.spacesBeforeComment = 2;
        String input = "a = 10, #comment";
        String expected = "a = 10,  #comment";
        checkFormatResults(input, expected);
    }

    public void testWhitespacesInComment() throws Exception {
        std.spacesInStartComment = 1;
        String input = "a = 10, #comment";
        String expected = "a = 10, # comment";
        checkFormatResults(input, expected);
    }

    public void testWhitespacesInComment2() throws Exception {
        std.spacesInStartComment = 1;
        String input = "#comment\r\n#\r\n# comment";
        String expected = "# comment\r\n#\r\n# comment";
        checkFormatResults(input, expected);
    }

    public void testWhitespacesInComment3() throws Exception {
        std.spacesInStartComment = 1;
        String input = "#!/usr/bin/python";
        checkFormatResults(input, input);
    }

    public void testWhitespacesInComment4() throws Exception {
        std.spacesInStartComment = 1;
        String input = "#----\n#a\n#----";
        String expected = "#----\n# a\n#----";
        checkFormatResults(input, expected);
    }

    public void testUnaryOnTab() throws Exception {
        String input = ""
                + "spam = (\n"
                + "\t-1\n"
                + ")\n";
        String expected = input;
        checkFormatResults(input, expected);
    }

    public void testUnaryOnSpaces() throws Exception {
        String input = ""
                + "spam = (\n"
                + "    -1\n"
                + ")\n";
        String expected = input;
        checkFormatResults(input, expected);
    }

    public void testSelfAndPlus() throws Exception {
        String input = ""
                + "y = self + x\n";
        String expected = input;
        checkFormatResults(input, expected);
    }

    public void testKeepAtMostOneEmptyLine() throws Exception {
        String input = ""
                + "def my(a):\n"
                + "    a = 10\n"
                + "\n"
                + "\n"
                + "\n"
                + "    b = 20\n"
                + "";

        String expected = ""
                + "def my(a):\n"
                + "    a = 10\n"
                + "\n"
                + "    b = 20\n"
                + "";
        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testSpacesBeforeClass() throws Exception {
        String input = ""
                + "class my:\n"
                + "    a = 10\n"
                + "class my:\n"
                + "    a = 10\n"
                + "";

        String expected = ""
                + "class my:\n"
                + "    a = 10\n"
                + "\n"
                + "\n"
                + "class my:\n"
                + "    a = 10\n"
                + "";
        std.manageBlankLines = true;
        std.addNewLineAtEndOfFile = false;
        checkFormatResults(input, expected);
    }

    public void testSpacesBeforeClass2() throws Exception {
        // Already is properly formatted.
        String input = ""
                + "a = 10\n"
                + "\n"
                + "\n"
                + "class my:\n"
                + "    a = 10\n"
                + "\n"
                + "    def foo():\n"
                + "        \n"
                + "";

        std.manageBlankLines = true;
        checkFormatResults(input, input);
    }

    public void testSpacesBeforeClass3() throws Exception {
        // Already is properly formatted.
        String input = ""
                + "a = 10\n"
                + "  \n"
                + "  \n"
                + "class my:\n"
                + "    a = 10\n"
                + "    \n"
                + "    def foo():\n"
                + "        \n"
                + "";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testSpacesBeforeAfterClass() throws Exception {
        // Already is properly formatted.
        String input = ""
                + "from a import b\n"
                + "\n"
                + "\n"
                + "\n"
                + "class my:\n" // line 4
                + "    a = 10\n"
                + "    \n"
                + "    \n"
                + "    def foo():\n" // line 8
                + "        '''docstring\n"
                + "        docstring'''\n"
                + "\n"
                + "\n"
                + "    def bar():\n" // line 13
                + "        '''docstring\n" // line 14
                + "        docstring'''\n"
                + "\n"
                + "\n"
                + "\n"
                + "class my:\n" // line 19
                + "    a = 10\n"
                + "    \n"
                + "    \n"
                + "    def foo():\n"
                + "        '''docstring\n"
                + "        docstring'''\n"
                + "\n"
                + "\n"
                + "    def bar():\n"
                + "        '''docstring\n"
                + "        docstring'''\n"
                + "\n"
                + "";

        std.manageBlankLines = true;
        std.trimLines = false;
        std.blankLinesInner = 2;
        std.blankLinesTopLevel = 3;
        checkFormatResults(input, input);
    }

    public void testSpacesBeforeClassIgnoreLiterals() throws Exception {
        // Already is properly formatted.
        String input = ""
                + "a = 10\n"
                + "  \n"
                + "        '''\n"
                + " tttttt\n"
                + "\n"
                + "\n"
                + "\n"
                + "        '''\n"
                + "  \n"
                + "  \n"
                + "class my:\n" // line 10
                + "    a = 10\n"
                + "    \n"
                + "    def foo():\n"
                + "";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testSpacesBeforeClassIgnoreLiterals2() throws Exception {
        // Already is properly formatted.
        String input = ""
                + "a = 10\n"
                + "  \n"
                + "        '''\n"
                + "        '''\n"
                + "  \n"
                + "  \n"
                + "class my:\n"
                + "    a = 10\n"
                + "    \n"
                + "    def foo():\n"
                + "";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testBlankLinesWithMultipleDecorators() throws Exception {
        String input = "" +
                "\n" +
                "\n" +
                "def m1():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "@foo\n" +
                "@bar\n" +
                "def m2():\n" +
                "    pass";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testIgnoreDefs() throws Exception {
        String input = "" +
                "\n" +
                "\n" +
                "def m1():\n" +
                "    a = def class\n" +
                "    pass";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testBlankLinesBeforeClassIgnoreLiterals3() throws Exception {
        // Already is properly formatted.
        String input = ""
                + "a = 10\n"
                + "  \n"
                + "        ''' check '''\n"
                + "  \n"
                + "  \n"
                + "class my:\n"
                + "    a = 10\n"
                + "    \n"
                + "    def foo():\n"
                + "";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testBlankLinesWithLogicalLines() throws Exception {
        // Already is properly formatted.
        String input = "" +
                "def method():\n" +
                "\n" +
                "    def foo():\n" +
                "        assert bar(),\\\n" +
                "            'line1 '\\\n" +
                "            'line2.'\n" +
                "\n" +
                "\n" +
                "if xx == 'bar':\n" +
                "    try:\n" +
                "        from a import b\n" +
                "    except ImportError:\n" +
                "        import c\n" +
                ""
                + "";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testBlankLinesCommentsDontChangeLevel() throws Exception {
        // Already is properly formatted.
        String input = "" +
                "class Foo:\n" +
                "\n" +
                "    def bar(self):\n" +
                "        a = 10\n" +
                "#             comment\n" +
                "\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testBlankLinesAfterTop() throws Exception {
        String input = "" +
                "class A:\n" +
                "\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "\n" +
                "class B:\n" +
                "    pass\n" +
                "\n" +
                "";

        String expected = "" +
                "class A:\n" +
                "\n" +
                "\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "\n" +
                "class B:\n" +
                "    pass\n" +
                "\n" +
                "";

        std.manageBlankLines = true;
        std.trimLines = false;
        std.blankLinesTopLevel = 3;
        std.blankLinesInner = 2;
        checkFormatResults(input, expected);
    }

    public void testBlankLinesCommentsDontChangeLevel2() throws Exception {
        // Already is properly formatted.
        String input = "" +
                "class Foo:\n" +
                "\n" +
                "    def bar(self):\n" +
                "        a = 10\n" +
                "\n" +
                "    def foo(self):\n" +
                "        b = 10\n" +
                "\n" +
                "\n" +
                "# comment from class\n" +
                "class Bar:\n" +
                "    pass" +
                "";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testBlankLinesCommentsDontChangeLevel3() throws Exception {
        // Already is properly formatted.
        String input = "" +
                "class Foo:\n" +
                "    b = 10\n" +
                "\n" +
                "\n" +
                "# comment from class\n" +
                "class Bar:\n" +
                "    pass" +
                "";

        std.manageBlankLines = true;
        std.trimLines = false;
        checkFormatResults(input, input);
    }

    public void testSpacesBeforeClassWithComments() throws Exception {
        String input = ""
                + "#comment\n"
                + "class my:\n"
                + "    a = 10\n"
                + "#comment\n"
                + "class my:\n"
                + "    a = 10\n"
                + "";

        String expected = ""
                + "#comment\n"
                + "class my:\n"
                + "    a = 10\n"
                + "\n"
                + "\n"
                + "#comment\n"
                + "class my:\n"
                + "    a = 10\n"
                + "";
        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testSpacesBeforeClassWithCommentsAndDecorator() throws Exception {
        String input = ""
                + "#comment\n"
                + "@decorator\n"
                + "class my:\n"
                + "    a = 10\n"
                + "#comment\n"
                + "@decorator\n"
                + "class my:\n"
                + "    a = 10\n"
                + "";

        String expected = ""
                + "#comment\n"
                + "@decorator\n"
                + "class my:\n"
                + "    a = 10\n"
                + "\n"
                + "\n"
                + "#comment\n"
                + "@decorator\n"
                + "class my:\n"
                + "    a = 10\n"
                + "";
        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testEmptyLinesBeforeAfter() throws Exception {
        String input = ""
                + "def my(a):\n"
                + "    pass\n"
                + "def my2(b):\n"
                + "    pass\n"
                + "";

        String expected = ""
                + "def my(a):\n"
                + "    pass\n"
                + "\n"
                + "\n"
                + "def my2(b):\n"
                + "    pass\n"
                + "";
        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testEmptyLinesBeforeAfter4() throws Exception {
        String input = ""
                + "from a import b\n" +
                "\n" +
                "\n" +
                "\n" +
                "#comment\n" +
                "class Test(object):\n" + // line 5
                "\n" +
                "\n" +
                "    def m2(self):\n" +
                "        pass\n" +
                ""
                + "";

        std.manageBlankLines = true;
        std.blankLinesTopLevel = 3;
        std.blankLinesInner = 2;
        checkFormatResults(input, input);
    }

    public void testEmptyLinesBeforeAfter5() throws Exception {
        String input = "" +
                "class Test(object):\n" +
                "\n" +
                "    def m2(self):\n" +
                "        pass\n" +
                "    a = 10\n" +
                "";
        String expected = "" +
                "class Test(object):\n" +
                "\n" +
                "    def m2(self):\n" +
                "        pass\n" +
                "\n" +
                "    a = 10\n" +
                "";

        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testEmptyLinesBeforeAfter6() throws Exception {
        String input = "" +
                "class Test(object):\n" +
                "\n" +
                "    def m2(self):\n" +
                "        pass\n" +
                "    @foo\n" +
                "\n" + // Remove this space
                "    def m3(self):\n" +
                "        pass\n" +
                "    a = 10\n" +
                "";
        String expected = "" +
                "class Test(object):\n" +
                "\n" +
                "    def m2(self):\n" +
                "        pass\n" +
                "\n" +
                "    @foo\n" +
                "    def m3(self):\n" +
                "        pass\n" +
                "\n" +
                "    a = 10\n" +
                "";

        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testEmptyLinesBeforeAfter8() throws Exception {
        String input = "" +
                "class C:\n" +
                "\n" +
                "    @foo\n" +
                "    @bar\n" +
                " \n" +
                " \n" +
                "    def x(self):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "class B(QWidget):\n" +
                "\n" +
                "    @property\n" +
                "    def model(self):\n" +
                "        pass\n" +
                "\n"
                + "";
        String expected = "" +
                "class C:\n" +
                "\n" +
                "    @foo\n" +
                "    @bar\n" +
                "    def x(self):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "class B(QWidget):\n" +
                "\n" +
                "    @property\n" +
                "    def model(self):\n" +
                "        pass\n" +
                "\n";

        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testEmptyLinesBeforeAfter7() throws Exception {
        String expected = "" +
                "class Test(object):\n" +
                "\n" +
                "    def m2(self):\n" +
                "        pass\n" +
                "\n" +
                "    @foo\n" +
                "    def m3(self):\n" +
                "        pass\n" +
                "\n" +
                "    a = 10\n" +
                "";

        std.manageBlankLines = true;
        checkFormatResults(expected, expected);
    }

    public void testEmptyLinesBeforeAfter2() throws Exception {
        String input = ""
                + "def my(a):\n"
                + "    pass\n"
                + "a = 10\n"
                + "";

        String expected = ""
                + "def my(a):\n"
                + "    pass\n"
                + "\n"
                + "\n"
                + "a = 10\n"
                + "";
        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testEmptyLinesBeforeAfter3() throws Exception {
        String input = ""
                + "def my(a):\n"
                + "    rara = 10 + \\\n"
                + "ignore this\n"
                + "a = 10\n"
                + "";

        String expected = ""
                + "def my(a):\n"
                + "    rara = 10 + \\\n"
                + "ignore this\n"
                + "\n"
                + "\n"
                + "a = 10\n"
                + "";
        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testEmptyLinesBeforeAfterAsyncDef() throws Exception {
        String input = ""
                + "async def my(a):\n"
                + "    pass\n"
                + "async def my2(b):\n"
                + "    pass\n"
                + "";

        String expected = ""
                + "async def my(a):\n"
                + "    pass\n"
                + "\n"
                + "\n"
                + "async def my2(b):\n"
                + "    pass\n"
                + "";
        std.manageBlankLines = true;
        checkFormatResults(input, expected);
    }

    public void testFormatSelectionChangingLines() {
        std.manageBlankLines = true;
        std.trimLines = true;

        String s = "" +
                "a  =  10  \n" +
                "\n" +
                "\n" +
                "\n" +
                "a  =  10  \n" +
                "";
        String expected = "" +
                "a = 10\n" +
                "\n" +
                "a  =  10  \n" +
                "";
        Document doc = new Document(s);

        int[] regionsForSave = new int[] { 0, 1, 2, 3 };
        PyFormatter.formatSelection(doc, regionsForSave, null, new PySelection(doc), std);
        assertEquals(expected, doc.get());
    }

    public void testSpacesBeforeClassIgnoreLiteralsWithSelection() throws Exception {
        // Already is properly formatted.
        String input = ""
                + "a = 10\n"
                + "  \n"
                + "        ''' check '''\n"
                + "  \n"
                + "class my:\n"
                + "    a = 10\n"
                + "    \n"
                + "    def foo():\n"
                + "";

        String expected = ""
                + "a = 10\n"
                + "  \n"
                + "        ''' check '''\n"
                + "\n"
                + "  \n"
                + "class my:\n"
                + "    a = 10\n"
                + "    \n"
                + "    def foo():\n"
                + "";

        std.manageBlankLines = true;
        std.trimLines = true;
        Document doc = new Document(input);
        int[] regionsForSave = new int[] { 4 };
        PyFormatter.formatSelection(doc, regionsForSave, null, new PySelection(doc), std);
        assertEquals(expected, doc.get());

    }

    public void testUpdateBlankLinesInCloseRegion() throws Exception {
        // Already is properly formatted.
        String input = ""
                + "a = 10\n"
                + "  \n"
                + "        ''' check '''\n"
                + "  \n"
                + "  \n"
                + "  \n"
                + "  \n"
                + "class my:\n"
                + "    a = 10\n"
                + "    \n"
                + "    def foo():\n"
                + "";

        String expected = ""
                + "a = 10\n"
                + "  \n"
                + "        ''' check '''\n"
                + "\n"
                + "  \n"
                + "class my:\n"
                + "    a = 10\n"
                + "    \n"
                + "    def foo():\n"
                + "";

        std.manageBlankLines = true;
        std.trimLines = true;
        Document doc = new Document(input);
        int[] regionsForSave = new int[] { 5 };
        PyFormatter.formatSelection(doc, regionsForSave, null, new PySelection(doc), std);
        assertEquals(expected, doc.get());
    }

    public void testUpdateBlankLinesInCloseRegion2() throws Exception {
        String input = ""
                + "class my:\n"
                + "    def foo():\n"
                + "        a = 10\n"
                + "\n"
                + "    def foo():\n"
                + "        a = 10\n"
                + "";

        String expected = ""
                + "class my:\n"
                + "    def foo():\n"
                + "        a = 10\n"
                + "\n"
                + "\n"
                + "    def foo():\n"
                + "        a = 10\n"
                + "";

        std.manageBlankLines = true;
        std.trimLines = true;
        std.blankLinesInner = 2;
        Document doc = new Document(input);
        int[] regionsForSave = new int[] { 3 };
        PyFormatter.formatSelection(doc, regionsForSave, null, new PySelection(doc), std);
        assertEquals(expected, doc.get());
    }

    public void testUpdateBlankLinesInTry() throws Exception {
        String input = "" +
                "def a():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "def b():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "def c():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "def d():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "def e():\n" +
                "    pass\n" +
                "\n" +
                "\n" +
                "try:\n" +
                "    Err\n" +
                "except foo:\n" +
                "    pass\n" +
                "\n"
                + "";

        std.updateFormatterStyle();
        std.manageBlankLines = true;
        checkFormatResults(input);
    }

    public void testUpdate() throws Exception {
        String input = ""
                + "class Foo(object):\n" +
                "\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "def test_anything():\n" +
                "    if a := Foo():\n" +
                "        pass";

        String expected = ""
                + "class Foo(object):\n" +
                "\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "def test_anything():\n" +
                "    if a := Foo():\n" +
                "        pass";
        checkFormatResults(input, expected);
    }

    public void testUpdate2() throws Exception {
        String input = ""
                + "class Foo(object):\n" +
                "\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "def test_anything():\n" +
                "    if a :="
                + "\n"
                + "Foo():\n" +
                "        pass";

        String expected = ""
                + "class Foo(object):\n" +
                "\n" +
                "    def foo(self):\n" +
                "        pass\n" +
                "\n" +
                "\n" +
                "def test_anything():\n" +
                "    if a := "
                + "\n"
                + "Foo():\n" +
                "        pass";
        checkFormatResults(input, expected);
    }
}
