package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ParseException;

import com.python.pydev.analysis.messages.IMessage;

public class OccurrencesAnalyzerPy36Test extends AnalysisTestsBase {

    private int initialGrammar;

    @Override
    public void setUp() throws Exception {
        initialGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_6;

        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        GRAMMAR_TO_USE_FOR_PARSING = initialGrammar;
        ParseException.verboseExceptions = true;
        super.tearDown();
    }

    public void testUsedVariable1() throws Exception {
        doc = new Document(""
                + "def bar(a):\n" +
                "    pass\n" +
                "\n" +
                "def foo():\n" +
                "    v = [1]\n" +
                "    bar(a=(*v, ))" +
                "");
        checkNoError();
    }

    public void testSyntaxErrorOnFStrings() throws Exception {
        doc = new Document("f'fstring{'" +
                "");
        IMessage[] messages = checkError("SyntaxError: Empty expression not allowed in f-string, Unbalanced '{'");
        assertEquals(1, messages.length);
        assertEquals(1, messages[0].getStartLine(doc));
        assertEquals(1, messages[0].getEndLine(doc));
        assertEquals(10, messages[0].getStartCol(doc));
        assertEquals(11, messages[0].getEndCol(doc));
    }

    public void testSyntaxErrorOnFStrings2() throws Exception {
        doc = new Document("\n\nf'fstring{'" +
                "");
        IMessage[] messages = checkError("SyntaxError: Empty expression not allowed in f-string, Unbalanced '{'");
        assertEquals(1, messages.length);
        assertEquals(3, messages[0].getStartLine(doc));
        assertEquals(3, messages[0].getEndLine(doc));
        assertEquals(10, messages[0].getStartCol(doc));
        assertEquals(11, messages[0].getEndCol(doc));
    }

    public void testSyntaxErrorOnFStrings3() throws Exception {
        doc = new Document("\n\nf''''\nfstring{'''" +
                "");
        IMessage[] messages = checkError("SyntaxError: Empty expression not allowed in f-string, Unbalanced '{'");
        assertEquals(1, messages.length);
        assertEquals(4, messages[0].getStartLine(doc));
        assertEquals(4, messages[0].getEndLine(doc));
        assertEquals(7, messages[0].getStartCol(doc));
        assertEquals(8, messages[0].getEndCol(doc));
    }

    public void testErrorOnFStringsExpression() throws Exception {
        ParseException.verboseExceptions = false;
        doc = new Document("f'fstring{i i i}'" +
                "");
        IMessage[] messages = checkError("SyntaxError: invalid syntax");
        assertEquals(1, messages.length);
        assertEquals(1, messages[0].getStartLine(doc));
        assertEquals(1, messages[0].getEndLine(doc));
        assertEquals(13, messages[0].getStartCol(doc));
        assertEquals(14, messages[0].getEndCol(doc));
    }

    public void testErrorOnFStringsExpression2() throws Exception {
        ParseException.verboseExceptions = false;
        doc = new Document("\n\nf'fstring{i i i}'" +
                "");
        IMessage[] messages = checkError("SyntaxError: invalid syntax");
        assertEquals(1, messages.length);
        assertEquals(3, messages[0].getStartLine(doc));
        assertEquals(3, messages[0].getEndLine(doc));
        assertEquals(13, messages[0].getStartCol(doc));
        assertEquals(14, messages[0].getEndCol(doc));
    }

    public void testErrorOnMultiLineFStringsExpression() throws Exception {
        ParseException.verboseExceptions = false;
        doc = new Document("f'''\nfstring{i i i}\n\n'''" +
                "");
        IMessage[] messages = checkError("SyntaxError: invalid syntax");
        assertEquals(1, messages.length);
        assertEquals(2, messages[0].getStartLine(doc));
        assertEquals(2, messages[0].getEndLine(doc));
        assertEquals(11, messages[0].getStartCol(doc));
        assertEquals(12, messages[0].getEndCol(doc));
    }

    public void testErrorOnMultiLineFStringsExpression2() throws Exception {
        ParseException.verboseExceptions = false;
        doc = new Document("\n\nf'''\nfstring{i i i}\n\n'''" +
                "");
        IMessage[] messages = checkError("SyntaxError: invalid syntax");
        assertEquals(1, messages.length);
        assertEquals(4, messages[0].getStartLine(doc));
        assertEquals(4, messages[0].getEndLine(doc));
        assertEquals(11, messages[0].getStartCol(doc));
        assertEquals(12, messages[0].getEndCol(doc));
    }

    public void testSemanticAnalysisOfFStrings() throws Exception {
        ParseException.verboseExceptions = false;
        doc = new Document("a = 10\nf'{a}'" +
                "");
        checkNoError();
    }

    public void testSemanticAnalysisOfFStrings2() throws Exception {
        ParseException.verboseExceptions = false;
        doc = new Document("a = 10\nf'{b}'" +
                "");
        IMessage[] messages = checkError("Undefined variable: b");
        assertEquals(1, messages.length);
        assertEquals(2, messages[0].getStartLine(doc));
        assertEquals(2, messages[0].getEndLine(doc));
        assertEquals(4, messages[0].getStartCol(doc));
        assertEquals(5, messages[0].getEndCol(doc));
    }

}
