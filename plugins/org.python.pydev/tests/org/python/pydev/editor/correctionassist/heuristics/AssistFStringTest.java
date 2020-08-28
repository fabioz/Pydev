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
    }
}
