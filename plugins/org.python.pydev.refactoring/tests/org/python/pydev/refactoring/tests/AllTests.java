/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public final class AllTests {
    private AllTests() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("PEPTIC Unit tests");

        // $JUnit-BEGIN$
        suite.addTest(org.python.pydev.refactoring.tests.adapter.AllTests.suite());
        suite.addTest(org.python.pydev.refactoring.tests.ast.AllTests.suite());
        suite.addTest(org.python.pydev.refactoring.tests.codegenerator.AllTests.suite());
        suite.addTest(org.python.pydev.refactoring.tests.coderefactoring.AllTests.suite());
        suite.addTest(org.python.pydev.refactoring.tests.rewriter.AllTests.suite());
        suite.addTest(org.python.pydev.refactoring.tests.utils.AllTests.suite());
        suite.addTest(org.python.pydev.refactoring.tests.visitors.AllTests.suite());
        // $JUnit-END$
        return suite;
    }

}
