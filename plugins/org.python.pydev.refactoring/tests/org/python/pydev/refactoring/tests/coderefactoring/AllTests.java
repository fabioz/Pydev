/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.coderefactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.refactoring.tests.coderefactoring.extractmethod.ExtractMethodTestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for org.python.pydev.refactoring.tests.coderefactoring");
        // $JUnit-BEGIN$
        suite.addTest(ExtractMethodTestSuite.suite());
        // $JUnit-END$
        return suite;
    }

}
