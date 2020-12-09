package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ParseException;

public class OccurrencesAnalyzerPy38Test extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerPy38Test analyzer2 = new OccurrencesAnalyzerPy38Test();
            analyzer2.setUp();
            analyzer2.testWalrusOperatorInIf2();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(OccurrencesAnalyzerPy38Test.class);
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
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_8;

        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        GRAMMAR_TO_USE_FOR_PARSING = initialGrammar;
        ParseException.verboseExceptions = true;
        super.tearDown();
    }

    public void testWalrusOperatorInIf() throws Exception {
        doc = new Document("" +
                ""
                + "def test():\n"
                + "    return 10\n"
                + "\n"
                + "if a := test() > 20:\n" +
                "    pass\n" +
                "");
        checkNoError();
    }

    public void testWalrusOperatorInIf2() throws Exception {
        doc = new Document("" +
                ""
                + "def test():\n"
                + "    return 10\n"
                + "\n"
                + "def foo():\n"
                + "    if a := test() > 20:\n" +
                "        pass\n" +
                "");
        checkNoError();
    }

    public void testWithBelowDefinedWithFuture() throws Exception {
        doc = new Document("" +
                "from __future__ import annotations\n" +
                "\n" +
                "class Foo:\n" +
                "    def __init__(self, bar: Bar):\n" +
                "        self.bar = bar\n" +
                "\n" +
                "class Bar:\n"
                + "    pass");
        checkNoError();
    }

    public void testWithBelowDefined() throws Exception {
        doc = new Document("" +
                "class Foo:\n" +
                "    def __init__(self, bar: Bar):\n" +
                "        self.bar = bar\n" +
                "\n" +
                "class Bar:\n"
                + "    pass");
        checkError("Undefined variable: Bar");
    }

    public void testClassWithReferenceToItself() throws Exception {
        doc = new Document("" +
                "def gen():\n" +
                "    class C:\n" +
                "        def m(self):\n" +
                "            return 42\n" +
                "        def mm(self):\n" +
                "            return C.m");
        checkNoError();
    }

}
