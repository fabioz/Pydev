package org.python.pydev.debug.pyunit;

import junit.framework.TestCase;

public class PyUnitExportImportTest extends TestCase {

    public void testPyUnitExportImport() throws Exception {
        PyUnitTestRun testRun = new PyUnitTestRun(null);
        PyUnitTestResult result = new PyUnitTestResult(testRun, "ok", "location", "testName", "stdout", "stderr",
                "100");
        testRun.addResult(result);
        testRun.setFinished(true);
        testRun.setTotalTime("100");

        String exportToClipboard = testRun.toXML();
        PyUnitTestRun testRun2 = PyUnitTestRun.fromXML(exportToClipboard);
        assertEquals("100", testRun2.getTotalTime());
        assertEquals(1, testRun2.getNumberOfRuns());
        assertEquals(0, testRun2.getNumberOfErrors());
    }
}
