/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.visitors;

import java.io.File;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class ScopeVarAssignVisitorTestSuite extends AbstractIOTestSuite {

    public static Test suite() {
        TESTDIR = "tests" + File.separator + "python" + File.separator + "visitor" + File.separator + "scopevarassign";
        ScopeVarAssignVisitorTestSuite testSuite = new ScopeVarAssignVisitorTestSuite();

        testSuite.createTests();

        return testSuite;
    }

    @Override
    protected IInputOutputTestCase createTestCase(String testCaseName) {
        return new ScopeVarAssignVisitorTestCase(testCaseName);
    }
}
