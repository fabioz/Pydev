/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.docutils.PyDocIterator;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.docutils.PySelection.TddPossibleMatches;
import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class PySelectionTest extends TestCase {

    private PySelection ps;
    private Document doc;
    private String docContents;

    public static void main(String[] args) {
        try {
            PySelectionTest test = new PySelectionTest();
            test.setUp();
            test.testGetInsideParentesis();
            test.tearDown();

            junit.textui.TestRunner.run(PySelectionTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        docContents = ""
                + "TestLine1\n"
                + "TestLine2#comm2\n"
                + "TestLine3#comm3\n"
                + "TestLine4#comm4\n";
        doc = new Document(docContents);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAddLine() {
        ps = new PySelection(new Document("line1\nline2\n"), new TextSelection(doc, 0, 0));
        ps.addLine("foo", 0);
        assertEquals("line1\nfoo\nline2\n", ps.getDoc().get());

        ps = new PySelection(new Document("line1\n"), new TextSelection(doc, 0, 0));
        ps.addLine("foo", 0);
        assertEquals("line1\nfoo\n", ps.getDoc().get());

        ps = new PySelection(new Document("line1"), new TextSelection(doc, 0, 0));
        ps.addLine("foo", 0);
        assertEquals("line1\nfoo\n", ps.getDoc().get().replace("\r\n", "\n"));
    }

    /**
     * @throws BadLocationException
     *
     */
    public void testGeneral() throws BadLocationException {
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        assertEquals("TestLine1", ps.getCursorLineContents());
        assertEquals("", ps.getLineContentsToCursor());
        ps.selectCompleteLine();

        assertEquals("TestLine1", ps.getCursorLineContents());
        assertEquals("TestLine1", ps.getLine(0));
        assertEquals("TestLine2#comm2", ps.getLine(1));

        ps.deleteLine(0);
        assertEquals("TestLine2#comm2", ps.getLine(0));
        ps.addLine("TestLine1", 0);

    }

    public void testImportLine() {
        String strDoc = ""
                + "#coding                   \n"
                + "''' this should be ignored\n"
                + "from xxx import yyy       \n"
                + "import www'''             \n"
                + "#we want the import to appear after this line\n"
                + "Class C:                  \n"
                + "    pass                  \n"
                + "import kkk                \n"
                + "\n" + "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(5, selection.getLineAvailableForImport(false));
    }

    public void testImportLine2() {
        String strDoc = ""
                + "#coding                   \n"
                + "#we want the import to appear after this line\n"
                + "Class C:                  \n"
                + "    pass                  \n"
                + "import kkk                \n"
                + "\n"
                + "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(2, selection.getLineAvailableForImport(false));
    }

    public void testImportLine3() {
        String strDoc = ""
                + "#coding                   \n"
                + "#we want the import to appear after this line\n"
                + "Class C:                  \n"
                + "    pass                  \n"
                + "import kkk                \n"
                + "                          \n"
                + "''' this should be ignored\n"
                + "from xxx import yyy       \n"
                + "import www'''             \n"
                + "\n"
                + "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(2, selection.getLineAvailableForImport(false));
    }

    public void testImportLine4() {
        String strDoc = ""
                + "class SomeClass( object ):\n"
                + "    '''This is the data that should be set...\n"
                + "    '''\n"
                + "\n"
                + "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(0, selection.getLineAvailableForImport(false));
    }

    public void testImportLine5() {
        String strDoc = ""
                + "'''This is the data that should be set...\n"
                + "'''\n"
                + "\n"
                + "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(2, selection.getLineAvailableForImport(false));
    }

    public void testImportLine6() {
        String strDoc = ""
                + "\n"
                + "\n"
                + "from __future__ import xxx\n"
                + "from a import xxx\n"
                + "from __future__ import xxx\n"
                + "#we want it to appear in this line\n";
        //must be after the last from __future__ import statement
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(5, selection.getLineAvailableForImport(false));
    }

    public void testImportLine6a() {
        String strDoc = ""
                + "\n"
                + "\n"
                + "import xxx\n"
                + "import xxx\n"
                + "import xxx\n"
                + "#we want it to appear in this line\n";
        //must be after the last from import statement
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(5, selection.getLineAvailableForImport(false));
    }

    public void testImportLine7() {
        String strDoc = ""
                + "'''comment block\n"
                + "from false_import import *\n"
                + "finish comment'''\n"
                + "\n"
                + "from __future__ import xxx\n"
                + "from a import xxx\n"
                + "from __future__ import xxx\n"
                + "#we want it to appear in this line\n";
        //must be after the last from __future__ import statement
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(7, selection.getLineAvailableForImport(false));
    }

    public void testImportLine8() {
        String strDoc = ""
                + "from a import ( #foo\n"
                + "a,\n"
                + "b, #bar\n"
                + "c)\n"
                + "#we want it to appear in this line\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(4, selection.getLineAvailableForImport(false));
    }

    public void testImportLine9() {
        String strDoc = ""
                + "from a import \\\n"
                + "a,\\\n"
                + "b,\\\n"
                + "c\n"
                + "#we want it to appear in this line\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(4, selection.getLineAvailableForImport(false));
    }

    public void testImportLine10() {
        String strDoc = ""
                + "from coilib40 import unittest\n"
                + "from plugins10.plugins.editorsstack import (\n"
                + "    EditorsStackDock )\n"
                + "#we want it to appear in this line\n"
                + "def m1():\n"
                + "    testca\n"
                + "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(3, selection.getLineAvailableForImport(false));
    }

    public void testImportLine11() {
        String strDoc = ""
                + "__version__ = '$Revision: 86849 $'\n"
                + "def m1():\n"
                + "    testca\n"
                + "\n";
        Document document = new Document(strDoc);
        PySelection selection = new PySelection(document);
        assertEquals(1, selection.getLineAvailableForImport(false));
    }

    public void testSelectAll() {
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        ps.selectAll(true);
        assertEquals(docContents, ps.getCursorLineContents() + "\n");
        assertEquals(docContents, ps.getSelectedText());

        ps = new PySelection(doc, new TextSelection(doc, 0, 9)); //first line selected
        ps.selectAll(true); //changes
        assertEquals(docContents, ps.getCursorLineContents() + "\n");
        assertEquals(docContents, ps.getSelectedText());

        ps = new PySelection(doc, new TextSelection(doc, 0, 9)); //first line selected
        ps.selectAll(false); //nothing changes
        assertEquals(ps.getLine(0), ps.getCursorLineContents());
        assertEquals(ps.getLine(0), ps.getSelectedText());
    }

    public void testFullRep() throws Exception {
        String s = "v=aa.bb.cc()";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 2, 2));
        assertEquals("aa.bb.cc", ps.getFullRepAfterSelection());

        s = "v=aa.bb.cc";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 2, 2));
        assertEquals("aa.bb.cc", ps.getFullRepAfterSelection());

    }

    public void testReplaceToSelection() throws Exception {
        String s = "vvvvppppaaaa";
        doc = new Document(s);
        ps = new PySelection(doc, 4);
        ps.replaceLineContentsToSelection("xxxx");
        assertEquals("xxxxppppaaaa", ps.getDoc().get());
    }

    public void testGetInsideParentesis() throws Exception {
        String s = "def m1(self, a, b)";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        List<String> insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(2, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));
        assertEquals("b", insideParentesisToks.get(1));

        s = "def m1(self, a, b, )";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(2, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));
        assertEquals("b", insideParentesisToks.get(1));

        s = "def m1(self, a, b=None)";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        insideParentesisToks = ps.getInsideParentesisToks(true).o1;
        assertEquals(3, insideParentesisToks.size());
        assertEquals("self", insideParentesisToks.get(0));
        assertEquals("a", insideParentesisToks.get(1));
        assertEquals("b", insideParentesisToks.get(2));

        s = "def m1(self, a, b=None)";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(2, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));
        assertEquals("b", insideParentesisToks.get(1));

        //Note: as Python dropped this support, so did PyDev: in this situation (b,c) is ignored.
        s = "def m1(self, a, (b,c) )";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(1, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));

        s = "def m1(self, a, b, \nc,\nd )";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(4, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));
        assertEquals("b", insideParentesisToks.get(1));
        assertEquals("c", insideParentesisToks.get(2));
        assertEquals("d", insideParentesisToks.get(3));

        s = "def m1(self, a=(1,2))";
        doc = new Document(s);
        ps = new PySelection(doc, new TextSelection(doc, 0, 0));
        insideParentesisToks = ps.getInsideParentesisToks(false).o1;
        assertEquals(1, insideParentesisToks.size());
        assertEquals("a", insideParentesisToks.get(0));

    }

    public void testGetLastIf() throws Exception {
        String s = "if False:\n"
                + "    print foo";
        doc = new Document(s);
        ps = new PySelection(doc, doc.getLength());
        assertEquals("if False:", ps.getPreviousLineThatStartsWithToken(PySelection.TOKENS_BEFORE_ELSE));

        s = "bar False:\n"
                + "    print foo";
        doc = new Document(s);
        ps = new PySelection(doc, doc.getLength());
        assertEquals(null, ps.getPreviousLineThatStartsWithToken(PySelection.TOKENS_BEFORE_ELSE));
    }

    public void testGetLastIf2() throws Exception {
        String s = "if True:\n"
                + "  if False:\n"
                + "    print foo\n"
                + "  a = 10\n" //as we're already in this indent level, an if in the same level has to be disconsidered!
                + "  b = 20"
                + "";
        doc = new Document(s);
        ps = new PySelection(doc, doc.getLength());
        assertEquals("if True:", ps.getPreviousLineThatStartsWithToken(PySelection.TOKENS_BEFORE_ELSE));
    }

    public void testGetLastIf3() throws Exception {
        String s = "if True:\n"
                + "  if False:\n"
                + "    print foo\n"
                + "  a = (10,\n" //as we're already in this indent level, an if in the same level has to be disconsidered!
                + "20)\n"
                + "  a = 30"
                + "";
        doc = new Document(s);
        ps = new PySelection(doc, doc.getLength());
        assertEquals("if True:", ps.getPreviousLineThatStartsWithToken(PySelection.TOKENS_BEFORE_ELSE));
    }

    public void testGetLineWithoutComments() {
        String s = "a = 'ethuenoteuho#ueoth'";
        doc = new Document(s);
        ps = new PySelection(doc, doc.getLength());
        assertEquals("a =                     ", ps.getLineWithoutCommentsOrLiterals());
    }

    public void testGetCurrToken() throws BadLocationException {
        String s = " aa = bb";
        doc = new Document(s);

        ps = new PySelection(doc, 0);
        assertEquals(new Tuple<String, Integer>("", 0), ps.getCurrToken());

        ps = new PySelection(doc, 1);
        assertEquals(new Tuple<String, Integer>("aa", 1), ps.getCurrToken());

        ps = new PySelection(doc, 2);
        assertEquals(new Tuple<String, Integer>("aa", 1), ps.getCurrToken());

        ps = new PySelection(doc, doc.getLength() - 1);
        assertEquals(new Tuple<String, Integer>("bb", 6), ps.getCurrToken());

        ps = new PySelection(doc, doc.getLength());
        assertEquals(new Tuple<String, Integer>("bb", 6), ps.getCurrToken());

        s = " aa = bb ";
        doc = new Document(s);

        ps = new PySelection(doc, doc.getLength());
        assertEquals(new Tuple<String, Integer>("", 9), ps.getCurrToken());

        ps = new PySelection(doc, doc.getLength() - 1);
        assertEquals(new Tuple<String, Integer>("bb", 6), ps.getCurrToken());
    }

    public void testGetCurrDottedStatement() throws BadLocationException {
        PythonPairMatcher pairMatcher = new PythonPairMatcher();
        ps = new PySelection(new Document("a"), 0);
        assertEquals("a", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document("aa.bb"), 0);
        assertEquals("aa.bb", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document("aa.bb"), 3);
        assertEquals("aa.bb", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document(" aa.bb"), 3);
        assertEquals("aa.bb", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document(" aa().bb"), 3);
        assertEquals("aa().bb", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document(" aa().bb"), 0);
        assertEquals("", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document("a aa().bb"), 0);
        assertEquals("a", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document("a aa().bb"), 9);
        assertEquals("aa().bb", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document("a aa().bb"), 2);
        assertEquals("aa().bb", ps.getCurrDottedStatement(pairMatcher).o1);

        ps = new PySelection(new Document("a aa(1).bb"), 2);
        assertEquals("aa(1).bb", ps.getCurrDottedStatement(pairMatcher).o1);
    }

    public void testGetLine() throws Exception {
        PySelection sel = new PySelection(new Document("foo\nbla"));
        assertEquals("foo", sel.getLine());
        assertEquals(0, sel.getLineOfOffset(1));
    }

    public void testSameLine() throws Exception {
        final Document doc = new Document("foo\nbla\nxxx");
        assertEquals(true, PySelection.isInside(0, doc.getLineInformation(0)));
        assertEquals(false, PySelection.isInside(0, doc.getLineInformation(1)));

        assertEquals(true, PySelection.isInside(4, doc.getLineInformation(1)));
    }

    public void testGetLineContentsToCursor() throws BadLocationException {
        Document doc = new Document("    ");
        PySelection selection = new PySelection(doc);
        assertEquals("", selection.getLineContentsToCursor());

    }

    public void testGetCurrLineWithoutCommsOrLiterals() throws Exception {
        Document doc = new Document("a#foo\nxxx");
        PySelection selection = new PySelection(doc, 1);
        assertEquals("a", selection.getLineContentsToCursor(true, true));

        String str = ""
                + "titleEnd = ('''\n"
                + "            [#''')"
                + //get with spaces in the place of lines or comments
                "";
        doc = new Document(str);
        selection = new PySelection(doc, str.length());
        assertEquals("                 )", selection.getLineContentsToCursor(true, true));

        str = ""
                + "foopp"
                + "";
        doc = new Document(str);
        selection = new PySelection(doc, 3); //only 'foo'
        assertEquals("foo", selection.getLineContentsToCursor(true, true));

    }

    public void testDocIterator() throws Exception {
        String str = ""
                + "''\n"
                + "bla"
                + "";
        doc = new Document(str);
        PyDocIterator iterator = new PyDocIterator(doc, false, true, true);
        assertEquals("  ", iterator.next());

    }

    public void testLineStartingScope() throws Exception {
        String str = ""
                + "class Bar:\n"
                + "\n"
                + "    def m1(self):\n"
                + "        pass\n"
                + "";
        doc = new Document(str);
        PySelection ps = new PySelection(doc, 0);
        LineStartingScope nextLineThatStartsScope = ps.getNextLineThatStartsScope();
        assertEquals(0, nextLineThatStartsScope.iLineStartingScope);

        ps = new PySelection(doc, 12);
        nextLineThatStartsScope = ps.getNextLineThatStartsScope();
        assertEquals(2, nextLineThatStartsScope.iLineStartingScope);

    }

    public void testGetLineToColon() throws Exception {
        PySelection selection;

        selection = new PySelection(new Document("class A:\r\n    pass"), 0);
        assertEquals("class A:", selection.getToColon());

        selection = new PySelection(new Document("class A:"), 0);
        assertEquals("class A:", selection.getToColon());

        selection = new PySelection(new Document("class "), 0);
        assertEquals("", selection.getToColon());//no colon

        selection = new PySelection(new Document("class A(\r\na,\r\nb):\r\n    pass"), 0);
        assertEquals("class A(\r\na,\r\nb):", selection.getToColon());
    }

    public void testIsInClassOrFunctionLine() throws Exception {
        matchFunc("def __foo__( *args,\n **kwargs ): #comment");
        matchFunc("def f( x ): #comment");
        matchFunc("def f( x, (a,b) ): #comment");
        matchFunc("def f( x=10 ): #comment");
        matchFunc("def f( x=10 )   : #comment");
        matchFunc("def f( *args, **kwargs ): #comment");
        matchFunc("def __foo__( *args, **kwargs ): #comment");
        matchFunc("def f", false);

        matchClass("class __A( object ): #comment");
        matchClass("class A( object ): #comment");
        matchClass("class A( class10 ): #comment");
        matchClass("class A( class10 )   : #comment");
        matchClass("class A10( class10,b.b ): ");
        matchClass("class Information:");
        matchClass("class Information( ");
        matchClass("class Information ");
        matchClass("class Information( UserDict.UserDict, IInformation ):");
        dontMatchClass("noclass Information ");
        dontMatchClass("noclass Information:");
    }

    public void testLineBreak() throws Exception {
        List<Integer> lineOffsets = PySelection.getLineBreakOffsets("aa\r\nbb\rcc\ndd\r\na");
        compare(new Integer[] { 2, 6, 9, 12 }, lineOffsets);
    }

    public void testLineStart() throws Exception {
        List<Integer> lineOffsets;

        lineOffsets = PySelection.getLineStartOffsets("\r\n\r\n\n#comment with RenFoo\r\n");
        compare(new Integer[] { 0, 2, 4, 5, 27 }, lineOffsets);

        lineOffsets = PySelection.getLineStartOffsets("d\r\na");
        compare(new Integer[] { 0, 3 }, lineOffsets);

        lineOffsets = PySelection.getLineStartOffsets("aa\r\nbb\rcc\ndd\r\na");
        compare(new Integer[] { 0, 4, 7, 10, 14 }, lineOffsets);

        lineOffsets = PySelection.getLineStartOffsets("\n\nfoo\nfoo\n");
        compare(new Integer[] { 0, 1, 2, 6, 10 }, lineOffsets);

    }

    private void compare(Integer[] is, List<Integer> offsets) {
        for (int i = 0; i < is.length; i++) {
            if (!is[i].equals(offsets.get(i))) {
                fail(StringUtils.format("%s != %s (%s)", is[i], offsets.get(i),
                        Arrays.deepToString(is)
                                + " differs from " + offsets));
            }
        }
    }

    private void dontMatchClass(String cls) {
        assertFalse("Matched class (when it shouldn't match):" + cls,
                new PySelection(new Document(cls)).isInClassLine());

    }

    private void matchClass(String cls) {
        assertTrue("Failed to match class:" + cls, new PySelection(new Document(cls)).isInClassLine());
    }

    private void matchFunc(String func) {
        matchFunc(func, true);
    }

    private void matchFunc(String func, boolean matchOnlyComplete) {
        assertTrue("Failed to match func:" + func,
                new PySelection(new Document(func)).isInFunctionLine(matchOnlyComplete));
    }

    public void testIsInDecl() throws Exception {
        assertEquals(PySelection.DECLARATION_CLASS,
                new PySelection(new Document("class A(foo):\r\n    pass"), 7).isInDeclarationLine());
        assertEquals(0, new PySelection(new Document("class A(foo):\r\n    pass"), 9).isInDeclarationLine());

        assertEquals(PySelection.DECLARATION_METHOD,
                new PySelection(new Document("def A(foo):\r\n    pass"), 5).isInDeclarationLine());
        assertEquals(0, new PySelection(new Document("def A(foo):\r\n    pass"), 6).isInDeclarationLine());
    }

    public static void checkStrEquals(String string, String string2) {
        assertEquals(string.replace("\r\n", "\n"), string2.replace("\r\n", "\n"));
    }

    public void testIsFuture() throws Exception {
        assertFalse(PySelection.isFutureImportLine("from a import b"));
        assertTrue(PySelection.isFutureImportLine("from __future__ import b"));
        assertFalse(PySelection.isFutureImportLine("from __future import b"));
        assertFalse(PySelection.isFutureImportLine("__future__ from a import b"));
        assertTrue(PySelection.isFutureImportLine("from __future__ "));
    }

    public void testGetBeforeAndAfterMatchingChars() throws Exception {
        Document doc = new Document();
        PySelection ps = new PySelection(doc);
        assertEquals(new Tuple<String, String>("", ""), ps.getBeforeAndAfterMatchingChars('\''));
        doc.set("''ab");
        assertEquals(new Tuple<String, String>("", "''"), ps.getBeforeAndAfterMatchingChars('\''));
        ps.setSelection(1, 1);
        assertEquals(new Tuple<String, String>("'", "'"), ps.getBeforeAndAfterMatchingChars('\''));
        ps.setSelection(2, 2);
        assertEquals(new Tuple<String, String>("''", ""), ps.getBeforeAndAfterMatchingChars('\''));
        ps.setSelection(3, 3);
        assertEquals(new Tuple<String, String>("", ""), ps.getBeforeAndAfterMatchingChars('\''));
    }

    public void testGetLineOfOffset() throws Exception {
        Document doc = new Document();
        PySelection ps = new PySelection(doc);
        assertEquals(0, ps.getLineOfOffset(-10));
        assertEquals(0, ps.getLineOfOffset(0));
        assertEquals(0, ps.getLineOfOffset(10));

        doc.set("aaa");
        assertEquals(0, ps.getLineOfOffset(-10));
        assertEquals(0, ps.getLineOfOffset(0));
        assertEquals(0, ps.getLineOfOffset(10));

        doc.set("aaa\nbbb");
        assertEquals(0, ps.getLineOfOffset(-10));
        assertEquals(0, ps.getLineOfOffset(0));
        assertEquals(1, ps.getLineOfOffset(10));
    }

    public void testGetEndOfDocumentOffset() throws Exception {
        Document doc = new Document();
        PySelection ps = new PySelection(doc);
        assertEquals(0, ps.getEndOfDocummentOffset());
        doc.set("   ");
        assertEquals(3, ps.getEndOfDocummentOffset());
    }

    public void testGetParametersAfter() throws Exception {
        Document doc = new Document();
        PySelection ps = new PySelection(doc);
        assertEquals(0, ps.getParametersAfterCall(10).size());

        doc.set("MyCall(aa, bb, 10, )");
        List<String> params = ps.getParametersAfterCall(6);
        assertEquals(3, params.size());
        assertEquals("10", params.get(2));

        doc.set("MyCall   \t(aa, bb, 10, )");
        params = ps.getParametersAfterCall(6);
        assertEquals(3, params.size());
        assertEquals("10", params.get(2));

        doc.set("MyCall('a,a', (bb, 10), {a:10}, [ouo,ueo])");
        params = ps.getParametersAfterCall(6);
        assertEquals(4, params.size());
        assertEquals("'a,a'", params.get(0));
        assertEquals("(bb, 10)", params.get(1));
        assertEquals("{a:10}", params.get(2));
        assertEquals("[ouo,ueo]", params.get(3));

        doc.set("MyCall(another(call, 1, 'thn', foo))");
        params = ps.getParametersAfterCall(6);
        assertEquals(1, params.size());
        assertEquals("another(call, 1, 'thn', foo)", params.get(0));
    }

    public void testGetClassNameInLine() throws Exception {
        assertEquals("Foo", PySelection.getClassNameInLine("class Foo(obje"));
        assertEquals("Foo", PySelection.getClassNameInLine("class Foo.uesonth(obje"));
    }

    public void testGetFunctionCalls() throws Exception {
        Document doc = new Document();
        PySelection ps = new PySelection(doc);
        assertEquals(0, ps.getTddPossibleMatchesAtLine().size());

        doc.set("MyCall(aa, bb, 10, )");
        List<TddPossibleMatches> calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(1, calls.size());
        assertEquals("MyCall(", calls.get(0).toString());

        doc.set("foo.MyCall(aa, bb, 10, )");
        calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(1, calls.size());
        assertEquals("foo.MyCall(", calls.get(0).toString());

        doc.set("foo.MyCall1 (aa, bb, 10, )");
        calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(1, calls.size());
        assertEquals("foo.MyCall1 (", calls.get(0).toString());

        doc.set("call1(aa, bar.call2(), 10, )");
        calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(2, calls.size());
        assertEquals("call1(", calls.get(0).toString());
        assertEquals("bar.call2(", calls.get(1).toString());

        doc.set("def m1(foo)");
        calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(0, calls.size());

        doc.set("class Bar(object):");
        calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(0, calls.size());

        doc.set("a = (1,3)");
        calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(0, calls.size());

        doc.set("self.a.b, my.call()");
        calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(2, calls.size());
        assertEquals("self.a.b", calls.get(0).toString());
        assertEquals("my.call(", calls.get(1).toString());

        doc.set("self.call().call2()");
        calls = ps.getTddPossibleMatchesAtLine();
        assertEquals(1, calls.size());
        assertEquals("self.call(", calls.get(0).toString());
    }

    public void testIntersects() throws Exception {
        int line = 0;
        int col = 1;
        int len = 2;
        doc = new Document(" aa      ");
        ps = new PySelection(doc, line, col, len);
        assertTrue(ps.intersects(1, 2));
        assertTrue(ps.intersects(1, 10));
        assertTrue(ps.intersects(0, 10));
        assertFalse(ps.intersects(0, 0));
        assertTrue(ps.intersects(0, 2));
        assertFalse(ps.intersects(0, 1));
        assertFalse(ps.intersects(3, 0));
        assertTrue(ps.intersects(2, 0));
    }

    public void testHasFromFutureImport() throws Exception {
        assertTrue(PySelection
                .hasFromFutureImportUnicode(new Document("#test\nfrom __future__ import unicode_literals")));
        assertTrue(PySelection
                .hasFromFutureImportUnicode(new Document("#test\nfrom __future__ import \\\nunicode_literals")));
        assertTrue(PySelection
                .hasFromFutureImportUnicode(new Document("#test\nfrom __future__ import (\nnunicode_literals)")));
        assertTrue(PySelection
                .hasFromFutureImportUnicode(new Document(
                        "#test\n'''ignore this\nueonth\nusenoth'''\nfrom __future__ import (a,\nnunicode_literals)")));
    }

    public void testGetCurrentMethodLines2() throws Exception {
        int line = 4;
        int col = 4;
        int len = 0;
        doc = new Document(""
                + "def m1():\n"
                + "    def check():\n"
                + "        return 10\n"
                + "    a = 10\n"
                + "    return as\n"
                + "\n"
                + "def foo():\n"
                + "    return 10");
        ps = new PySelection(doc, line, col, len);
        Tuple<Integer, Integer> startEndLines = ps.getCurrentMethodStartEndLines();
        assertEquals(new Tuple<Integer, Integer>(0, 4), startEndLines);
    }

    public void testGetCurrentMethodLines() throws Exception {
        int line = 1;
        int col = 1;
        int len = 0;
        doc = new Document(""
                + "def m1():\n"
                + "    a = 10\n"
                + "    b = 20");
        ps = new PySelection(doc, line, col, len);
        Tuple<Integer, Integer> startEndLines = ps.getCurrentMethodStartEndLines();
        assertEquals(new Tuple<Integer, Integer>(0, 2), startEndLines);
    }

    public void testGetCurrentMethodLines3() throws Exception {
        int line = 1;
        int col = 1;
        int len = 0;
        doc = new Document(""
                + "    a = 10\n"
                + "    b = 20");
        ps = new PySelection(doc, line, col, len);
        Tuple<Integer, Integer> startEndLines = ps.getCurrentMethodStartEndLines();
        assertEquals(new Tuple<Integer, Integer>(0, 1), startEndLines);
    }

    public void testGetCurrentMethodLines4() throws Exception {
        int line = 2;
        int col = 9;
        int len = 0;
        doc = new Document(""
                + "def m1():\n"
                + "    def check():\n"
                + "        return 10\n"
                + "    a = 10\n"
                + "    return a\n"
                + "");
        ps = new PySelection(doc, line, col, len);
        Tuple<Integer, Integer> startEndLines = ps.getCurrentMethodStartEndLines();
        assertEquals(new Tuple<Integer, Integer>(1, 2), startEndLines);
    }
}
