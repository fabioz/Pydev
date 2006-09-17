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
            test.testNewRelativeImport();
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
    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }
    
    public void testNewRelativeImport2() throws Exception {
        //considering we're at: testlib.unittest.testcase
        String doc = "from . import ";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/pysrc/testlib/unittest/testcase.py");
        requestCompl(file,doc, doc.length(), -1, PythonCompletionWithoutBuiltinsTest.getTestLibUnittestTokens());
    }
    
    public void testNewRelativeImport() throws Exception {
        //considering we're at: testlib.unittest.testcase
        String doc = "from .. import ";
        File file = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests/pysrc/testlib/unittest/testcase.py");
        requestCompl(file,doc, doc.length(), -1, new String[]{"__init__","unittest"});
    }

}
