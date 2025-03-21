package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.analysis.messages.IMessage;
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

    public void testTypeAlias() {
        doc = new Document("""
                type Point[T, Y] = tuple[T, Y]

                def method(arg: Point[int, int]):
                    print(type(arg))
                """);
        checkNoError();
    }

    public void testTypeAlias2() {
        doc = new Document("""
                from typing import cast
                class C:
                    def foo[T](self, x: T | None) -> T:
                        return cast(T, x)
                """);
        checkNoError();
    }

    public void testTypeAlias3() {
        doc = new Document("""
                from typing import cast
                class C:
                    def foo[T](self, x: T = T): # The T as the default value is wrong, T is not defined!
                        return cast(T, x)
                """);
        IMessage[] messages = checkError("Undefined variable: T\n");
        assertEquals(1, messages.length);
        assertEquals(3, messages[0].getStartLine(doc));
        assertEquals(29, messages[0].getStartCol(doc));
    }

    public void testTypeAlias4() {
        doc = new Document("""
                def foo[T](*, x: T) -> str:
                    return str(x)
                """);
        checkNoError();
    }

    public void testTypeAlias5() {
        doc = new Document("""
                class C[T]:
                    def foo(self, *, x: T) -> str:
                        return str(x)
                    def bar[V](self, *, y: V) -> str:
                        return str(y)
                        """);
        checkNoError();
    }

    public void testTypeAlias6() {
        doc = new Document("""
                def foo[T](*x: T) -> str:
                    print(T)
                    return str(x)
                            """);
        checkNoError();
    }

    public void testTypeAlias7() {
        doc = new Document("""
                def foo[T](**x: T) -> str:
                    print(T)
                    return str(x)
                            """);
        checkNoError();
    }

    public void testTypeAlias8() {
        doc = new Document("""
                def foo(**x: T) -> str:
                    return str(x)
                            """);
        IMessage[] messages = checkError("Undefined variable: T\n");
        assertEquals(1, messages.length);
        assertEquals(1, messages[0].getStartLine(doc));
        assertEquals(14, messages[0].getStartCol(doc));
    }

    public void testImportTypeAlias() {
        doc = new Document("from extendable.typecheck.typemod import FooType\n" +
                "print(FooType)\n");

        checkNoError();
    }

}
