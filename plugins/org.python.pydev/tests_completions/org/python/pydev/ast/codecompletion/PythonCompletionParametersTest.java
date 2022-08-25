/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 17, 2006
 * @author Fabio
 */
package org.python.pydev.ast.codecompletion;

import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;

public class PythonCompletionParametersTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            PythonCompletionParametersTest test = new PythonCompletionParametersTest();
            test.setUp();
            test.testParameterCompletion3();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonCompletionParametersTest.class);
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
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    public void testParameterCompletion() throws Exception {
        String doc = "" +
                "def m1(foo):\n" +
                "   foo.bar = 2\n" +
                "   foo."; //<- bring tokens that are already defined in the local
        String[] toks = new String[] { "bar" };
        requestCompl(doc, doc.length(), toks.length, toks);
    }

    public void testParameterCompletion2() throws Exception {
        String doc = "" +
                "def m1(foo):\n" +
                "   foo.bar = 2\n" +
                "   foo.bar2 = 2\n" +
                "   foo."; //<- bring tokens that are already defined in the local
        String[] toks = new String[] { "bar", "bar2" };
        requestCompl(doc, doc.length(), toks.length, toks);
    }

    public void testParameterCompletion3() throws Exception {
        String doc = "" +
                "def m1(foo):\n" +
                "   foo.bar.x = 2\n" +
                "   foo.bar.y = 2\n" +
                "   foo.bar."; //<- bring tokens that are already defined in the local
        String[] toks = new String[] { "x", "y" };
        requestCompl(doc, doc.length(), toks.length, toks);
    }

}
