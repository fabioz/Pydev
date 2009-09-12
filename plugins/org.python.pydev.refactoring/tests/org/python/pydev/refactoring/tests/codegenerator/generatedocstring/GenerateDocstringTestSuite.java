/* 
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.tests.codegenerator.generatedocstring;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class GenerateDocstringTestSuite extends AbstractIOTestSuite {

	public GenerateDocstringTestSuite(String name) {
		super(name);
	}

	public static Test suite() {
		String testdir = "tests" + I + "python" + I + "codegenerator" + I + "generatedocstring";
		GenerateDocstringTestSuite testSuite = new GenerateDocstringTestSuite("Generate Docstrng");

		testSuite.createTests(testdir);

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new GenerateDocstringTestCase(testCaseName);
	}
}
