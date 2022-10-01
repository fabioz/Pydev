package org.python.pydev.parser.prettyprinter;

import org.python.pydev.core.IGrammarVersionProvider;

public class PrettyPrinter311Test extends AbstractPrettyPrinterTestBase {

    public static void main(String[] args) {
        try {
            DEBUG = true;
            PrettyPrinter311Test test = new PrettyPrinter311Test();
            test.setUp();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PrettyPrinter311Test.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_11);
    }

    public void testExceptionGroup() {
        String s = "try:\n"
                + "    pass\n"
                + "except* TypeError:\n"
                + "    pass\n"
                + "";
        checkPrettyPrintEqual(s);
    }
}