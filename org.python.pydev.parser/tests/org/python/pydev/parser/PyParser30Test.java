package org.python.pydev.parser;

import java.io.File;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;

public class PyParser30Test extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParser30Test test = new PyParser30Test();
            test.setUp();
            test.testNoAssignToFalse();
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
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_0);
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
    
    public void testWrongPrint() {
        String s = "" +
        "print 'error'\n" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testBytes() {
        String s = "" +
        "a = b'error'\n" +
        "";
        parseLegalDocStr(s);
    }
    
    public void testReprNotAccepted() {
        String s = "" +
        "`error`\n" +
        "";
        parseILegalDocStr(s);
    }
    
    
    public void testNoLessGreater() {
        String s = "a <> b" +
        "\n" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testNoAssignToFalse() {
        String s = "False = 1" +
        "\n" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testNoAssignToTrue() {
        String s = "True = 1" +
        "\n" +
        "";
        parseILegalDocStr(s);
    }
    
    public void testNoAssignToNone() {
        String s = "None = 1" +
        "\n" +
        "";
        parseILegalDocStr(s);
    }
    
//    public void testSetComprehension() {
//        String s = "" +
//        "namespace = {'a':1, 'b':2, 'c':1, 'd':1}\n" +
//        "abstracts = {name\n" +
//        "             for name, value in namespace.items()\n" +
//        "             if value==1}\n" +
//        "print(abstracts)\n" +
//        "\n" +
//        "";
//        parseLegalDocStr(s);
//    }
//    
    
//    public void testLib() throws Exception {
//        parseFilesInDir(new File(TestDependent.PYTHON_30_LIB), false);
//    }


}
