package org.python.pydev.refactoring.tests.visitors;

import java.io.File;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public class ClassVisitorTestSuite extends AbstractIOTestSuite {

	public static Test suite() {
		TESTDIR = "tests" + File.separator + "python" + File.separator + "visitor" + File.separator + "classvisitor";
		ClassVisitorTestSuite testSuite = new ClassVisitorTestSuite();

		testSuite.createTests();

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new ClassVisitorTestCase(testCaseName);
	}
}
