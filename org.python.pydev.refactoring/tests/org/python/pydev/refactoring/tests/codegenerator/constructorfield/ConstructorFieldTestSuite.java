package org.python.pydev.refactoring.tests.codegenerator.constructorfield;

import java.io.File;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class ConstructorFieldTestSuite extends AbstractIOTestSuite {

	public static Test suite() {
		TESTDIR = "tests" + File.separator + "python" + File.separator
				+ "codegenerator" + File.separator + "constructorfield";
		ConstructorFieldTestSuite testSuite = new ConstructorFieldTestSuite();

		testSuite.createTests();

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new ConstructorFieldTestCase(testCaseName);
	}
}
