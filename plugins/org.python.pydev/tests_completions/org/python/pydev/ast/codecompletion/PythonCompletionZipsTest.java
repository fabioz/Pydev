/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.shared_core.callbacks.ICallback;

public class PythonCompletionZipsTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        try {
            PythonCompletionZipsTest builtins = new PythonCompletionZipsTest();
            builtins.setUp();
            builtins.testZip();
            builtins.tearDown();

            junit.textui.TestRunner.run(PythonCompletionZipsTest.class);

        } catch (Exception e) {
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

        //add the zip and the egg files here...
        this.restorePythonPath(
                TestDependent.getCompletePythonLib(true, isPython3Test()) + "|" + TestDependent.TEST_PYSRC_TESTING_LOC
                        + "myzipmodule.zip" + "|" + TestDependent.TEST_PYSRC_TESTING_LOC + "myeggmodule.egg",
                false);
        codeCompletion = new PyCodeCompletion();
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            @Override
            public Object call(CompletionRecursionException e) {
                throw new RuntimeException(
                        "Recursion error:" + org.python.pydev.shared_core.log.Log.getExceptionStr(e));
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

    public void testZip() throws Exception {
        String s = "import myzipmodule\n" +
                "myzipmodule.";

        requestCompl(s, s.length(), -1, new String[] { "MyZipClass" });
    }

    public void testEgg() throws Exception {
        String s = "import myeggmodule\n" +
                "myeggmodule.";

        requestCompl(s, s.length(), -1, new String[] { "MyEggClass" });
    }

}
