package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ParseException;

public class OccurrencesAnalyzerPy310Test extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerPy310Test analyzer2 = new OccurrencesAnalyzerPy310Test();
            analyzer2.setUp();
            analyzer2.testMatchStmtSimple();
            analyzer2.tearDown();
            System.out.println("finished");
            junit.textui.TestRunner.run(OccurrencesAnalyzerPy310Test.class);
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
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_10;
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        GRAMMAR_TO_USE_FOR_PARSING = initialGrammar;
        ParseException.verboseExceptions = true;
        super.tearDown();
    }

    public void testMatchStmtSimple() {
        doc = new Document(""
                + "command = 'foo'\n"
                + "action = 'bar'\n"
                + "obj = dict()\n"
                + "match command.split():\n"
                + "    case [action,obj]:\n"
                + "        pass\n");
        checkNoError();
    }

    public void testMatchStmtSimple2() {
        doc = new Document(""
                + "command = 'foo'\n"
                + "action = 'bar'\n"
                + "obj = dict()\n"
                + "match command.split():\n"
                + "    case (action,obj):\n"
                + "        pass\n");
        checkNoError();
    }

    public void testMatchAsSoftKeyword() {
        doc = new Document(""
                + "match = 10\n"
                + "print(match)");
        checkNoError();
    }

    public void testCaseAsSoftKeyword() {
        doc = new Document(""
                + "case = 10\n"
                + "print(case)");
        checkNoError();
    }

    public void testMatchAndCaseAsSoftKeyword() {
        doc = new Document(""
                + "match = 30\n"
                + "case = 10\n"
                + "result = match + case\n"
                + "print(result)");
        checkNoError();
    }
}
