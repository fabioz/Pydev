package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ParseException;

public class OccurrencesAnalyzerPy36Test extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerPy36Test analyzer2 = new OccurrencesAnalyzerPy36Test();
            analyzer2.setUp();
            analyzer2.testNoDuplicateOnTypingOverride();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(OccurrencesAnalyzerPy36Test.class);
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

    public void testFstringVarNotDeclared() throws Exception {
        ParseException.verboseExceptions = false;
        doc = new Document("f'{{{test}}}'");
        IMessage[] messages = checkError("Undefined variable: test");
        assertEquals(1, messages.length);
        assertEquals(1, messages[0].getStartLine(doc));
        assertEquals(1, messages[0].getEndLine(doc));
        assertEquals(6, messages[0].getStartCol(doc));
        assertEquals(10, messages[0].getEndCol(doc));
    }

    public void testFstringVarNotDeclared2() throws Exception {
        ParseException.verboseExceptions = false;
        doc = new Document("f'{{ {test}}}'");
        IMessage[] messages = checkError("Undefined variable: test");
        assertEquals(1, messages.length);
        assertEquals(1, messages[0].getStartLine(doc));
        assertEquals(1, messages[0].getEndLine(doc));
        assertEquals(7, messages[0].getStartCol(doc));
        assertEquals(11, messages[0].getEndCol(doc));
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

    public void testEmptyFString() throws Exception {
        doc = new Document("def fn():\n" +
                "    if True:\n" +
                "        return f\"\"\n" +
                "" +
                "");
        checkNoError();
    }

    public void testFStringOk() throws Exception {
        doc = new Document("def fn():\n" +
                "    f'{{message}}'\n" +
                "" +
                "");
        checkNoError();
    }

    public void testFStringOk2() throws Exception {
        doc = new Document("def fn():\n" +
                "    LOGGERNAME_LENGTH = 17\n" +
                "    f'{{name:{LOGGERNAME_LENGTH}.{LOGGERNAME_LENGTH}s}} {{message}}'\n" +
                "" +
                "");
        checkNoError();
    }

    public void testFStringOk3() throws Exception {
        doc = new Document("def fn():\n" +
                "    var = 'element'\n" +
                "    width = 11\n" +
                "    print(f'{var:>{width}}')" +
                "");
        checkNoError();
    }

    public void testFStringNotOk3() throws Exception {
        doc = new Document("def fn():\n" +
                "    width = 11\n" +
                "    print(f'{var:>{width}}')" +
                "");
        checkError("Undefined variable: var");
    }

    public void testFStringNotOk3a() throws Exception {
        doc = new Document("def fn():\n" +
                "    var = 11\n" +
                "    print(f'{var:>{width}}')" +
                "");
        checkError("Undefined variable: width");
    }

    public void testFStringErr() throws Exception {
        doc = new Document(
                "def method():\n" +
                        "    xxxx = 'foo'\n" +
                        "    y = f'{{x}} is {x}'" +
                        "");
        checkError(
                "Undefined variable: x",
                "Unused variable: xxxx",
                "Unused variable: y");
    }

    public void testFStringNoErr() throws Exception {
        doc = new Document(
                "def method():\n" +
                        "    xxxx = 'foo'\n" +
                        "    y = f'{{x}} is {xxxx}'" +
                        "");
        checkError("Unused variable: y");
    }

    public void testFStringNoErr2() throws Exception {
        doc = new Document("" +
                "def method():\n" +
                "    val = 10\n" +
                "    width = 10\n" +
                "    precision = 10\n" +
                "    f'{val:{width}.{precision}f}'" +
                "");
        checkNoError();
    }

    public void testFStringErr2() throws Exception {
        doc = new Document("" +
                "def method():\n" +
                "    width = 10\n" +
                "    precision = 10\n" +
                "    f'{val:{width}.{precision}f}'" +
                "");
        checkError("Undefined variable: val");
    }

    public void testFStringNoErr3() throws Exception {
        doc = new Document("" +
                "def method():\n" +
                "    a = 10\n" +
                "    b = 10\n" +
                "    call = 10\n" +
                "    f'{call(a,b)}'" +
                "");
        checkNoError();
    }

    public void testFStringErr5() throws Exception {
        doc = new Document("" +
                "def method():\n" +
                "    a = 10\n" +
                "    call = 10\n" +
                "    f'{call(a,b)}'" +
                "");
        checkError("Undefined variable: b");
    }

    public void testFStringErr6() throws Exception {
        doc = new Document("" +
                "def method():\n" +
                "    call = 10\n" +
                "    f'{call({a},b)}'" + // Actually creating a set(a)
                "");
        checkError(
                "Undefined variable: a",
                "Undefined variable: b");
    }

    public void testFStringErr7() throws Exception {
        doc = new Document(
                "def method():\n" +
                        "    f'{val:{call(b,c)}}'" +
                        "");
        checkError(
                "Undefined variable: val",
                "Undefined variable: call",
                "Undefined variable: b",
                "Undefined variable: c");
    }

    public void testFStringErr8() throws Exception {
        doc = new Document(
                "def method():\n" +
                        "    val=1\n" +
                        "    width=1\n" +
                        "    precision=1\n" +
                        "    x=1\n" +
                        "    f'{val:{width}.{precision}.{not_found}f}'" +
                        "");
        checkError("Unused variable: x", "Undefined variable: not_found");
    }

    public void testFStringErr9() throws Exception {
        doc = new Document(
                "def method():\n" +
                        "    d = {0:'zero'}\n" +
                        "    f'{d[y]}'" +
                        "");
        checkError("Undefined variable: y");
    }

    public void testFStringErr10() throws Exception {
        doc = new Document(
                "def method():\n" +
                        "    d = {0:'zero'}\n" +
                        "    f'''{d[\ny]}'''" +
                        "");
        checkError("Undefined variable: y");
    }

    public void testNoDuplicateOnTypingOverride() {
        doc = new Document(""
                + "import typing\n"
                + "\n"
                + "class A:\n" +
                "    @typing.overload\n" +
                "    def spam(self, n:int):\n" +
                "        pass\n" +
                "\n" +
                "    @typing.overload\n" +
                "    def spam(self, n:str):\n" +
                "        pass\n" +
                "\n" +
                "    def spam(self, n):\n" +
                "        pass\n" +
                "");
        analyzer = new OccurrencesAnalyzer();
        // Seems like we aren't getting it from the library (but that's ok, just check that
        // Duplicated signature: spam does not appear).
        checkError("Unresolved import: typing");
    }

}
