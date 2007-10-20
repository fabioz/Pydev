/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.adapter;

import java.io.File;

import junit.framework.Test;

import org.python.pydev.refactoring.tests.core.AbstractIOTestSuite;
import org.python.pydev.refactoring.tests.core.IInputOutputTestCase;

public class ClassDefAdapterTestSuite extends AbstractIOTestSuite {

	public static Test suite() {
		TESTDIR = "tests" + File.separator + "python" + File.separator + "adapter" + File.separator + "classdef";
		ClassDefAdapterTestSuite testSuite = new ClassDefAdapterTestSuite();

		testSuite.createTests();
        testSuite.addTest(new HierarchyTestCase("testHierarchyWithBuiltins"));

		return testSuite;
	}

	@Override
	protected IInputOutputTestCase createTestCase(String testCaseName) {
		return new ClassDefAdapterTestCase(testCaseName);
	}
}
