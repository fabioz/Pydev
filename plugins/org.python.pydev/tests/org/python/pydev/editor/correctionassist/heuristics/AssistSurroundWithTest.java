/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.TestCaseUtils;
import org.python.pydev.core.docutils.PySelection;

/**
 * @author fabioz
 *
 */
public class AssistSurroundWithTest extends TestCase {

    public static void main(String[] args) {
        try {
            AssistSurroundWithTest builtins = new AssistSurroundWithTest();
            builtins.setUp();
            builtins.testSurround();
            builtins.tearDown();

            junit.textui.TestRunner.run(AssistSurroundWithTest.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testSurround2() throws Exception {
        AssistSurroundWith assistSurroundWith = new AssistSurroundWith();
        IDocument doc = new Document("" +
                "def m1():\n" +
                "\n" +
                "#c\n" +
                "    a = 10\n" +
                "\n" +
                "\n");
        PySelection ps = new PySelection(doc, 1, 0, 13);
        int offset = ps.getAbsoluteCursorOffset();
        List<ICompletionProposal> props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        props.get(0).apply(doc);
        TestCaseUtils.assertContentsEqual("" +
                "def m1():\n" +
                "    try:\n" +
                "    \n" +
                "    #c\n" +
                "        a = 10\n"
                +
                "    except${cursor}:\n" +
                "        raise\n" +
                "\n" +
                "\n" +
                "", doc.get());
    }

    public void testSurround3() throws Exception {
        AssistSurroundWith assistSurroundWith = new AssistSurroundWith();
        IDocument doc = new Document("" +
                "def m1():\n" +
                "\n" +
                "#c\n" +
                "#    a = 10\n" +
                "\n" +
                "\n");
        PySelection ps = new PySelection(doc, 1, 0, 14);
        int offset = ps.getAbsoluteCursorOffset();
        List<ICompletionProposal> props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        props.get(0).apply(doc);
        TestCaseUtils.assertContentsEqual("" +
                "def m1():\n" +
                "try:\n" +
                "    \n" +
                "    #c\n" +
                "    #    a = 10\n"
                +
                "except${cursor}:\n" +
                "    raise\n" +
                "\n" +
                "\n" +
                "", doc.get());
    }

    public void testSurround() throws Exception {
        AssistSurroundWith assistSurroundWith = new AssistSurroundWith();
        int offset = 0;
        IDocument doc = new Document("a = 10");
        PySelection ps = new PySelection(doc, 0, 0, 3);
        List<ICompletionProposal> props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        props.get(0).apply(doc);
        TestCaseUtils.assertContentsEqual("" +
                "try:\n" +
                "    a = 10\n" +
                "except${cursor}:\n" +
                "    raise" +
                "",
                doc.get());

        doc = new Document("" +
                "def m1():\n" +
                "\n" +
                "\n" +
                "    a = 10\n" +
                "\n" +
                "\n");
        ps = new PySelection(doc, 1, 0, 11);
        props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        props.get(0).apply(doc);
        TestCaseUtils.assertContentsEqual("" +
                "def m1():\n" +
                "    try:\n" +
                "    \n" +
                "    \n" +
                "        a = 10\n"
                +
                "    except${cursor}:\n" +
                "        raise\n" +
                "\n" +
                "\n" +
                "", doc.get());

        doc = new Document("" +
                "\n" +
                "\n" +
                "\n");
        ps = new PySelection(doc, 1, 0, 1);
        props = assistSurroundWith.getProps(ps, null, null, null, null, offset);
        assertEquals(0, props.size());
    }
}
