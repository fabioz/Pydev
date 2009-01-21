/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.core;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for org.python.pydev.refactoring.tests.core");
        // $JUnit-BEGIN$
        suite.addTestSuite(LexerTestCase.class);
        // $JUnit-END$
        return suite;
    }

}
