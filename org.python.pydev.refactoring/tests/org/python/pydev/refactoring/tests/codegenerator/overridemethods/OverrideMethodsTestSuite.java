package org.python.pydev.refactoring.tests.codegenerator.overridemethods;

import java.io.File;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class OverrideMethodsTestSuite extends AbstractIOTestSuite {

	public static Test suite() {
		TESTDIR = "tests" + File.separator + "python" + File.separator + "codegenerator" + File.separator + "overridemethods";
		OverrideMethodsTestSuite testSuite = new OverrideMethodsTestSuite();

		testSuite.createTests();

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new OverrideMethodsTestCase(testCaseName);
	}
}
