/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.coderefactoring.extractmethod;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class ExtractMethodTestSuite extends AbstractIOTestSuite {

    public ExtractMethodTestSuite(String name) {
        super(name);
    }

    public static Test suite() {
        String testdir = "tests" + I + "python" + I + "coderefactoring" + I + "extractmethod";

        ExtractMethodTestSuite tests = new ExtractMethodTestSuite("Extract Method");
        tests.createTests(testdir);

        return tests;
    }

    @Override
    protected IInputOutputTestCase createTestCase(String testCaseName) {
        return new ExtractMethodTestCase(testCaseName);
    }

}
