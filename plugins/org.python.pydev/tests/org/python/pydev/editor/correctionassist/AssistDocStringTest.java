/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.correctionassist.docstrings.AssistDocString;
import org.python.pydev.shared_core.string.StringUtils;

public class AssistDocStringTest extends TestCase {

    public static void main(String[] args) {
        try {
            AssistDocStringTest test = new AssistDocStringTest();
            test.setUp();
            test.testApply();
            test.tearDown();
            junit.textui.TestRunner.run(AssistDocStringTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AssistDocString assist;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assist = new AssistDocString();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Testing the method isValid()
     */
    public void testIsValid() {
        /**
         * Dummy class for keeping data together.
         */
        class TestEntry {

            public TestEntry(String declaration, boolean expectedResult, int selectionOffset) {
                this.declaration = declaration;
                this.expectedResult = expectedResult;
                this.selectionOffset = selectionOffset;
            }

            public TestEntry(String declaration, boolean expectedResult) {
                this(declaration, expectedResult, -1);
            }

            public final String declaration;

            public final boolean expectedResult;

            public final int selectionOffset;
        }
        ;

        TestEntry testData[] = { new TestEntry("def f( x,\nb,\nc ): #comment", true, 3),
                new TestEntry("    def f(x, y,   z)  :", true), new TestEntry("def f( x='' ): #comment", true),
                new TestEntry("def f( x=\"\" ): #comment", true), new TestEntry("def f( x=[] ): #comment", true),
                new TestEntry("def f( x={a:1} ): #comment", true),
                new TestEntry("def f( x=1, *args, **kwargs ): #comment", true), new TestEntry("def f():", true),
                new TestEntry("def  f() : ", true),
                new TestEntry("def seek(self, pos: int, whence: int) -> int:", true),
                new TestEntry("def f( x ):", true), new TestEntry("def f( x ): #comment", true),
                new TestEntry("class X:", true), new TestEntry("class    X(sfdsf.sdf):", true),
                new TestEntry("clas    X(sfdsf.sdf):", false), new TestEntry("    class    X(sfdsf.sdf):", true),
                new TestEntry("class X():", true) };

        for (int i = 0; i < testData.length; i++) {
            TestEntry testEntry = testData[i];
            Document d = new Document(testEntry.declaration);
            int selectionOffset;
            if (testEntry.selectionOffset == -1) {
                selectionOffset = testEntry.declaration.length();
            } else {
                selectionOffset = testEntry.selectionOffset;
            }

            PySelection ps = new PySelection(d, new TextSelection(d, selectionOffset, 0));
            String sel = PyAction.getLineWithoutComments(ps);
            boolean expected = testEntry.expectedResult;
            boolean isValid = assist.isValid(ps, sel, null, selectionOffset);
            assertEquals(StringUtils.format("Expected %s was %s sel: %s", expected, isValid, sel), expected, isValid);
        }
    }

    public void testApply() throws Exception {
        String expected;
        expected = "def foo(a): #comment\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    @param a:\r\n" +
                "    @type a:\r\n"
                +
                "    '''";
        check(expected, "def foo(a): #comment");

        expected = "def f( x, ):\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    @param x:\r\n" +
                "    @type x:\r\n"
                +
                "    '''";
        check(expected, "def f( x, ):");

        expected = "def f( x, ):\n" +
                "    '''\n" +
                "    \n" +
                "    @param x:\n" +
                "    @type x:\n" +
                "    '''\n";
        check(expected, "def f( x, ):\n" +
                "    pass\n");

        expected = "def f( x y ):\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    '''";
        check(expected, "def f( x y ):");

        expected = "def f( x,y=10 ):\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    @param x:\r\n" +
                "    @type x:\r\n"
                +
                "    @param y:\r\n" +
                "    @type y:\r\n" +
                "    '''";
        check(expected, "def f( x,y=10 ):");

        expected = "def f( , ):\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    '''";
        check(expected, "def f( , ):");

        expected = "def f( ):\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    '''";
        check(expected, "def f( ):");

        expected = "def f(:\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    '''";
        check(expected, "def f(:");

        expected = "class f:\r\n" +
                "    '''\r\n" +
                "    \r\n" +
                "    '''";
        check(expected, "class f:");

        check("def f):", "def f):", 0);

        expected = "" +
                "def seek(self, pos:int, whence: int) -> int:\r\n" +
                "    '''\r\n" +
                "    \r\n"
                +
                "    @param pos:\r\n" +
                "    @type pos:\r\n" +
                "    @param whence:\r\n" +
                "    @type whence:\r\n"
                +
                "    '''" +
                "";
        check(expected, "def seek(self, pos:int, whence: int) -> int:");

    }

    private void check(String expected, String initial) throws BadLocationException {
        check(expected, initial, 1);
    }

    private void check(String expected, String initial, int proposals) throws BadLocationException {
        Document doc = new Document(initial);
        PySelection ps = new PySelection(doc, 0, 0);
        AssistDocString assist = new AssistDocString("@");
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, ps.getAbsoluteCursorOffset());
        assertEquals(proposals, props.size());
        if (props.size() > 0) {
            props.get(0).apply(doc);
            String expect = StringUtils.replaceNewLines(expected, "\n");
            String obtained = StringUtils.replaceNewLines(doc.get(), "\n");
            if (!expect.equals(obtained)) {
                System.out.println("====Expected====");
                System.out.println(expect);
                System.out.println("====Obtained====");
                System.out.println(obtained);
                assertEquals(expect, obtained);
            }
        }
    }

}
