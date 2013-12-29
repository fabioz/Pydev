/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 03/09/2005
 */
package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class StringUtilsTest extends TestCase {

    public static void main(String[] args) {
        try {
            StringUtilsTest test = new StringUtilsTest();
            test.setUp();
            test.testParseInt();
            test.tearDown();
            junit.textui.TestRunner.run(StringUtilsTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link StringUtils#joinIterable(Iterable, String)}.
     */
    public void testJoinIterable() {

        List<String> strings = new ArrayList<String>(3);
        strings.add("test");
        strings.add("fest");
        strings.add("zest");

        List<Integer> integers = new ArrayList<Integer>(3);
        integers.add(Integer.valueOf(1));
        integers.add(Integer.valueOf(2));
        integers.add(Integer.valueOf(3));

        Iterable<?>[] inputs = {
                strings,
                integers
        };

        String[] delimiters = {
                "\\n",
                "+"
        };

        String[] expectedResults = {
                "test\\nfest\\nzest",
                "1+2+3"
        };

        int i = 0;
        for (Iterable<?> input : inputs) {
            String delim = delimiters[i];
            String expectedResult = expectedResults[i];
            String result = StringUtils.joinIterable(delim, input);

            assertEquals(result, expectedResult);
            i++;
        }
    }

    /**
     * Test method for {@link StringUtils#joinIterable(Iterable, String)}
     * using bogus input.
     *
     * @throws Exception
     */
    public void testJoinIterableBogus() throws Exception {

        List<String> inputs = new ArrayList<String>(3);
        inputs.add("nominal");
        inputs.add(null);
        inputs.add("nominal");

        String delimiter = "\\r\\n";

        try {
            String result = StringUtils.joinIterable(delimiter, null);
            System.out.println("result = " + result);
            fail("The exception java.lang.IllegalArgumentException should have been thrown if 'objs' is null.");
        } catch (java.lang.IllegalArgumentException exception) {
            // The test succeeded by throwing the expected exception
        }
        try {
            String result = StringUtils.joinIterable(null, inputs);
            System.out.println("result = " + result);
            fail("The exception java.lang.IllegalArgumentException should have been thrown if 'delimiter' is null.");
        } catch (java.lang.IllegalArgumentException exception) {
            // The test succeeded by throwing the expected exception
        }
    }

    /**
     * Test method for {@link StringUtils#repeatString(String, int)}.
     * Already includes some bogus value coverage.
     */
    public void testRepeatString() {

        String[] inputs = { "Sun", "Java", "*", " ", "-", "André", null };
        int[] timesList = { -20, 3, 5, 4, 3, 2, 1 };

        String[] expectedResults = {
                "",
                "JavaJavaJava",
                "*****",
                "    ",
                "---",
                "AndréAndré",
                "null"
        };

        int len = inputs.length;
        for (int i = 0; i < len; i++) {
            String input = inputs[i];
            int times = timesList[i];

            String expectedResult = expectedResults[i];
            String result = StringUtils.repeatString(input, times);

            assertEquals(expectedResult, result);
        }
    }

    /**
     * Test method for {@link StringUtils#lastIndexOf(String, String)}.
     */
    public void testLastIndexOf() {

        String[] inputs = {
                "if ((method(\"test %s\" % name))):\n    print \"True\"",
                "\"\"\"test \\\"%s\"\"\" % \"\"\"fest\"\"\") # comment",
                "\"\"\"test \\\"%s\"\"\" % \"\"\"fest\"\"\") # comment # another comment?!",
                null,
                "André",
                "André",
                "for (Enumeration el=v.elements(); el.hasMoreElements(); ) {"
        };

        String[] regexes = {
                "\\%",
                "\\\\\"",
                "#",
                "\\B",
                null,
                "\u00e9",
                "\\;"
        };

        int[] expectedResults = { 22, -1, 40, -1, -1, 4, 54 };

        int len = inputs.length;
        for (int i = 0; i < len; i++) {
            String input = inputs[i];
            String regex = regexes[i];

            int expectedResult = expectedResults[i];
            int result = StringUtils.lastIndexOf(input, regex);

            assertEquals(expectedResult, result);
        }
    }

    /**
     * Test method for {@link StringUtils#indexOf(String, char, boolean)}.
     * @throws Exception
     */
    public void testIndexOf() throws Exception {

        String[] inputs = {
                "if ((method(\"test %s\" % name))):\n    print \"True\"",
                "if ((method(\"test %s\" % name))):\n    print \"True\"",
                "\"\"\"test #\\\"%s\"\"\" % \"\"\"fest\"\"\") # comment # another comment?!",
                null,
                "André",
                "André",
                "for (Enumeration el=v.elements(); el.hasMoreElements(); ) {",
                "\"whitespace =     \"# the string has ended"
        };

        char[] chars = {
                '%',
                '"',
                '#',
                '\0',
                (char) -1,
                '\u00e9',
                ';',
                ' '
        };

        // results for ignoreInStringLiteral == true
        int[] expectedResults1 = { 22, 12, 31, -1, -1, 4, 32, 20 };

        // results for ignoreInStringLiteral == false
        int[] expectedResults2 = { 18, 12, 8, -1, -1, 4, 32, 11 };

        int len = inputs.length;
        for (int i = 0; i < len; i++) {
            String input = inputs[i];
            char character = chars[i];

            int expectedResult1 = expectedResults1[i];
            int result = PyStringUtils.indexOf(input, character, true);
            assertEquals(expectedResult1, result);

            result = PyStringUtils.indexOf(input, character, false);
            int expectedResult2 = expectedResults2[i];
            assertEquals(expectedResult2, result);
        }
    }

    /**
     * Test method for {@link StringUtils#findSubstring(String, char, boolean)}.
     * @throws Exception
     */
    public void testFindSubstring() throws Exception {

        String[] inputs = {
                "if ((method(\"test %s\" % name))):\n    print \"True\"",
                "if ((method(\"test %s\" % name))):\n    print \"True\"",
                "\"\"\"test #\\\"%s\"\"\" % \"\"\"fest\"\"\") # comment # another comment?!",
                null,
                "André",
                "André",
                "for (Enumeration el=v.elements(); el.hasMoreElements(); ) {",
                "\"whitespace =     \"# the string has ended"
        };

        char[] chars = {
                '%',
                '"',
                '#',
                '\0',
                (char) -1,
                '\u00e9',
                ';',
                ' '
        };

        // results for ignoreInStringLiteral == true
        String[] expectedResults1 = {
                " name))):\n    print \"True\"",
                "test %s\" % name))):\n    print \"True\"",
                " comment # another comment?!",
                null,
                null,
                "",
                " el.hasMoreElements(); ) {",
                "the string has ended"
        };

        // results for ignoreInStringLiteral == false
        String[] expectedResults2 = {
                "s\" % name))):\n    print \"True\"",
                "test %s\" % name))):\n    print \"True\"",
                "\\\"%s\"\"\" % \"\"\"fest\"\"\") # comment # another comment?!",
                null,
                null,
                "",
                " el.hasMoreElements(); ) {",
                "=     \"# the string has ended"
        };

        int len = inputs.length;
        for (int i = 0; i < len; i++) {
            String input = inputs[i];
            char character = chars[i];

            String expectedResult1 = expectedResults1[i];
            String result = PyStringUtils.findSubstring(input, character, true);
            assertEquals(expectedResult1, result);

            result = PyStringUtils.findSubstring(input, character, false);
            String expectedResult2 = expectedResults2[i];
            assertEquals(expectedResult2, result);
        }
    }

    public void testNthIndexOf() {
        assertEquals(-1, StringUtils.nthIndexOf("", '.', 0));
        assertEquals(0, StringUtils.nthIndexOf(".", '.', 1));
        assertEquals(1, StringUtils.nthIndexOf("...", '.', 2));
        assertEquals(2, StringUtils.nthIndexOf("...", '.', 3));
        assertEquals(-1, StringUtils.nthIndexOf("...", '.', 4));
        assertEquals(1, StringUtils.nthIndexOf("A.B.C.", '.', 1));
        assertEquals(3, StringUtils.nthIndexOf("A.B.C.", '.', 2));
        assertEquals(5, StringUtils.nthIndexOf("A.B.C.", '.', 3));
        assertEquals(-1, StringUtils.nthIndexOf("A.B.C.", '.', 4));
    }

    public void testFormat() {
        assertEquals("teste", StringUtils.format("%s", new Object[] { "teste" }));
        assertEquals("teste 1",
                StringUtils.format("%s 1", new Object[] { "teste" }));
        assertEquals(
                "teste 1 2 3 teste",
                StringUtils.format("teste %s %s 3 %s", new Object[] { "1", "2",
                        "teste" }));
        assertEquals("teste 1 2 %s",
                StringUtils.format("teste 1 2 %%s", new Object[] {}));
        assertEquals("teste 1 2 3",
                StringUtils.format("teste 1 2 %s", new Object[] { "3" }));
        assertEquals("teste 1 2 3",
                StringUtils.format("%s 1 2 3", new Object[] { "teste" }));
        assertEquals("teste 1 2 3",
                StringUtils.format("%s 1 2 %s", new Object[] { "teste", 3 }));
        assertEquals("null 1 2 null",
                StringUtils.format("%s 1 2 %s", new Object[] { null, null }));
        assertEquals("", StringUtils.format("%s", new Object[] { "" }));
        assertEquals("%", StringUtils.format("%", new Object[] {}));

        assertEquals("", StringUtils.format("%1", new Object[] {}));
        assertEquals("", StringUtils.format("% ", new Object[] {}));
    }

    public void testStripExt() {
        assertEquals("teste", StringUtils.stripExtension("teste.aaa"));
        assertEquals("teste.aaa", StringUtils.stripExtension("teste.aaa.bbb"));
        assertEquals("teste", StringUtils.stripExtension("teste"));
        assertEquals("a", StringUtils.stripExtension("a.a"));
        assertEquals("", StringUtils.stripExtension(""));
    }

    public void testReplaceAllSlashes() throws Exception {
        assertEquals("foo", StringUtils.replaceAllSlashes("foo"));
        assertEquals("foo/", StringUtils.replaceAllSlashes("foo\\"));
        assertEquals("/foo/", StringUtils.replaceAllSlashes("\\foo\\"));
        assertEquals("/foo///", StringUtils.replaceAllSlashes("\\foo\\\\\\"));

    }

    public void testReplaceAll() throws Exception {
        assertEquals("foo", StringUtils.replaceAll("fjj", "j", "o"));
        assertEquals("fok", StringUtils.replaceAll("fkkkk", "kkk", "o"));
        assertEquals("foo", StringUtils.replaceAll("fkkkk", "kk", "o"));
        assertEquals("kkkkkkkkk", StringUtils.replaceAll("www", "w", "kkk"));
        assertEquals("www", StringUtils.replaceAll("www", "o", "a"));

        String initial = "" +
                "import sys; sys.ps1=''; sys.ps2=''\r\n" +
                "print >> sys.stderr, 'PYTHONPATH:'\r\n"
                +
                "for p in sys.path:\r\n" +
                "    print >> sys.stderr,  p\r\n" +
                "\r\n" + //to finish the for scope
                "print >> sys.stderr, 'Ok, all set up... Enjoy'\r\n" +
                "";
        assertEquals(initial, StringUtils.replaceAll(initial, "\r\n", "\r\n"));

        String expected = "" +
                "import sys; sys.ps1=''; sys.ps2=''\r" +
                "print >> sys.stderr, 'PYTHONPATH:'\r"
                +
                "for p in sys.path:\r" +
                "    print >> sys.stderr,  p\r" +
                "\r" + //to finish the for scope
                "print >> sys.stderr, 'Ok, all set up... Enjoy'\r" +
                "";
        assertEquals(expected, StringUtils.replaceAll(initial, "\r\n", "\r"));
    }

    public void testRemoveWhitespaceColumnsToLeft() throws Exception {
        assertEquals("foo", PyStringUtils.removeWhitespaceColumnsToLeft("   foo"));
        assertEquals("foo\n", PyStringUtils.removeWhitespaceColumnsToLeft("   foo\n"));
        assertEquals("foo\n   foo\n", PyStringUtils.removeWhitespaceColumnsToLeft(" foo\n    foo\n"));
    }

    public void testTrim() throws Exception {
        assertEquals("  foo", StringUtils.rightTrim("  foo  "));
        assertEquals("foo  ", StringUtils.leftTrim("  foo  "));
        assertEquals("\t\tfoo", StringUtils.rightTrim("\t\tfoo\t\t"));
        assertEquals("foo\t\t", StringUtils.leftTrim("\t\tfoo\t\t"));

    }

    public void testFixWhitespaceColumnsToLeftFromDocstring() throws Exception {
        assertEquals("foo", PyStringUtils.fixWhitespaceColumnsToLeftFromDocstring("foo", "    "));
        assertEquals("\n    foo", PyStringUtils.fixWhitespaceColumnsToLeftFromDocstring("\nfoo", "    "));
        assertEquals("\n    foo\n    ", PyStringUtils.fixWhitespaceColumnsToLeftFromDocstring("\nfoo\n", "    "));
        assertEquals("\n    \n    foo\n    ",
                PyStringUtils.fixWhitespaceColumnsToLeftFromDocstring("\n\nfoo\n", "    "));
    }

    public void testSplitOn1st() throws Exception {
        assertEquals(new Tuple<String, String>("aa", "bb.cc"), StringUtils.splitOnFirst("aa.bb.cc", '.'));
        assertEquals(new Tuple<String, String>("aa_bb_cc", ""), StringUtils.splitOnFirst("aa_bb_cc", '.'));

        assertEquals(new Tuple<String, String>("aa", "bb.cc"), StringUtils.splitOnFirst("aa<TAG>bb.cc", "<TAG>"));
        assertEquals(new Tuple<String, String>("aa_bb_cc", ""), StringUtils.splitOnFirst("aa_bb_cc", "TAG"));

    }

    public void testSplitWithMax() throws Exception {
        String[] split = StringUtils.split("a b c", ' ', 1).toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "a b c" }, split));

        split = StringUtils.split("a b c", ' ', 2).toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "a", "b c" }, split));

        split = StringUtils.split("aaa  bb  ", ' ', 2).toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb  " }, split));

        split = StringUtils.split("aaa  bb  ", ' ', 3).toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.split("aaa  bb  ", ' ', 1).toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa  bb  " }, split));
    }

    public void testIterLines() throws Exception {
        ArrayList<String> arrayList = new ArrayList<String>();

        Iterable<String> iterLines = StringUtils.iterLines("aa\nbb\nccc");
        Iterator<String> it = iterLines.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next());
        }
        assertEquals(Arrays.asList("aa\n", "bb\n", "ccc"), arrayList);
        arrayList.clear();

        for (String s : StringUtils.iterLines("aa")) {
            arrayList.add(s);
        }
        assertEquals(Arrays.asList("aa"), arrayList);
        arrayList.clear();

        for (String s : StringUtils.iterLines("")) {
            arrayList.add(s);
        }
        assertEquals(Arrays.asList(), arrayList);
        arrayList.clear();

        for (String s : StringUtils.iterLines("\n")) {
            arrayList.add(s);
        }
        assertEquals(Arrays.asList("\n"), arrayList);
        arrayList.clear();

        for (String s : StringUtils.iterLines("\n\na")) {
            arrayList.add(s);
        }
        assertEquals(Arrays.asList("\n", "\n", "a"), arrayList);
        arrayList.clear();

        for (String s : StringUtils.iterLines("a\n\r\n")) {
            arrayList.add(s);
        }
        assertEquals(Arrays.asList("a\n", "\r\n"), arrayList);
        arrayList.clear();

        for (String s : StringUtils.iterLines("a\n\r\n\r\r")) {
            arrayList.add(s);
        }
        assertEquals(Arrays.asList("a\n", "\r\n", "\r", "\r"), arrayList);
        arrayList.clear();

        for (String s : StringUtils.iterLines("a\n\r")) {
            arrayList.add(s);
        }
        assertEquals(Arrays.asList("a\n", "\r"), arrayList);
        arrayList.clear();
    }

    public void testSplitOnWhitespaces() throws Exception {
        String[] split = StringUtils.splitInWhiteSpaces("aaa bb  ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.splitInWhiteSpaces("aaa bb").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.splitInWhiteSpaces("   ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] {}, split));
    }

    public void testSplit() throws Exception {
        String[] split = StringUtils.split("aaa bb  ", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.split("|||", '|').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] {}, split));

        split = StringUtils.split("|a||", '|').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "a" }, split));

        split = StringUtils.split("  aaa  bb   ", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.split("aaa  bb", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.split("aaa  bb  ", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.split("aaa ", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa" }, split));

        split = StringUtils.split(" aaa", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa" }, split));

        split = StringUtils.split("aaa", ' ').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa" }, split));

        split = StringUtils.split("aaa bb\tccc\nbb ", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb", "ccc", "bb" }, split));

        split = StringUtils.split("aaa bb\t\t ccc\nbb ", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb", "ccc", "bb" }, split));

        split = StringUtils.split("aaa bb\t \n", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.split("aaa \t\nbb\t \n", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "bb" }, split));

        split = StringUtils.split("aaa", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa" }, split));

        split = StringUtils.split("aaa\t\n ", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa" }, split));

        split = StringUtils.split("\t\n  aaa\t\n ", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa" }, split));

        split = StringUtils.split("\t\n  aaa", ' ', '\t', '\n').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa" }, split));

        split = StringUtils.split(" aaa   ", new char[] { ' ' }).toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa" }, split));

        split = StringUtils.splitAndRemoveEmptyTrimmed("|| |a||b||", '|').toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "a", "b" }, split));
    }

    public void testSplitOnString() throws Exception {
        String[] split;

        split = StringUtils.split("&&2|1|2|0&&1|3|4|0&&2|1|2|0", "&&").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "2|1|2|0", "1|3|4|0", "2|1|2|0" }, split));

        split = StringUtils.split("aaa bb ccc bb kkk bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "ccc", "kkk" }, split));

        split = StringUtils.split("aaa bb ccc bb kkk bb", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "ccc", "kkk bb" }, split));

        split = StringUtils.split("aaa bb ccc bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "ccc" }, split));

        split = StringUtils.split(" bb aaa bb ccc bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "aaa", "ccc" }, split));

        split = StringUtils.split(" bb  bb ccc bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "ccc" }, split));

        split = StringUtils.split("ccc", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "ccc" }, split));

        split = StringUtils.split("", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] {}, split));

        split = StringUtils.split("", "b").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] {}, split));

        split = StringUtils.split("b", "b").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] {}, split));

        split = StringUtils.split("c", "b").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "c" }, split));

        split = StringUtils.split("", "").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] {}, split));

        split = StringUtils.split("a", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "a" }, split));

        split = StringUtils.split(" bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] {}, split));

        split = StringUtils.split(" bb b", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "b" }, split));

        split = StringUtils.split(" bb b bb", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "b bb" }, split));

        split = StringUtils.split(" bb b  bb ", " bb ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { "b " }, split));

        split = StringUtils.split(" bb b  bb ", " bb2 ").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { " bb b  bb " }, split));

        split = StringUtils.split(" bb b  bb ", "b").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { " ", " ", "  ", " " }, split));

        split = StringUtils.split(" bb bb  bb ", "bb").toArray(new String[0]);
        assertTrue(Arrays.equals(new String[] { " ", " ", "  ", " " }, split));
    }

    public void testReplaceChars() throws Exception {
        assertEquals("aaaXeeeXcccX",
                StringUtils.replaceNewLines("aaa\neee\r\nccc\r", "X"));
        assertEquals("aaabbbccc", StringUtils.removeNewLineChars("aaa\r\nbbb\rccc\n"));
    }

    public void testCodingStd() throws Exception {
        assertEquals("a_b_c", StringUtils.asStyleLowercaseUnderscores("a_b_c"));
        assertEquals("a_and_b", StringUtils.asStyleLowercaseUnderscores("aAndB"));
        assertEquals("abc", StringUtils.asStyleLowercaseUnderscores("ABC"));
        assertEquals("a_b_c", StringUtils.asStyleLowercaseUnderscores("A_B_C"));
        assertEquals("a_bd_c", StringUtils.asStyleLowercaseUnderscores("A_BD_C"));
        assertEquals("my_camel_call", StringUtils.asStyleLowercaseUnderscores("MyCamelCall"));
        assertEquals("__a__b__c__", StringUtils.asStyleLowercaseUnderscores("__a__b__c__"));
        assertEquals("__a__b__c__", StringUtils.asStyleLowercaseUnderscores("__a__B__c__"));
        assertEquals("__a_b__b__c__", StringUtils.asStyleLowercaseUnderscores("__aB__B__c__"));
        assertEquals("", StringUtils.asStyleLowercaseUnderscores(""));
        assertEquals("a", StringUtils.asStyleLowercaseUnderscores("a"));
        assertEquals("a", StringUtils.asStyleLowercaseUnderscores("A"));
        assertEquals("aa", StringUtils.asStyleLowercaseUnderscores("AA"));
        assertEquals("aab", StringUtils.asStyleLowercaseUnderscores("AAb"));
        assertEquals("-*&()_1", StringUtils.asStyleLowercaseUnderscores("-*&()1"));
        assertEquals("my_constant", StringUtils.asStyleLowercaseUnderscores("MY_CONSTANT"));

        assertEquals("myCamelCall", StringUtils.asStyleCamelCaseFirstLower("MyCamelCall"));
        assertEquals("myLowerCall", StringUtils.asStyleCamelCaseFirstLower("my_lower_call"));
        assertEquals("__myLowerCall__", StringUtils.asStyleCamelCaseFirstLower("__my__lower__call__"));
        assertEquals("__myLOowerCall__", StringUtils.asStyleCamelCaseFirstLower("__my__lOower__call__"));
        assertEquals("", StringUtils.asStyleCamelCaseFirstLower(""));
        assertEquals("a", StringUtils.asStyleCamelCaseFirstLower("a"));
        assertEquals("a", StringUtils.asStyleCamelCaseFirstLower("A"));
        assertEquals("ab", StringUtils.asStyleCamelCaseFirstLower("Ab"));
        assertEquals("myConstant", StringUtils.asStyleCamelCaseFirstLower("MY_CONSTANT"));

        assertEquals("Ab", StringUtils.asStyleCamelCaseFirstUpper("Ab"));
        assertEquals("", StringUtils.asStyleCamelCaseFirstUpper(""));
        assertEquals("A", StringUtils.asStyleCamelCaseFirstUpper("a"));
        assertEquals("AB", StringUtils.asStyleCamelCaseFirstUpper("a_b"));
        assertEquals("ABc", StringUtils.asStyleCamelCaseFirstUpper("a_bc"));
        assertEquals("-*&()1", StringUtils.asStyleCamelCaseFirstUpper("-*&()1"));
        assertEquals("MyConstant", StringUtils.asStyleCamelCaseFirstUpper("MY_CONSTANT"));

    }

    public void testRemoveWhitespaceColumnsToLeftAndApplyIndent() {
        assertEquals("    a=10\n#comment",
                PyStringUtils.removeWhitespaceColumnsToLeftAndApplyIndent("a=10\n#comment", "    ", false));
        assertEquals("    a=10\n#comment\n    b=30",
                PyStringUtils.removeWhitespaceColumnsToLeftAndApplyIndent("a=10\n#comment\nb=30", "    ", false));
        assertEquals("    a=10\n    #comment",
                PyStringUtils.removeWhitespaceColumnsToLeftAndApplyIndent("a=10\n#comment", "    ", true));
        assertEquals("    a=10\n    #comment\n    b=30",
                PyStringUtils.removeWhitespaceColumnsToLeftAndApplyIndent("a=10\n#comment\nb=30", "    ", true));
        assertEquals("    a=10\n    \n    b=30",
                PyStringUtils.removeWhitespaceColumnsToLeftAndApplyIndent("    a=10\n\n    b=30", "    ", true));
    }

    public void testIsPythonIdentifier() throws Exception {
        assertFalse(PyStringUtils.isPythonIdentifier(""));
        assertFalse(PyStringUtils.isPythonIdentifier("1aa"));
        assertFalse(PyStringUtils.isPythonIdentifier("a!1"));
        assertFalse(PyStringUtils.isPythonIdentifier("a1'"));

        assertTrue(PyStringUtils.isPythonIdentifier("a"));
        assertTrue(PyStringUtils.isPythonIdentifier("a1"));
        assertTrue(PyStringUtils.isPythonIdentifier("a1��"));
    }

    public void testGetFirstWithUpper() throws Exception {
        assertEquals("", StringUtils.getWithFirstUpper(""));
        assertEquals("A", StringUtils.getWithFirstUpper("a"));
        assertEquals("Aa", StringUtils.getWithFirstUpper("aa"));
    }

    public void testIndentTo() throws Exception {
        assertEquals("", StringUtils.indentTo("", ""));
        assertEquals("  aa\n  bb", StringUtils.indentTo("aa\nbb", "  "));
        assertEquals(" a", StringUtils.indentTo("a", " "));
    }

    public void testMd5() throws Exception {
        assertEquals("ck2u8j60r58fu0sgyxrigm3cu", StringUtils.md5(""));
        assertEquals("4l3c9nzlvo3spzkuri5l3r4si",
                org.python.pydev.shared_core.string.StringUtils
                        .md5("c:\\my_really\\big\\python\\path\\executable\\is_\\very_very_very\\long\\python.exe"));
    }

    public void testJoin() throws Exception {
        assertEquals("", StringUtils.join("", "", ""));
        assertEquals("null", StringUtils.join("", null, ""));
        assertEquals("nulla", StringUtils.join("", null, "a"));

        assertEquals("rara", StringUtils.join("rara", "", ""));
        assertEquals("nullrara", StringUtils.join("rara", null, ""));
        assertEquals("nullwhata", StringUtils.join("what", null, "a"));

        assertEquals("ra", StringUtils.join("ra", "", ""));
        assertEquals("nullra", StringUtils.join("ra", null, ""));
        assertEquals("nullwha", StringUtils.join("wh", null, "a"));

        assertEquals(";", StringUtils.join(";", "", ""));
        assertEquals("null;", StringUtils.join(";", null, ""));
        assertEquals("null;a", StringUtils.join(";", null, "a"));
        assertEquals("null", StringUtils.join(";", (Object) null));
        assertEquals("null", StringUtils.join("", (Object) null));
        assertEquals("nullnulla",
                StringUtils.join("", (Object) null, (Object) null, "a"));

        assertEquals("b",
                StringUtils.join("/", new String[] { "a", "b", "c" }, 1, 2));
        assertEquals("b/c",
                StringUtils.join("/", new String[] { "a", "b", "c" }, 1, 3));

        List<String> l = new ArrayList<String>();
        l.add("b");
        l.add("c");
        assertEquals("b/c", StringUtils.join("/", l));
    }

    public void testParseInt() throws Exception {
        FastStringBuffer buf = new FastStringBuffer();
        try {
            StringUtils.parsePositiveLong(buf);
            fail("Expecting exception");
        } catch (NumberFormatException e) {
            //empty
        }

        buf.append("0");
        assertEquals(0, StringUtils.parsePositiveLong(buf));

        buf.append("10");
        assertEquals(10, StringUtils.parsePositiveLong(buf));

        buf.append("1");
        assertEquals(101, StringUtils.parsePositiveLong(buf));

        buf.append("a");
        try {
            StringUtils.parsePositiveLong(buf);
            fail("Expecting exception");
        } catch (NumberFormatException e) {
            //empty
        }
        buf.deleteLast();
        assertEquals(101, StringUtils.parsePositiveLong(buf));

        buf.append("4");
        assertEquals(1014, StringUtils.parsePositiveLong(buf));

        buf.append("9");
        assertEquals(10149, StringUtils.parsePositiveLong(buf));
    }

    public void testShorten() throws Exception {
        assertEquals("aaa ... bbb", StringUtils.shorten("aaaccccccbbb", 11));
        assertEquals("aaa ... bb1", StringUtils.shorten("aaaccccccbbb1", 11));
        assertEquals("aaa ... bb12", StringUtils.shorten("aaaccccccbbb12", 12));
    }

    public void testValidString() throws Exception {
        String s = "abrscuosanutha12323421\t\r\n `$@!$%^$&*(*()&áéíóúâAUOEÁÉ";
        assertEquals(StringUtils.isValidTextString(s.getBytes(), s.getBytes().length), true);

        byte[] b = new byte[] { 0xF, 0xF, 0xF, 0xD, 0, 2, 3 };
        assertEquals(!StringUtils.isValidTextString(b, b.length), true);
    }

    public void testValidString2() throws Exception {
        String s = "";
        assertEquals(StringUtils.isValidTextString(s.getBytes(), 0), true);
    }

    public void testValidString3() throws Exception {
        String s = "";
        assertEquals(StringUtils.isValidTextString(s.getBytes(), -1), true);
    }

    public void testValidString4() throws Exception {
        String s = "a";
        assertEquals(StringUtils.isValidTextString(s.getBytes(), 4), true);
    }
}
