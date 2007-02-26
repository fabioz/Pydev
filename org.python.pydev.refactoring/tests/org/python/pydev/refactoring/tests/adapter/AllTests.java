package org.python.pydev.refactoring.tests.adapter;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for org.python.pydev.refactoring.tests.adapter");
		// $JUnit-BEGIN$
		suite.addTest(ClassDefAdapterTestSuite.suite());
		suite.addTest(FunctionDefAdapterTestSuite.suite());
		suite.addTest(ModuleAdapterTestSuite.suite());
		// $JUnit-END$
		return suite;
	}

}
