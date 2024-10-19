package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ParseException;

public class OccurrencesAnalyzerPy312Test extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerPy312Test analyzer2 = new OccurrencesAnalyzerPy312Test();
            analyzer2.setUp();
            analyzer2.testTypeVarClassSimple();
            analyzer2.tearDown();
            System.out.println("finished");
            junit.textui.TestRunner.run(OccurrencesAnalyzerPy312Test.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private int initialGrammar;

    @Override
    public void setUp() throws Exception {
        initialGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_12;
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        GRAMMAR_TO_USE_FOR_PARSING = initialGrammar;
        ParseException.verboseExceptions = true;
        super.tearDown();
    }

    @Override
    protected boolean isPython3Test() {
        return true;
    }

    public void testTypeVarMethodSimple() {
        doc = new Document("""
                def method[T](argument:T)->None:
                    print(argument)
                """);
        checkNoError();
    }

    public void testTypeVarClassSimple() {
        doc = new Document("""
                class ClassA[T1, T2, T3](list[T1]):
                    def method1(self, a: T2) -> None:
                        ...

                    def method2(self) -> T3:
                        ...
                    """);
        checkNoError();
    }

}
