/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.codegenerator.generateproperties;

import java.io.File;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class GeneratePropertiesTestSuite extends AbstractIOTestSuite {

    public GeneratePropertiesTestSuite(String name) {
        super(name);
    }

    public static Test suite() {
        String testdir = "tests" + File.separator + "python" + File.separator + "codegenerator" + File.separator
                + "generateproperties";
        GeneratePropertiesTestSuite testSuite = new GeneratePropertiesTestSuite("Generate Properties");

        testSuite.createTests(testdir);

        return testSuite;
    }

    @Override
    protected IInputOutputTestCase createTestCase(String testCaseName) {
        return new GeneratePropertiesTestCase(testCaseName);
    }
}
