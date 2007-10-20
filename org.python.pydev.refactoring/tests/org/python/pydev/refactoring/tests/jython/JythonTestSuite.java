/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.jython;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

import junit.framework.Test;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public class JythonTestSuite extends AbstractIOTestSuite {

	public static Test suite() {
		JythonTestSuite testSuite = new JythonTestSuite();

		testSuite.createTests();

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new JythonTestCase(testCaseName);
	}
}
