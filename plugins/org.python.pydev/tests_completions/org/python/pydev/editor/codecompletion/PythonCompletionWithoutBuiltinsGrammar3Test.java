/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

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
                throw new RuntimeException("Recursion error:" + Log.getExceptionStr(e));
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
                return IPythonNature.GRAMMAR_PYTHON_VERSION_3_0;
            }
        };
    }

    public void testGrammar3AbsoluteAndRelativeImports() throws Exception {
        String file = TestDependent.TEST_PYSRC_LOC + "extendable/grammar3/sub1.py";
        String strDoc = "from relative import ";
        ICompletionProposal[] codeCompletionProposals = requestCompl(new File(file), strDoc, strDoc.length(), -1,
                new String[] { "DTest" });
        assertNotContains("NotFound", codeCompletionProposals);
    }

}
