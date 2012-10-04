/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.adapter;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class FunctionDefAdapterTestSuite extends AbstractIOTestSuite {

    public FunctionDefAdapterTestSuite(String name) {
        super(name);
    }

    public static Test suite() {
        String testdir = "tests" + File.separator + "python" + File.separator + "adapter" + File.separator
                + "functiondef";
        FunctionDefAdapterTestSuite testSuite = new FunctionDefAdapterTestSuite("FunctionDef");

        testSuite.createTests(testdir);
        testSuite.addTest(new TestSuite(FunctionDefAdapterTestCase2.class));
        return testSuite;
    }

    @Override
    protected IInputOutputTestCase createTestCase(String testCaseName) {
        return new FunctionDefAdapterTestCase(testCaseName);
    }
}
