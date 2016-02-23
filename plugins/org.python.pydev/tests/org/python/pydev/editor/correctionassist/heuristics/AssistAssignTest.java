/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.codingstd.ICodingStd;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;

/**
 * @author Fabio Zadrozny
 */
public class AssistAssignTest extends TestCase {

    static class NonCamelCodingStd implements ICodingStd {

        @Override
        public boolean localsAndAttrsCamelcase() {
            return false;
        }

    }

    static class CamelCodingStd implements ICodingStd {

        @Override
        public boolean localsAndAttrsCamelcase() {
            return true;
        }

    }

    private static final boolean DEBUG = false;
    private AssistAssign assist;

    public static void main(String[] args) {
        try {
            AssistAssignTest test = new AssistAssignTest();
            test.setUp();
            test.testCodingStd2();
            test.tearDown();
            junit.textui.TestRunner.run(AssistAssignTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assist = new AssistAssign(new CamelCodingStd());
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSimple() throws BadLocationException {
        String d = "" +
                "from testAssist import assist\n" +
                "assist.NewMethod(a,b)";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());

    }

    public void testSimple2() throws BadLocationException {
        String d = "" +
                "from testAssist import assist\n" +
                "assist.NewMethod(a = 1, b = 2)";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (newMethod)", props);
    }

    public void testSimpleUnderline() throws BadLocationException {
        String d = "" +
                "from testAssist import assist\n" +
                "assist._NewMethod(a = 1, b = 2)";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (newMethod)", props);
    }

    public void testSimple3() throws BadLocationException {
        String d = "" +
                "from testAssist import assist\n" +
                "a = assist.NewMethod(a,b)";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(false, assist.isValid(ps, sel, null, d.length()));

    }

    public void testCodingStd() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());
        String d = "" +
                "from testAssist import assist\n" +
                "assist.NewMethod(a = 1, b = 2)";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (new_method)", props);
    }

    public void testCodingStd2() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());
        String d = "" +
                "from testAssist import assist\n" +
                "assist._NewMethod(a = 1, b = 2)";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        ICompletionProposal prop0 = assertContains("Assign to local (new_method)", props);
        ICompletionProposal prop1 = assertContains("Assign to field (self._new_method)", props);

        prop0.apply(doc);

        String expected = "" +
                "from testAssist import assist\n" +
                "new_method = assist._NewMethod(a = 1, b = 2)";

        assertEquals(expected, doc.get());

        doc = new Document(d);
        prop1.apply(doc);

        expected = "" +
                "from testAssist import assist\n" +
                "self._new_method = assist._NewMethod(a = 1, b = 2)";
        assertEquals(expected, doc.get());
    }

    public void testSimple4() throws BadLocationException {
        String d = "" +
                "def m1():\n" +
                "   foo";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (foo)", props);
    }

    public void testSimple5() throws BadLocationException {
        String d = "" +
                "def m1():\n" +
                "   1+1";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (result)", props);
    }

    public void testSimple6() throws BadLocationException {
        String d = "" +
                "def m1():\n" +
                "   a = 1";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(false, assist.isValid(ps, sel, null, d.length()));
    }

    public void testSimple7() throws BadLocationException {
        String d = "" +
                "def m1():\n" +
                "   ALL_UPPERCASE";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (allUppercase)", props);
    }

    public void testSimple8() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());

        String d = "" +
                "def m1():\n" +
                "   IKVMClass";

        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (ikvmclass)", props);
    }

    public void testSimple9() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());

        String d = "" +
                "def m1():\n" +
                "   IKVMClassBBBar";

        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (ikvmclass_bbbar)", props);
    }

    public void testSimple10() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());

        String d = "" +
                "def m1():\n" +
                "   my.call().NewCall()";

        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (new_call)", props);
    }

    public void testSimple11() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());

        String d = "" +
                "CustomReportDocument(self.GetDataDirectory(),'custom_report_test')";

        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (custom_report_document)", props);
    }

    public void testSimple12() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());

        String d = "" +
                "_callMe()";

        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (call_me)", props);
    }

    public void test12a() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());

        String d = "" +
                "My20Provider()";

        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (my_20_provider)", props);
    }

    public void test13() throws BadLocationException {
        assist = new AssistAssign(new NonCamelCodingStd());

        String d = "" +
                "_GetMyFoo()";

        Document doc = new Document(d);
        PySelection ps = new PySelection(doc, new TextSelection(doc, d.length(), 0));
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, d.length()));
        List<ICompletionProposal> props = assist.getProps(ps, null, null, null, null, d.length());
        assertEquals(2, props.size());
        assertContains("Assign to local (my_foo)", props);
    }

    private ICompletionProposal assertContains(String string, List<ICompletionProposal> props) {
        StringBuffer buffer = new StringBuffer("Available: \n");

        for (ICompletionProposal proposal : props) {
            if (DEBUG) {
                System.out.println(proposal.getDisplayString());
            }
            if (proposal.getDisplayString().equals(string)) {
                return proposal;
            }
            buffer.append(proposal.getDisplayString());
            buffer.append("\n");
        }
        fail(string +
                " not found. " + buffer);
        return null;
    }
}
