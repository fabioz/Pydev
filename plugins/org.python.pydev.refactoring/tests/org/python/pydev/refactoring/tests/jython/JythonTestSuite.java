/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.jython;

import java.io.File;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

import junit.framework.Test;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public class JythonTestSuite extends AbstractIOTestSuite {

	public JythonTestSuite(String name) {
		super(name);
	}

	public static Test suite() {
		String testdir = "tests" + File.separator + "python" + File.separator + "rewriter";
		
		JythonTestSuite testSuite = new JythonTestSuite("Jython");

		testSuite.createTests(testdir);

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new JythonTestCase(testCaseName);
	}
}
