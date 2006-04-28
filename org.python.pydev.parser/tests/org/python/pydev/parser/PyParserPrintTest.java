/*
 * Created on Feb 8, 2006
 */
package org.python.pydev.parser;


public class PyParserPrintTest extends PyParserTestBase{

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            PyParserPrintTest test = new PyParserPrintTest();
            test.setUp();
//            test.testParser10();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParserPrintTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testComments1() {
        String s = "" +
        "#comment00\n" +
        "class Class1: #comment0        \n" +
        "    #comment1                  \n" +
        "    def met1(self, a):#comment2\n" +
        "        pass                   \n" +
        "#comment3";
        parseLegalDocStr(s);

    }
}
