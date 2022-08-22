/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeCompletion2Test extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyCodeCompletion2Test.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(TestDependent.getCompletePythonLib(true, isPython3Test()) +
                "|" + TestDependent.PYTHON2_PIL_PACKAGES,
                false);
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            @Override
            public Object call(CompletionRecursionException e) {
                throw new RuntimeException(
                        "Recursion error:" + org.python.pydev.shared_core.log.Log.getExceptionStr(e));
            }

        };
    }

    public void testSelfOrClsCompletion() throws Exception {
        String s = "" +
                "class B:\n" +
                "    def m2(self):\n" +
                "        pass\n" +
                "\n" +
                "class A:\n"
                +
                "    m1 = B()\n" +
                "    def foo(self):\n" +
                "        self.m1." +
                "";

        SystemPythonNature nature = new SystemPythonNature(PyCodeCompletion2Test.nature.getRelatedInterpreterManager());
        PySelection ps = new PySelection(new Document(s), s.length() - 1);
        ICompletionState state = new CompletionState(ps.getStartLineIndex(), ps.getAbsoluteCursorOffset()
                - ps.getStartLine().getOffset(), null, nature, "");
        CompletionRequest request = new CompletionRequest(null, nature, ps.getDoc(), "self.m1",
                ps.getAbsoluteCursorOffset(), 0, new PyCodeCompletion(), "", false);
        TokensList selfCompletions = new TokensList();
        PyCodeCompletion.getSelfOrClsCompletions(request, selfCompletions, state, false, false, "self.m1");
        ArrayList<IToken> lst = new ArrayList<>();
        for (IterTokenEntry entry : selfCompletions) {
            IToken token = entry.getToken();
            if (token.getRepresentation().startsWith("_")) {
                continue;
            }
            lst.add(token);
        }
        assertEquals(1, lst.size());
        assertEquals("m2", lst.get(0).getRepresentation());

    }
}
