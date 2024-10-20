package org.python.pydev.parser.prettyprinter;

import org.python.pydev.core.IGrammarVersionProvider;

public class PrettyPrinter312Test extends AbstractPrettyPrinterTestBase {

    public static void main(String[] args) {
        try {
            DEBUG = true;
            PrettyPrinter312Test test = new PrettyPrinter312Test();
            test.setUp();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PrettyPrinter312Test.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_12);
    }

    public void testTypeVar() {
        String s = """
                def f312[T,Y:str](argument:T)->None:
                    pass
                """;
        checkPrettyPrintEqual(s);
    }

    public void testTypeVarInClass() {
        String s = """
                class f312[T,Y:str]:
                    pass
                """;
        checkPrettyPrintEqual(s);
    }

    public void testTypeAlias() {
        String s = """
                type Point[X,Y]=tuple[X,Y]
                """;
        checkPrettyPrintEqual(s);
    }
}