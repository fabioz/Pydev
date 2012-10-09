/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.visitors;

import junit.framework.Test;
import junit.framework.TestSuite;

public final class AllTests {
    private AllTests() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Visitors tests");
        // $JUnit-BEGIN$
        suite.addTest(AttributeVisitorTestSuite.suite());
        suite.addTest(ClassVisitorTestSuite.suite());
        suite.addTest(PropertyVisitorTestSuite.suite());
        suite.addTest(ScopeVarAssignVisitorTestSuite.suite());
        suite.addTest(ScopeVarVisitorTestSuite.suite());
        suite.addTest(SelectionExtensionTestSuite.suite());

        // $JUnit-END$
        return suite;
    }

}
