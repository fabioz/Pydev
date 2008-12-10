/*
 * Created on Feb 11, 2006
 */
package org.python.pydev.parser.prettyprinter;
import org.python.pydev.core.IGrammarVersionProvider;


public class PrettyPrinter30Test extends AbstractPrettyPrinterTestBase{

    public static void main(String[] args) {
        try {
            PrettyPrinter30Test test = new PrettyPrinter30Test();
            test.setUp();
            test.testMetaClass3();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PrettyPrinter30Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
    }

    public void testMetaClass() throws Exception {
        String s = "" +
        "class IOBase(metaclass=abc.ABCMeta):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    

    public void testMetaClass2() throws Exception {
        String s = "" +
        "class IOBase(object,*args,metaclass=abc.ABCMeta):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
    
    public void testMetaClass3() throws Exception {
        String s = "" +
        "class B(*[x for x in [object]]):\n" +
        "    pass\n" +
        "";
        checkPrettyPrintEqual(s);
    }
}
