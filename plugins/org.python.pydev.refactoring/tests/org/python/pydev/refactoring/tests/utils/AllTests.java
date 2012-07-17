/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.tests.utils;

import junit.framework.Test;
import junit.framework.TestSuite;

public final class AllTests {
    private AllTests() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for org.python.pydev.refactoring.utils");
        //$JUnit-BEGIN$
        suite.addTestSuite(StringUtilsTest.class);
        suite.addTestSuite(TestUtilsTest.class);
        suite.addTestSuite(FileUtilsTest.class);
        //$JUnit-END$
        return suite;
    }

}
