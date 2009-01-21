/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.rewriter;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for org.python.pydev.refactoring.tests.rewriter");
        // $JUnit-BEGIN$
        suite.addTest(RewriterTestSuite.suite());
        // $JUnit-END$
        return suite;
    }

}
