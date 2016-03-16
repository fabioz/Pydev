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
package org.python.pydev.editor.codecompletion;

import java.io.File;

import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

public class PythonCompletion25Test extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            PythonCompletion25Test test = new PythonCompletion25Test();
            test.setUp();
            test.testNewRelativeFromOtherModule();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonCompletion25Test.class);
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

    //changed... returns all but testcase
    public String[] getTestLibUnittestTokens() {
        return new String[] { "__file__", "__name__", "__init__", "__path__", "__dict__", "anothertest", "AnotherTest",
                "GUITest", "guitestcase", "main", "relative", "t", "TestCase", "TestCaseAlias" };
    }

    public void testNewRelativeFromOtherModule() throws Exception {
        String doc = "" + "from .file1 import imp1\n" + "imp1.";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/pysrc/extendable/newimport/sub1/file2.py");
        String[] toks = new String[] { "__file__", "__name__", "__dict__", "Imp1" };
        requestCompl(file, doc, doc.length(), toks.length, toks);
    }

    public void testNewRelativeFromOtherModule2() throws Exception {
        String doc = "" + "from .. import imp1\n" + "imp1.";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/pysrc/extendable/newimport/sub1/file1.py");
        String[] toks = new String[] { "__file__", "__name__", "__dict__", "Imp1" };
        requestCompl(file, doc, doc.length(), toks.length, toks);
    }

    public void testNewRelativeImport2() throws Exception {
        //considering we're at: testlib.unittest.testcase
        String doc = "from . import ";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/pysrc/testlib/unittest/testcase.py");
        String[] toks = getTestLibUnittestTokens();
        requestCompl(file, doc, doc.length(), toks.length, toks);
    }

    public void testNewRelativeImport2a() throws Exception {
        //considering we're at: testlib.unittest.testcase
        String doc = "from ."; //just show the modules
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/pysrc/testlib/unittest/testcase.py");
        String[] toks = new String[] { "__init__", "anothertest", "guitestcase", "relative",
        //                "relative.testrelative",
        //                "relative.toimport",
        };
        requestCompl(file, doc, doc.length(), toks.length, toks);
    }

    public void testNewRelativeImport3() throws Exception {
        //considering we're at: testlib.unittest.testcase
        String doc = "from .anothertest import ";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/pysrc/testlib/unittest/testcase.py");
        String[] toks = new String[] { "__file__", "__name__", "__dict__", "t", "AnotherTest" };
        requestCompl(file, doc, doc.length(), toks.length, toks);
    }

    public void testNewRelativeImport3a() throws Exception {
        //considering we're at: testlib.unittest.testcase
        String doc = "from ..unittest.anothertest import ";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/pysrc/testlib/unittest/testcase.py");
        String[] toks = new String[] { "__file__", "__name__", "__dict__", "t", "AnotherTest" };
        requestCompl(file, doc, doc.length(), toks.length, toks);
    }

    public void testNewRelativeImportInvalid() throws Exception {
        //considering we're at: testlib.unittest.testcase
        String doc = "from ........... import ";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/pysrc/testlib/unittest/testcase.py");
        String[] toks = new String[] {};
        requestCompl(file, doc, doc.length(), toks.length, toks);
    }

    public void testNewRelativeImport() throws Exception {
        //considering we're at: testlib.unittest.testcase
        String doc = "from .. import ";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/pysrc/testlib/unittest/testcase.py");
        requestCompl(file, doc, doc.length(), -1, new String[] { "__init__", "unittest" });
    }

}
