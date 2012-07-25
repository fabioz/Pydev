/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.adapter;

import junit.framework.Test;
import junit.framework.TestSuite;

public final class AllTests {
    private AllTests() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Adapter tests");
        // $JUnit-BEGIN$
        suite.addTest(ClassDefAdapterTestSuite.suite());
        suite.addTest(FunctionDefAdapterTestSuite.suite());
        suite.addTest(ModuleAdapterTestSuite.suite());
        // $JUnit-END$
        return suite;
    }

}
