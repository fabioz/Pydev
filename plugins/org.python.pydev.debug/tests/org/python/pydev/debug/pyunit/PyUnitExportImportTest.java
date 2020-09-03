package org.python.pydev.debug.pyunit;

import java.util.List;

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

    public void testPyUnitImportExisting() throws Exception {
        String exportToClipboard = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<?pydev-testrun version=\"1.0\"?><pydev-testsuite>\n" +
                "<summary errors=\"0\" failures=\"0\" finished=\"true\" name=\"Test Run: 0\" tests=\"0\" total_time=\"100\"/>\n"
                +
                "<test location=\"location\" status=\"ok\" test=\"testName\" time=\"100\">\n" +
                "<stdout><![CDATA[stdout]]></stdout>\n" +
                "<stderr><![CDATA[stderr]]></stderr>\n" +
                "</test>\n" +
                "<launch/>\n" +
                "</pydev-testsuite>\n" +
                "";
        PyUnitTestRun testRun2 = PyUnitTestRun.fromXML(exportToClipboard);
        assertEquals("100", testRun2.getTotalTime());
        assertEquals(1, testRun2.getNumberOfRuns());
        assertEquals(0, testRun2.getNumberOfErrors());
    }

    public void testPyUnitExportImport2() throws Exception {
        PyUnitTestRun testRun = new PyUnitTestRun(null);
        String capturedOutput = "\2foo\27foo\28";
        PyUnitTestResult result = new PyUnitTestResult(testRun, "ok", "location", "testName", capturedOutput,
                "stderr",
                "100");
        testRun.addResult(result);
        testRun.setFinished(true);
        testRun.setTotalTime("100");

        String exportToClipboard = testRun.toXML();
        System.out.println(exportToClipboard);
        PyUnitTestRun testRun2 = PyUnitTestRun.fromXML(exportToClipboard);
        List<PyUnitTestResult> sharedResultsList = testRun2.getSharedResultsList();
        assertEquals(1, sharedResultsList.size());
        assertEquals(sharedResultsList.get(0).capturedOutput, capturedOutput);
        assertEquals("100", testRun2.getTotalTime());
        assertEquals(1, testRun2.getNumberOfRuns());
        assertEquals(0, testRun2.getNumberOfErrors());
    }
}
