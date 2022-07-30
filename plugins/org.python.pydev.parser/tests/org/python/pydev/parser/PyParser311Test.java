/**
 * Copyright (c) 2022 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;

public class PyParser311Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser311Test test = new PyParser311Test();
            test.setUp();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser311Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_11);
    }

    public void testMatchExceptionGroups() {
        String s = "\n"
                + "try:\n"
                + "    pass\n"
                + "except* TypeError as e:\n"
                + "    pass\n"
                + "\n"
                + "";
        parseLegalDocStr(s);
    }
}
