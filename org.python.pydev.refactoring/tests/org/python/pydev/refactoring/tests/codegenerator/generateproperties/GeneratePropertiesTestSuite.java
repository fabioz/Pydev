package org.python.pydev.refactoring.tests.codegenerator.generateproperties;

import java.io.File;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class GeneratePropertiesTestSuite extends AbstractIOTestSuite {

	public static Test suite() {
		TESTDIR = "tests" + File.separator + "python" + File.separator
				+ "codegenerator" + File.separator + "generateproperties";
		GeneratePropertiesTestSuite testSuite = new GeneratePropertiesTestSuite();

		testSuite.createTests();

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new GeneratePropertiesTestCase(testCaseName);
	}
}
