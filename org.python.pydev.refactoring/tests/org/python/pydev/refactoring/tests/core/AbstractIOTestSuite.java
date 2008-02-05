/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.core;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

import junit.framework.TestSuite;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public abstract class AbstractIOTestSuite extends TestSuite {

	protected static String TESTDIR = "tests" + File.separator + "python" + File.separator + "rewriter";

	// can be used to choose which test we want to run
	public static String FILE_FILTER = "^test.+\\.py$";
	static{
//	    FILE_FILTER = "testExtractMethod12.py";
//	    FILE_FILTER = "testBoolOp.py";
//	    FILE_FILTER = "testDoubleAssign.py";
//	    FILE_FILTER = "testBaseClass.py";
	}

	protected void createTests() {
		File[] testFiles = getTestFiles(System.getProperty("testDir", TESTDIR));

		if (testFiles == null)
			return;

		for (File testFile : testFiles) {
			this.addTest(createTest(testFile));
		}
	}

	private IInputOutputTestCase createTest(File file) {
		String filename = file.getName();
		String testCaseName = filename.substring(0, filename.length() - 3);

		IInputOutputTestCase testCase = createTestCase(testCaseName);

		IOTestCaseLexer lexer;
		try {
			lexer = new IOTestCaseLexer(new FileReader(file));
			lexer.scan();
			testCase.setFile(file);
			testCase.setSource(lexer.getSource());
			testCase.setResult(lexer.getResult());
			testCase.setConfig(lexer.getConfig());
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return testCase;
	}

	protected abstract IInputOutputTestCase createTestCase(String testCaseName);

	private File[] getTestFiles(String path) {
		File dir = new File(path);
		File[] testFiles = dir.listFiles(new TestFilenameFilter());

		return testFiles;
	}

	protected class TestFilenameFilter implements FilenameFilter {

		public boolean accept(File dir, String name) {
			return name.matches(System.getProperty("filter", FILE_FILTER));
		}

	}

}
