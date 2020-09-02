/**
 * Copyright (c) 2020 by Brainwy Software Ltda
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.proposals.DefaultCompletionProposalFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

import junit.framework.TestCase;

public class AssistFStringTest extends TestCase {
    private AssistFString assist;

    public static void main(String[] args) {
        try {
            AssistFStringTest test = new AssistFStringTest();
            test.setUp();
            test.tearDown();
            junit.textui.TestRunner.run(AssistFStringTest.class);
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
        assist = new AssistFString();
        CompletionProposalFactory.set(new DefaultCompletionProposalFactory());
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        CompletionProposalFactory.set(null);
    }

    public void testSimple() throws BadLocationException, MisconfigurationException {
        String d = "print('''a = %s %s''' % (a, b,))\n" +
                "\n" +
                "x = 20";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 10);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 10));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 10);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("print(f'''a = {a} {b}''')\n" +
                "\n" +
                "x = 20", doc.get());
    }

    public void testSimple2() throws BadLocationException, MisconfigurationException {
        String d = "x = 'a = %s' % some_name";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 10);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 10));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 10);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("x = f'a = {some_name}'", doc.get());
    }

    public void testSimple3() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s\" % some_name.another_name('ignore,anything,here')";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {some_name.another_name('ignore,anything,here')}\"", doc.get());
    }

    public void testSimple4() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s\" % some_name[:2]";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {some_name[:2]}\"", doc.get());
    }

    public void testSimple5() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s %s\" % (a , c)";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {a} {c}\"", doc.get());
    }

    public void testSimple6() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s %s\" % some_name";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {some_name[0]} {some_name[1]}\"", doc.get());
    }

    public void testSimple7() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s\" % some_name.another_name('ignore,anything,here').another_name2('ignore, this, here')";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {some_name.another_name('ignore,anything,here').another_name2('ignore, this, here')}\"",
                doc.get());
    }

    public void testSimple8() throws BadLocationException, MisconfigurationException {
        String d = "\"%s - %s\" % (\n" +
                "  a,\n" +
                "  b\n" +
                ")";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"{a} - {b}\"",
                doc.get());
    }

    public void testSimple9() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s %s\" % ('something' , 'foobar')";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {'something'} {'foobar'}\"", doc.get());
    }

    public void testSimple10() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s %s\" % (\"something\", \"foobar\")";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {\"something\"} {\"foobar\"}\"", doc.get());
    }

    public void testSimple11() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s %s\" % (some_str[:-1], some_str2[-1:])";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {some_str[:-1]} {some_str2[-1:]}\"", doc.get());
    }

    public void testSimple12() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s %s\" % ({'id': 1, 'value':10}, {'id': 2, 'value':20})";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {{'id': 1, 'value':10}} {{'id': 2, 'value':20}}\"", doc.get());
    }

    public void testSimple13() throws BadLocationException, MisconfigurationException {
        String d = "call('%s %s' % foo, bar)";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 10);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 10));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 10);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("call(f'{foo} {bar}')", doc.get());
    }

    public void testSimple14() throws BadLocationException, MisconfigurationException {
        String d = "call('%s %s' % foo, bar)\n" +
                "\n" +
                "x = 10";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 10);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 10));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 10);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("call(f'{foo} {bar}')\n" +
                "\n" +
                "x = 10", doc.get());
    }

    public void testSimple15() throws BadLocationException, MisconfigurationException {
        String d = "print('''a = %s %s''' % (a, b,))\n" +
                "\n" +
                "x = 10";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 10);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 10));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 10);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("print(f'''a = {a} {b}''')\n" +
                "\n" +
                "x = 10", doc.get());
    }

    public void testSimple16() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s %s\" % (\n" +
                "\"something\", \n" +
                "\"foobar\"\n" +
                ")";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 10);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 10));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 10);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {\"something\"} {\"foobar\"}\"", doc.get());
    }

    public void testSimple17() throws BadLocationException, MisconfigurationException {
        // this should give no suggestion nor error
        String d = "\"a = %s\" % {'id': 1, 'value':10}";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(0, props.size());
    }

    public void testSimple18() throws BadLocationException, MisconfigurationException {
        String d = "\"a = %s %s\" % (\n" +
                "some_str[:-1], \n" +
                "some_str2[-1:]\n" +
                ")\n"
                + "\n"
                + "x = 10";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("f\"a = {some_str[:-1]} {some_str2[-1:]}\"\n" +
                "\n" +
                "x = 10", doc.get());
    }

    public void testSimple19() throws BadLocationException, MisconfigurationException {
        String d = "call('%s %s' % foo, bar,)\n" +
                "\n" +
                "x = 10";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 10);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 10));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 10);
        assertEquals(1, props.size());
        assertEquals("Convert to f-string", props.get(0).getDisplayString());
        props.get(0).apply(doc);
        assertEquals("call(f'{foo} {bar}')\n" +
                "\n" +
                "x = 10", doc.get());
    }

    public void testSimple20() throws BadLocationException, MisconfigurationException {
        // this should give no suggestion nor error
        String d = "'%s' % (somevar";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(0, props.size());
    }

    public void testSimple21() throws BadLocationException, MisconfigurationException {
        // this should give no suggestion nor error
        String d = "''%s' % test[";

        Document doc = new Document(d);

        PySelection ps = new PySelection(doc, 5);
        String sel = PyAction.getLineWithoutComments(ps);

        assertEquals(true, assist.isValid(ps, sel, null, 5));
        List<ICompletionProposalHandle> props = assist.getProps(ps, null, null, null, null, 5);
        assertEquals(0, props.size());
    }
}
