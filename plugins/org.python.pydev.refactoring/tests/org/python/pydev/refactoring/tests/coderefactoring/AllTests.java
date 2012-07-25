/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.coderefactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.refactoring.tests.coderefactoring.extractlocal.ExtractLocalTestSuite;
import org.python.pydev.refactoring.tests.coderefactoring.extractmethod.ExtractMethodTestSuite;
import org.python.pydev.refactoring.tests.coderefactoring.inlinelocal.InlineLocalTestSuite;

public final class AllTests {
    /* Hide Constructor */
    private AllTests() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Coderefactoring Tests");
        // $JUnit-BEGIN$
        suite.addTest(ExtractLocalTestSuite.suite());
        suite.addTest(ExtractMethodTestSuite.suite());
        suite.addTest(InlineLocalTestSuite.suite());
        // $JUnit-END$
        return suite;
    }

}
