package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;

public class PyParser312Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser312Test test = new PyParser312Test();
            test.setUp();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser312Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_12);
    }

    public void testFStrings() {
        String s = "version = {\"major\": 3, \"minor\": 12}\n"
                + "f'Python {version['major']}.{version['minor']}'\n"
                + "";

        // TODO: Support this!
        // parseLegalDocStr(s);

    }

    public void testTypeVarSyntaxClass() {
        String s = """
                class myclass[T, Y:str]:
                    pass
                """;
        parseLegalDocStr(s);
    }

    public void testTypeVarSyntax() {
        String s = """
                def f312[T, Y:str](e: T) -> None: ...
                """;
        parseLegalDocStr(s);
    }

    public void testTypeVarSyntax2() {
        String s = """
                def f312[*X, **Y](e: T) -> None: ...
                """;
        parseLegalDocStr(s);
    }

}
