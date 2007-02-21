package org.python.pydev.editor.codecompletion;

import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

public class PythonCompletionStringsTest  extends CodeCompletionTestsBase {
    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            PythonCompletionStringsTest test = new PythonCompletionStringsTest();
            test.setUp();
            test.test1();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonCompletionStringsTest.class);
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
        codeCompletion = new PyStringCodeCompletion();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }
    

	public void test1() throws Exception {
        String doc = "" +
        "def m1(foo, bar):\n" +
        "   '''\n" +
        "   @param \n" +
        "   '''"; //<- bring tokens that are already defined in the local
        
        
        String[] toks = new String[]{"bar", "foo"};
        requestCompl(doc, doc.length()-"\n   '''".length(), toks.length, toks); //request right after the params
	}
}
