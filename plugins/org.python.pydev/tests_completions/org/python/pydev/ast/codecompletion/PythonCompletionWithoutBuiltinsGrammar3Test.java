/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.ast.codecompletion.PyCodeCompletion;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

/**
 * This tests the 'whole' code completion, passing through all modules.
 *
 * @author Fabio Zadrozny
 */
public class PythonCompletionWithoutBuiltinsGrammar3Test extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            //DEBUG_TESTS_BASE = true;
            PythonCompletionWithoutBuiltinsGrammar3Test test = new PythonCompletionWithoutBuiltinsGrammar3Test();
            test.setUp();
            test.testGrammar3AbsoluteAndRelativeImports();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonCompletionWithoutBuiltinsGrammar3Test.class);
        } catch (Throwable e) {
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
        this.restorePythonPath("", false);
        codeCompletion = new PyCodeCompletion();
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            @Override
            public Object call(CompletionRecursionException e) {
                throw new RuntimeException(
                        "Recursion error:" + org.python.pydev.shared_core.log.Log.getExceptionStr(e));
            }

        };
    }

    @Override
    protected void afterRestorSystemPythonPath(InterpreterInfo info) {
        //No checks: created it without a system pythonpath
    }

    @Override
    protected void checkSize() {
        //No checks: created it without a system pythonpath
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

    @Override
    protected PythonNature createNature() {
        return new PythonNature() {
            @Override
            public int getInterpreterType() throws CoreException {
                return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
            }

            @Override
            public int getGrammarVersion() {
                return IPythonNature.GRAMMAR_PYTHON_VERSION_3_6;
            }
        };
    }

    public void testGrammar3AbsoluteAndRelativeImports() throws Exception {
        String file = TestDependent.TEST_PYSRC_TESTING_LOC + "extendable/grammar3/sub1.py";
        String strDoc = "from relative import ";
        ICompletionProposalHandle[] codeCompletionProposals = requestCompl(new File(file), strDoc, strDoc.length(), -1,
                new String[] { "DTest" });
        assertNotContains("NotFound", codeCompletionProposals);
    }

    public void testDictAccess() throws Exception {
        String s = ""
                + "class A:\n"
                + "    def method(self):\n"
                + "        pass\n"
                + "class Starship:\n" +
                "    stats: Dict[A, A] = {}\n" +
                "    for key, val in stats.items():\n" +
                "        key.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("method()", prop.getDisplayString());
    }

    public void testListAccess() throws Exception {
        String s = ""
                + "class A:\n"
                + "    def method(self):\n"
                + "        pass\n"
                + "primes: List[A] = []\n"
                + "for p in primes:\n"
                + "    p.";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("method()", prop.getDisplayString());
    }

    public void testListAccess2() throws Exception {
        String s = ""
                + "class A:\n"
                + "    def method(self):\n"
                + "        pass\n"
                + "primes: List[A] = []\n"
                + "primes[0].";

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length(), -1, new String[] {});
        assertEquals(1, proposals.length);
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("method()", prop.getDisplayString());
    }
}
