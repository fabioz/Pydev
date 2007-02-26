package org.python.pydev.refactoring.tests.coderefactoring.extractmethod;

import java.io.File;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class ExtractMethodTestSuite extends AbstractIOTestSuite {

	public static Test suite() {
		TESTDIR = "tests" + File.separator + "python" + File.separator
				+ "coderefactoring" + File.separator + "extractmethod";
		ExtractMethodTestSuite testSuite = new ExtractMethodTestSuite();

		testSuite.createTests();

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new ExtractMethodTestCase(testCaseName);
	}
}
