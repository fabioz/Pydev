package com.python.pydev.runalltests2;

import org.python.pydev.editor.codecompletion.revisited.jython.JythonCompletionWithBuiltinsTestWorkbench;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllWorkbenchTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(AllTests.class.getName());
        suite.addTestSuite(JythonCompletionWithBuiltinsTestWorkbench.class);
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

}
