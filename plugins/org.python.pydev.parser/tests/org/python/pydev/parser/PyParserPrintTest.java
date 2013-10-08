/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 8, 2006
 */
package org.python.pydev.parser;

public class PyParserPrintTest extends PyParserTestBase {

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
                "    #comment1                  \n"
                +
                "    def met1(self, a):#comment2\n" +
                "        pass                   \n" +
                "#comment3";
        parseLegalDocStr(s);

    }
}
