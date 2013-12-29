/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.StringUtils;

public class PythonApplyCompletionsTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            PythonApplyCompletionsTest test = new PythonApplyCompletionsTest();
            test.setUp();
            test.testApply3();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonApplyCompletionsTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            public Object call(CompletionRecursionException e) {
                throw new RuntimeException("Recursion error:" + Log.getExceptionStr(e));
            }

        };
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
        PyCodeCompletion.onCompletionRecursionException = null;
    }

    public void testApply1() throws Exception {
        String s0 = "from extendable.nested2 import mod2, mod3\n" +
                "mod%s";

        String s = StringUtils.format(s0, "2");

        int offset = s.length() - 1;
        ICompletionProposal[] proposals = requestCompl(s, offset, -1, new String[] {});
        assertEquals(1, proposals.length);
        PyLinkedModeCompletionProposal p = (PyLinkedModeCompletionProposal) proposals[0];
        Document d = new Document(s);
        p.fLen = 1;
        p.applyOnDoc(offset, true, d, 3, '\n');
        assertEquals(StringUtils.format(s0, "3"), d.get());
    }

    public void testApply2() throws Exception {
        String s = "class XX:\n" +
                "    def method1(self, a, b):\n" +
                "        return 1\n" +
                "    def foo(self):\n"
                +
                "        self.metho";

        int offset = s.length();
        ICompletionProposal[] proposals = requestCompl(s, offset, -1, new String[] {});
        assertEquals(1, proposals.length);
        PyLinkedModeCompletionProposal p = (PyLinkedModeCompletionProposal) proposals[0];
        Document d = new Document(s);
        p.applyOnDoc(offset, false, d, "metho".length(), '.');
        //System.out.println(d.get());
        assertEquals(s +
                "d1.", d.get());
        assertEquals(new Point(d.getLength(), 0), p.getSelection(d));
    }

    public void testApply3() throws Exception {
        String s = "class XX:\n" +
                "    def method1(self, a, b):\n" +
                "        return 1\n" +
                "    def foo(self):\n"
                +
                "        self.metho";

        int offset = s.length() - 1;
        ICompletionProposal[] proposals = requestCompl(s, offset, -1, new String[] {});
        assertEquals(1, proposals.length);
        PyLinkedModeCompletionProposal p = (PyLinkedModeCompletionProposal) proposals[0];
        p.fLen = 1;
        Document d = new Document(s);
        p.applyOnDoc(offset, true, d, "meth".length(), '\n');
        //System.out.println(d.get());
        assertEquals(s +
                "d1", d.get());
        assertEquals(new Point(d.getLength(), 0), p.getSelection(d));
    }

    public void testApply4() throws Exception {
        String s = "class XX:\n" +
                "    def method1(self, a, b):\n" +
                "        return 1\n" +
                "    def foo(self):\n"
                +
                "        self.metho";

        int offset = s.length() - 1;
        ICompletionProposal[] proposals = requestCompl(s, offset, -1, new String[] {});
        assertEquals(1, proposals.length);
        PyLinkedModeCompletionProposal p = (PyLinkedModeCompletionProposal) proposals[0];
        p.fLen = 1;

        Document d = new Document(s);
        p.applyOnDoc(offset, true, d, "meth".length(), '(');
        //System.out.println(d.get());
        assertEquals(s +
                "d1", d.get());
        assertEquals(new Point(d.getLength(), 0), p.getSelection(d));
    }

    public void testApply5() throws Exception {
        String s = "class XX:\n" +
                "    def method1(self, a, b):\n" +
                "        return 1\n" +
                "    def foo(self):\n"
                +
                "        self.meth";

        int offset = s.length();
        ICompletionProposal[] proposals = requestCompl(s, offset, -1, new String[] {});
        assertEquals(1, proposals.length);
        PyLinkedModeCompletionProposal p = (PyLinkedModeCompletionProposal) proposals[0];

        Document d = new Document(s);
        p.applyOnDoc(offset, false, d, 4, '(');
        //System.out.println(d.get());
        assertEquals(s +
                "od1()", d.get());
        assertEquals(new Point(d.getLength() - 1, 0), p.getSelection(d));
    }

    public void testApply6() throws Exception {
        String s = "class XX:\n" +
                "    def method1(self, a, b):\n" +
                "        return 1\n" +
                "    def foo(self):\n"
                +
                "        self.meth";

        int offset = s.length();
        ICompletionProposal[] proposals = requestCompl(s, offset, -1, new String[] {});
        assertEquals(1, proposals.length);
        PyLinkedModeCompletionProposal p = (PyLinkedModeCompletionProposal) proposals[0];

        Document d = new Document(s);
        p.applyOnDoc(offset, false, d, 4, ')');
        //System.out.println(d.get());
        assertEquals(s +
                "od1(a, b)", d.get());
        assertEquals(new Point(d.getLength() - "a, b)".length(), 0), p.getSelection(d));
    }

}
