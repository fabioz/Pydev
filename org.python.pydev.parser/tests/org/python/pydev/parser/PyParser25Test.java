/*
 * Created on Sep 1, 2006
 * @author Fabio
 */
package org.python.pydev.parser;

import junit.framework.AssertionFailedError;

import org.python.pydev.core.IPythonNature;

/**
 * Test for parsing python 2.5
 * @author Fabio
 */
public class PyParser25Test extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParser25Test test = new PyParser25Test();
            test.setUp();
            test.testConditionalExp1();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser25Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PyParser.USE_FAST_STREAM = true;
    }
    
    /**
     * This test checks the new conditional expression.
     */
    public void testConditionalExp1(){
        defaultVersion = IPythonNature.GRAMMAR_PYTHON_VERSION_2_5;
        String str = "a = 1 if True else 2\n";
        parseLegalDocStr(str);
    }
    
    /**
     * This test checks that the old version still gives an error
     */
    public void testConditionalExp1err(){
        defaultVersion = IPythonNature.GRAMMAR_PYTHON_VERSION_2_4;
        String str = "a = 1 if True else 2\n";
        try {
            parseLegalDocStr(str);
            fail("This construct is not valid for version 2.4");
        } catch (AssertionFailedError e) {
        }
    }
}
