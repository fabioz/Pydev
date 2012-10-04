/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.rewriter;

import java.io.File;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

import junit.framework.Test;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public class RewriterTestSuite extends AbstractIOTestSuite {

    public RewriterTestSuite(String name) {
        super(name);
    }

    public static Test suite() {
        String testdir = "tests" + File.separator + "python" + File.separator + "rewriter";

        RewriterTestSuite testSuite = new RewriterTestSuite("Rewriter");
        testSuite.createTests(testdir);

        return testSuite;

    }

    @Override
    protected IInputOutputTestCase createTestCase(String testCaseName) {
        return new RewriterTestCase(testCaseName);
    }

}
