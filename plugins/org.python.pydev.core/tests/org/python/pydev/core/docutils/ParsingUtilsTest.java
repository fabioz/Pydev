/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 14, 2006
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class ParsingUtilsTest extends TestCase {

    public static void main(String[] args) {
        try {
            ParsingUtilsTest test = new ParsingUtilsTest();
            test.setUp();
            test.testFindNextChar();
            test.tearDown();
            junit.textui.TestRunner.run(ParsingUtilsTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsInCommOrStr() {
        String str = "" +
                "#comm1\n" +
                "'str'\n" +
                "pass\n" +
                "";
        assertEquals(ParsingUtils.PY_COMMENT, ParsingUtils.getContentType(str, 2));
        assertEquals(ParsingUtils.PY_SINGLELINE_BYTES1, ParsingUtils.getContentType(str, 10));
        assertEquals(ParsingUtils.PY_DEFAULT, ParsingUtils.getContentType(str, 17));
    }

    public void testIsInCommOrStr2() {
        String str = "" +
                "'''\n" +
                "foo\n" +
                "'''" +
                "";
        assertEquals(ParsingUtils.PY_DEFAULT, ParsingUtils.getContentType(str, str.length()));
        assertEquals(ParsingUtils.PY_DEFAULT, ParsingUtils.getContentType(str, str.length() - 1));
        assertEquals(ParsingUtils.PY_MULTILINE_BYTES1, ParsingUtils.getContentType(str, str.length() - 2));
    }

    public void testEatComments() {
        String str = "" +
                "#comm1\n" +
                "pass\n" +
                "";
        ParsingUtils parsingUtils = ParsingUtils.create(str);
        int i = parsingUtils.eatComments(null, 0);
        assertEquals('\n', parsingUtils.charAt(i));
    }

    public void testEatLiterals() throws SyntaxErrorException {
        String str = "" +
                "'''\n" +
                "pass\n" +
                "'''" +
                "w" +
                "";
        ParsingUtils parsingUtils = ParsingUtils.create(str);
        int i = parsingUtils.eatLiterals(null, 0);
        assertEquals(11, i);
        assertEquals('\'', parsingUtils.charAt(i));
    }

    public void testInvalidSyntax() {
        String str = "" +
                "'" + //not properly closed
                "";
        ParsingUtils parsingUtils = ParsingUtils.create(str, true);
        try {
            parsingUtils.eatLiterals(null, 0);
            fail("Expected invalid code.");
        } catch (SyntaxErrorException e) {
            //expected
        }

        str = "" +
                "'''" + //not properly closed
                "";
        parsingUtils = ParsingUtils.create(str, true);
        try {
            parsingUtils.eatLiterals(null, 0);
            fail("Expected invalid code.");
        } catch (SyntaxErrorException e) {
            //expected
        }

        str = "" +
                "(" + //not properly closed
                "";
        parsingUtils = ParsingUtils.create(str, true);
        try {
            parsingUtils.eatPar(0, null);
            fail("Expected invalid code.");
        } catch (SyntaxErrorException e) {
            //expected
        }

    }

    public void testEatWhitespaces() {
        String str = "" +
                "    #comm\n" +
                "pass\n" +
                "";
        ParsingUtils parsingUtils = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        int i = parsingUtils.eatWhitespaces(buf, 0);
        assertEquals(3, i);
        assertEquals("    ", buf.toString());
        assertEquals(' ', parsingUtils.charAt(i));
    }

    public void testEatWhitespaces2() {
        String str = "" +
                "    ";
        ParsingUtils parsingUtils = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        int i = parsingUtils.eatWhitespaces(buf, 0);
        assertEquals("    ", buf.toString());
        assertEquals(' ', parsingUtils.charAt(i));
        assertEquals(3, i);
    }

    public void testIterator() throws Exception {
        String str = "" +
                "#c\n" +
                "'s'\n" +
                "pass\n" +
                "";
        Document d = new Document(str);
        Iterator<String> it = ParsingUtils.getNoLiteralsOrCommentsIterator(d);
        assertEquals("\n", it.next());
        assertEquals(true, it.hasNext());
        assertEquals("\n", it.next());
        assertEquals(true, it.hasNext());
        assertEquals("pass\n", it.next());
        assertEquals(false, it.hasNext());
    }

    public void testGetFlattenedLine() throws Exception {
        String str = "" +
                "line #c\n" +
                "start =\\\n" +
                "10 \\\n" +
                "30\n" +
                "call(\n" +
                "   ttt,\n" +
                ")\n";
        ParsingUtils parsing = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        assertEquals(6, parsing.getFullFlattenedLine(0, buf.clear()));
        assertEquals('c', str.charAt(6));
        assertEquals("line ", buf.toString());

        parsing.getFullFlattenedLine(1, buf.clear());
        assertEquals("ine ", buf.toString());

        assertEquals(23, parsing.getFullFlattenedLine(8, buf.clear()));
        assertEquals('0', str.charAt(23));
        assertEquals("start =10 30", buf.toString());

        assertEquals(39, parsing.getFullFlattenedLine(25, buf.clear()));
        assertEquals("call", buf.toString());
        assertEquals(')', str.charAt(39));
    }

    public void testGetFlattenedLine2() throws Exception {
        String str = "" +
                "line = '''\n" +
                "bla bla bla''' = xxy\n" +
                "what";
        ParsingUtils parsing = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        assertEquals(30, parsing.getFullFlattenedLine(0, buf.clear()));
        assertEquals('y', str.charAt(30));
        assertEquals("line =  = xxy", buf.toString());
    }

    public void testGetFlattenedLine3() throws Exception {
        String str = "" +
                "a = c(\r\n" +
                "a)\r\n" +
                "b = 20\r\n";
        ParsingUtils parsing = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        assertEquals(9, parsing.getFullFlattenedLine(0, buf.clear()));
        assertEquals("a = c", buf.toString());
        assertEquals(')', str.charAt(9));
    }

    public void testGetFlattenedLine4() throws Exception {
        String str = "" +
                "a = c(\r" +
                "a)\r" +
                "b = 20\r";
        ParsingUtils parsing = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        assertEquals(8, parsing.getFullFlattenedLine(0, buf.clear()));
        assertEquals("a = c", buf.toString());
        assertEquals(')', str.charAt(8));
    }

    public void testGetFlattenedLine5() throws Exception {
        String str = "" +
                "a = c(\n" +
                "a)\n" + //char 8 == )
                "b = 20\n";
        ParsingUtils parsing = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        assertEquals(')', str.charAt(8));
        assertEquals(8, parsing.getFullFlattenedLine(0, buf.clear()));
        assertEquals("a = c", buf.toString());
    }

    public void testGetFlattenedLine6() throws Exception {
        String str = "" +
                "a = '''" +
                "a)\n" +
                "'''\n" +
                "b = 10";
        ParsingUtils parsing = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        assertEquals(12, parsing.getFullFlattenedLine(0, buf.clear()));
        assertEquals("a = ", buf.toString());
        assertEquals('\'', str.charAt(12));
    }

    public void testGetFlattenedLine7() throws Exception {
        String str = "" +
                "a = '''" +
                "a)\n" +
                "'''";
        ParsingUtils parsing = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        assertEquals(12, parsing.getFullFlattenedLine(0, buf.clear()));
        assertEquals("a = ", buf.toString());
        assertEquals('\'', str.charAt(12));
    }

    public void testGetFlattenedLine8() throws Exception {
        String str = "" +
                "a = '''" +
                "a)\n" +
                "'''\\";
        ParsingUtils parsing = ParsingUtils.create(str);
        FastStringBuffer buf = new FastStringBuffer();
        assertEquals(13, parsing.getFullFlattenedLine(0, buf.clear()));
        assertEquals("a = ", buf.toString());
        assertEquals('\\', str.charAt(13));
    }

    public void testIterator2() throws Exception {
        String str = "" +
                "#c\n" +
                "'s'" +
                "";
        Document d = new Document(str);
        PyDocIterator it = (PyDocIterator) ParsingUtils.getNoLiteralsOrCommentsIterator(d);
        assertEquals(-1, it.getLastReturnedLine());

        assertEquals("\n", it.next());
        assertEquals(0, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("", it.next());
        assertEquals(1, it.getLastReturnedLine());
        assertEquals(false, it.hasNext());
    }

    public void testIterator3() throws Exception {
        String str = "" +
                "#c";
        Document d = new Document(str);
        PyDocIterator it = (PyDocIterator) ParsingUtils.getNoLiteralsOrCommentsIterator(d);
        assertEquals(-1, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("", it.next());
        assertEquals(0, it.getLastReturnedLine());
        assertEquals(false, it.hasNext());
    }

    public void testIterator5() throws Exception {
        String str = "" +
                "class Foo:\n" +
                "    '''\n" +
                "    \"\n" +
                "    b\n" +
                "    '''a\n" +
                "    pass\n" +
                "\n";
        Document d = new Document(str);
        PyDocIterator it = new PyDocIterator(d, false, true, true);
        assertEquals(-1, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("class Foo:", it.next());
        assertEquals(0, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("       ", it.next());
        assertEquals(1, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("     ", it.next());
        assertEquals(2, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("     ", it.next());
        assertEquals(3, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("       a", it.next());
        assertEquals(4, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("    pass", it.next());
        assertEquals(5, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("", it.next());
        assertEquals(6, it.getLastReturnedLine());
        assertEquals(false, it.hasNext());
    }

    public void testIterator7() throws Exception {
        String str = "" +
                "'''\n" +
                "\n" +
                "'''\n" +
                "";
        Document d = new Document(str);
        PyDocIterator it = new PyDocIterator(d, false, true, true);

        assertEquals("   ", it.next());
        assertEquals(0, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("", it.next());
        assertEquals(1, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("   ", it.next());
        assertEquals(2, it.getLastReturnedLine());
        assertEquals(false, it.hasNext());
    }

    public void testIterator6() throws Exception {
        String str = "" +
                "'''\n" +
                "\n" +
                "'''\n" +
                "class Foo:\n" +
                "    '''\n" +
                "    \"\n" +
                "    b\n"
                +
                "    '''a\n" +
                "    pass\n" +
                "    def m1(self):\n" +
                "        '''\n" +
                "        eueueueueue\n"
                +
                "        '''\n" +
                "\n" +
                "\n";
        Document d = new Document(str);
        PyDocIterator it = new PyDocIterator(d, false, true, true);
        assertEquals(-1, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());
        for (int i = 0; i < d.getNumberOfLines() - 1; i++) {
            it.next();
            assertEquals(i, it.getLastReturnedLine());
            if (i == d.getNumberOfLines() - 2) {
                assertTrue("Failed at line:" + i, !it.hasNext());

            } else {
                assertTrue("Failed at line:" + i, it.hasNext());
            }
        }
    }

    public void testIterator4() throws Exception {
        String str = "" +
                "pass\r" +
                "foo\n" +
                "bla\r\n" +
                "what";
        Document d = new Document(str);
        PyDocIterator it = (PyDocIterator) ParsingUtils.getNoLiteralsOrCommentsIterator(d);
        assertEquals(-1, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("pass\r", it.next());
        assertEquals(0, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("foo\n", it.next());
        assertEquals(1, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("bla\r\n", it.next());
        assertEquals(2, it.getLastReturnedLine());
        assertEquals(true, it.hasNext());

        assertEquals("what", it.next());
        assertEquals(3, it.getLastReturnedLine());
        assertEquals(false, it.hasNext());
    }

    public void testMakeParseable() throws Exception {
        assertEquals("a=1\r\n", ParsingUtils.makePythonParseable("a=1", "\r\n"));

        String code = "class C:\n" +
                "    pass";
        String expected = "class C:\r\n" +
                "    pass\r\n" +
                "\r\n";
        assertEquals(expected, ParsingUtils.makePythonParseable(code, "\r\n"));

        code = "class C:" +
                "";
        expected = "class C:\r\n" +
                "";
        assertEquals(expected, ParsingUtils.makePythonParseable(code, "\r\n"));

        code = "    def m1(self):" +
                "";
        expected = "    def m1(self):\r\n" +
                "";
        assertEquals(expected, ParsingUtils.makePythonParseable(code, "\r\n"));

        code = "class C:\n" +
                "    pass\n" +
                "a = 10";
        expected = "class C:\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "a = 10" +
                "\r\n";
        assertEquals(expected, ParsingUtils.makePythonParseable(code, "\r\n"));

        code = "class C:\n" +
                "    \n" +
                "    pass\n" +
                "a = 10";
        expected = "class C:\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "a = 10" +
                "\r\n";
        assertEquals(expected, ParsingUtils.makePythonParseable(code, "\r\n"));

        code = "class AAA:\n" +
                "    \n" +
                "    \n" +
                "    def m1(self):\n" +
                "        self.bla = 10\n" +
                "\n" +
                "";
        expected = "class AAA:\r\n" +
                "    def m1(self):\r\n" +
                "        self.bla = 10\r\n" +
                "\r\n";
        assertEquals(expected, ParsingUtils.makePythonParseable(code, "\r\n"));

        code = "a=10" +
                "";
        expected = "\na=10\n" +
                "";
        assertEquals(expected, ParsingUtils.makePythonParseable(code, "\n", new FastStringBuffer("    pass", 16)));
    }

    public void testEatLiteralBackwards() throws Exception {
        String str = "" +
                "'''\n" +
                "pass\n" +
                "'''" +
                "w" +
                "";
        ParsingUtils parsingUtils = ParsingUtils.create(str, true);
        FastStringBuffer buf = new FastStringBuffer();
        int i = parsingUtils.eatLiteralsBackwards(buf.clear(), 11);
        assertEquals(0, i);
        assertEquals('\'', parsingUtils.charAt(i));
        assertEquals("'''\npass\n'''", buf.toString());

        str = "" +
                "'ue'" +
                "";
        parsingUtils = ParsingUtils.create(str, true);
        assertEquals(0, parsingUtils.eatLiteralsBackwards(buf.clear(), 3));
        assertEquals("'ue'", buf.toString());

        str = "" +
                "ue'" +
                "";
        parsingUtils = ParsingUtils.create(str, true);
        try {
            parsingUtils.eatLiteralsBackwards(buf.clear(), 2);
            fail("Expecting syntax error");
        } catch (SyntaxErrorException e) {
            //expected
            assertEquals("", buf.toString());
        }

        str = "" +
                " '  \\'  \\'ue'" +
                "";
        parsingUtils = ParsingUtils.create(str, true);
        parsingUtils.eatLiteralsBackwards(buf.clear(), str.length() - 1);
        assertEquals("'  \\'  \\'ue'", buf.toString());
    }

    public void testRemoveCommentsWhitespacesAndLiterals() throws Exception {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append("#c\n#f\n#c\n");
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf, false, false);
        assertEquals(buf.toString(), "");

        buf = new FastStringBuffer();
        buf.append("#\n#\n#\n");
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf, false, false);
        assertEquals(buf.toString(), "");

        buf = new FastStringBuffer();
        buf.append("#\n#f\n#\n");
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf, false, false);
        assertEquals(buf.toString(), "");
    }

    public void testFindNextChar() throws Exception {
        String s = "aaaaaa()";
        ParsingUtils parsingUtils = ParsingUtils.create(s);
        assertEquals(6, parsingUtils.findNextChar(0, '('));
        assertEquals(7, parsingUtils.eatPar(6, null));
    }

    public void testEatFromImportStatement() throws Exception {
        String s = "from";
        ParsingUtils parsingUtils = ParsingUtils.create(s);
        FastStringBuffer buf = new FastStringBuffer();

        assertEquals(0, parsingUtils.eatFromImportStatement(null, 0));

        s = "from ";
        parsingUtils = ParsingUtils.create(s);
        assertEquals(5, parsingUtils.eatFromImportStatement(null, 0));

        s = "from\t";
        parsingUtils = ParsingUtils.create(s);
        assertEquals(s.length(), parsingUtils.eatFromImportStatement(buf, 0));
        assertEquals(s, buf.toString());

        s = "from a import (#comment\nx)";
        parsingUtils = ParsingUtils.create(s);
        buf = new FastStringBuffer();
        assertEquals(s.length(), parsingUtils.eatFromImportStatement(buf, 0));
        assertEquals("from a import (x)", buf.toString());

        s = "from a import \\\nx";
        parsingUtils = ParsingUtils.create(s);
        buf = new FastStringBuffer();
        assertEquals(s.length(), parsingUtils.eatFromImportStatement(buf, 0));
        assertEquals(s, buf.toString());

        s = "from a import \\\r\nx";
        parsingUtils = ParsingUtils.create(s);
        buf = new FastStringBuffer();
        assertEquals(s.length(), parsingUtils.eatFromImportStatement(buf, 0));
        assertEquals(s, buf.toString());

        s = "from a import x #comment";
        parsingUtils = ParsingUtils.create(s);
        buf = new FastStringBuffer();
        assertEquals(s.length(), parsingUtils.eatFromImportStatement(buf, 0));
        assertEquals("from a import x ", buf.toString());

    }
}
