/**
 * Copyright (c) 2019 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;

/**
 * @author Fabio
 */
public class PyParserCythonTest extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParserCythonTest test = new PyParserCythonTest();
            test.setUp();
            test.testCythonParsing();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParserCythonTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCythonParsing() throws Exception {
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_CYTHON);
        String str = "" +
                "import os.print.os\n" +
                "print(os.print.os)\n" +
                "";
        parseLegalDocStr(str);
    }

}
