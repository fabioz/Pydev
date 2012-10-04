/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.core;

import java.io.File;
import java.io.FilenameFilter;

import junit.framework.TestSuite;

/**
 * @author Dennis Hunziker, Ueli Kistler
 */
public abstract class AbstractIOTestSuite extends TestSuite {
    private static final int EXTENSION = 3;
    protected static final String I = File.separator;

    // can be used to choose which test we want to run
    public static String FILE_FILTER = "^test.+\\.py$";
    static {
        //	    FILE_FILTER = "^testExtractMethodImport3.py$";
    }

    public AbstractIOTestSuite(String name) {
        super(name);
    }

    protected void createTests(String testdir) {
        for (File testFile : getTestFiles(testdir)) {
            this.addTest(createTest(testFile));
        }
    }

    private IInputOutputTestCase createTest(File file) {
        String filename = file.getName();
        String testCaseName = filename.substring(0, filename.length() - EXTENSION);

        TestData data = new TestData(file);
        IInputOutputTestCase testCase = createTestCase(testCaseName);
        testCase.setData(data);

        return testCase;
    }

    private File[] getTestFiles(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            throw new RuntimeException("No such directory: " + dir.getAbsolutePath());
        }
        File[] testFiles = dir.listFiles(new TestFilenameFilter());

        if (testFiles == null) {
            throw new RuntimeException("No such directory or IO error while looking for files in " + path);
        }

        return testFiles;
    }

    private final class TestFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.matches(System.getProperty("filter", FILE_FILTER));
        }
    }

    protected abstract IInputOutputTestCase createTestCase(String testCaseName);
}
