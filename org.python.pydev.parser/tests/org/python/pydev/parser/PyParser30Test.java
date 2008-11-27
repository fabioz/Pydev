package org.python.pydev.parser;

public class PyParser30Test extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParser30Test test = new PyParser30Test();
            test.setUp();
            test.testTryExceptAs();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser30Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PyParser.USE_FAST_STREAM = true;
    }

    public void testTryExceptAs() {
        String s = "" +
        "try:\n" +
        "    print('10')\n" +
        "except RuntimeError as e:\n" +
        "    print('error')\n" +
        "";
        parseLegalDocStr(s);
    }


}
