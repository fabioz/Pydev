package org.python.pydev.refactoring.tests.ast;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.python.pydev.refactoring.tests.ast.factory.PyAstFactoryTest;

public class AllTests {

    public static Test suite() {
        return new TestSuite(PyAstFactoryTest.class);
    }

}
