/*
 * Created on Sep 1, 2006
 * @author Fabio
 */
package org.python.pydev.parser;

import org.python.pydev.parser.jython.SimpleNode;

/**
 * Test for parsing python 2.5
 * @author Fabio
 */
public class PyParser25Test extends PyParserTestBase{

    public static void main(String[] args) {
        try {
            PyParser25Test test = new PyParser25Test();
            test.setUp();
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
        String str = "a = 1 if True else 2\n";
        SimpleNode node = parseLegalDocStr(str);
    }
}
