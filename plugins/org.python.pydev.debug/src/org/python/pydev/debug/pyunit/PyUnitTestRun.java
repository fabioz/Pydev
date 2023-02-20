/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.XMLUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PyUnitTestRun {

    private final ArrayList<PyUnitTestResult> results;
    private final Map<Tuple<String, String>, PyUnitTestStarted> testsRunning;

    private final Object resultsLock = new Object();
    private final Object testsRunningLock = new Object();

    public final String name;

    private static int currentRun = 0;
    private static Object lock = new Object();
    private int numberOfErrors;
    private int numberOfFailures;
    private String totalNumberOfRuns = "0";
    private boolean finished;
    private IPyUnitLaunch pyUnitLaunch;
    private int nextIndex = 0;
    private String totalTime; //null while not set.

    /**
     * Helper to know whether we've already saved this PyUnitTestRun to the disk to be restored later.
     */
    public Integer savedDiskIndex;

    public PyUnitTestRun(IPyUnitLaunch server) {
        synchronized (lock) {
            this.name = "Test Run: " + currentRun;
            currentRun += 1;
        }
        this.pyUnitLaunch = server;
        this.results = new ArrayList<PyUnitTestResult>();
        this.testsRunning = new LinkedHashMap<Tuple<String, String>, PyUnitTestStarted>();
    }

    public Collection<PyUnitTestStarted> getTestsRunning() {
        synchronized (testsRunningLock) {
            return new ArrayList<PyUnitTestStarted>(testsRunning.values());
        }
    }

    public void setTotalNumberOfRuns(String totalNumberOfRuns) {
        this.totalNumberOfRuns = totalNumberOfRuns;
    }

    public synchronized void addResult(PyUnitTestResult result) {
        if (result.status.equals("fail")) {
            numberOfFailures += 1;

        } else if (result.status.equals("error")) {
            numberOfErrors += 1;

        } else if (result.isOk() || result.isSkip()) {
            //ignore

        } else {
            Log.log("Unexpected status: " + result.status);
        }
        Tuple<String, String> key = new Tuple<String, String>(result.location, result.test);
        synchronized (testsRunningLock) {
            this.testsRunning.remove(key);//when a result is added, it should be removed from the tests running.
        }
        synchronized (resultsLock) {
            results.add(result);
        }
    }

    public void addStartTest(PyUnitTestStarted result) {
        Tuple<String, String> key = new Tuple<String, String>(result.location, result.test);
        synchronized (testsRunningLock) {
            this.testsRunning.put(key, result);
        }
    }

    /**
     * @return the same instance that's used internally to back up the results (use with care outside of this api
     * mostly for testing).
     */
    public List<PyUnitTestResult> getSharedResultsList() {
        return results;
    }

    public int getNumberOfRuns() {
        synchronized (resultsLock) {
            return results.size();
        }
    }

    public int getNumberOfErrors() {
        return numberOfErrors;
    }

    public int getNumberOfFailures() {
        return numberOfFailures;
    }

    public String getTotalNumberOfRuns() {
        return totalNumberOfRuns;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean getFinished() {
        return finished;
    }

    public void stop() {
        if (this.pyUnitLaunch != null) {
            IPyUnitLaunch s = this.pyUnitLaunch;
            if (s != null) {
                s.stop();
            }
        }

    }

    public void relaunch() {
        if (this.pyUnitLaunch != null) {
            IPyUnitLaunch s = this.pyUnitLaunch;
            if (s != null) {
                s.relaunch();
            }
        }
    }

    @Override
    public String toString() {
        return "PyUnitTestResult.\n" + "    Finished: " + this.finished + "\n" + "    Number of runs: "
                + this.results.size() + "" + "    Number of failures:" + this.numberOfFailures + "\n"
                + "    Number of errors: " + this.numberOfErrors + "\n" + "";
    }

    public void relaunchOnlyErrors() {
        IPyUnitLaunch s = this.pyUnitLaunch;
        if (s != null) {
            ArrayList<PyUnitTestResult> arrayList = new ArrayList<PyUnitTestResult>(this.results.size());
            for (PyUnitTestResult pyUnitTestResult : this.results) {
                if (!pyUnitTestResult.isOk() && !pyUnitTestResult.isSkip()) {
                    arrayList.add(pyUnitTestResult);
                }
            }
            s.relaunchTestResults(arrayList);
        }
    }

    /**
     * @param mode ILaunchManager.DEBUG_MODE or ILaunchManager.RUN_MODE
     */
    public void relaunch(List<PyUnitTestResult> resultsToRelaunch, String mode) {
        IPyUnitLaunch s = this.pyUnitLaunch;
        if (s != null) {
            s.relaunchTestResults(resultsToRelaunch, mode);
        } else {
            Log.log("Unable to relaunch (the original launch is no longer available or it was not properly restored).");
        }
    }

    public synchronized String getNextTestIndex() {
        return Integer.toString(++nextIndex);
    }

    public void setTotalTime(String totalTime) {
        this.totalTime = totalTime;
    }

    public String getTotalTime() {
        return this.totalTime;
    }

    public String getShortDescription() {
        FastStringBuffer buf = new FastStringBuffer(this.name, 20);
        buf.append(" (");
        buf.append("Tests: ");
        buf.append(this.getTotalNumberOfRuns());
        if (this.getNumberOfErrors() > 0) {
            buf.append(" Errors: ");
            buf.append(this.getNumberOfErrors());
        }
        if (this.getNumberOfFailures() > 0) {
            buf.append(" Failures: ");
            buf.append(this.getNumberOfFailures());
        }
        buf.append(")");
        return buf.toString();
    }

    public String toXML() {
        try {
            DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
            icFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            icFactory.setFeature("http://xml.org/sax/features/validation", false);
            icFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            icFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder icBuilder = icFactory.newDocumentBuilder();
            Document document = icBuilder.newDocument();
            ProcessingInstruction version = document.createProcessingInstruction("pydev-testrun", "version=\"1.0\""); //$NON-NLS-1$ //$NON-NLS-2$
            document.appendChild(version);

            PyUnitTestRun pyUnitTestRun = this;

            Element root = document.createElement("pydev-testsuite");
            document.appendChild(root);

            Element summary = document.createElement("summary");
            summary.setAttribute("name", name);
            summary.setAttribute("errors", String.valueOf(pyUnitTestRun.getNumberOfErrors()));
            summary.setAttribute("failures", String.valueOf(pyUnitTestRun.getNumberOfFailures()));
            summary.setAttribute("tests", String.valueOf(pyUnitTestRun.getTotalNumberOfRuns()));
            summary.setAttribute("finished", String.valueOf(pyUnitTestRun.getFinished()));
            summary.setAttribute("total_time", String.valueOf(pyUnitTestRun.getTotalTime()));
            root.appendChild(summary);

            for (PyUnitTestResult pyUnitTestResult : pyUnitTestRun.getResults()) {
                Element test = document.createElement("test");
                test.setAttribute("status", pyUnitTestResult.status);
                test.setAttribute("location", pyUnitTestResult.location);
                test.setAttribute("test", pyUnitTestResult.test);
                test.setAttribute("time", pyUnitTestResult.time);

                test.appendChild(XMLUtils.createBinaryElement(document, "stdout", pyUnitTestResult.capturedOutput));
                test.appendChild(XMLUtils.createBinaryElement(document, "stderr", pyUnitTestResult.errorContents));

                root.appendChild(test);
            }

            Element launchElement = document.createElement("launch");
            root.appendChild(launchElement);
            if (pyUnitLaunch != null) {
                pyUnitLaunch.fillXMLElement(document, launchElement);
            }

            ByteArrayOutputStream s = new ByteArrayOutputStream();

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$

            DOMSource source = new DOMSource(document);
            StreamResult outputTarget = new StreamResult(s);
            transformer.transform(source, outputTarget);
            return new String(s.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.log(e);
        }
        return "";

    }

    private List<PyUnitTestResult> getResults() {
        List<PyUnitTestResult> lst;
        synchronized (resultsLock) {
            lst = new ArrayList<>(results);
        }
        return lst;
    }

    public static class FillTestRunXmlHandler extends DefaultHandler {

        private final PyUnitTestRun testRun;
        private String fStatus;
        private String fLocation;
        private String fTest;
        private String fTime;
        private boolean fInStdout;
        private boolean fInStderr;
        private boolean fInLaunchMemento;
        private String fLaunchMode;

        // Things which may have binary contents.
        private String fErrorContents;
        private String fCapturedOutput;
        private String fLaunchMementoContents;

        private String fCapturedErrorEncoding;
        private String fCapturedOutputEncoding;
        private String fLaunchMementoEncoding;

        public FillTestRunXmlHandler(PyUnitTestRun testRun) {
            this.testRun = testRun;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            //System.out.println("chars: " + new String(ch, start, length));
            if (fInStdout) {
                try {
                    fCapturedOutput = decode(new String(ch, start, length), fCapturedOutputEncoding);
                } catch (Exception e) {
                    // Ignore (we could be reading just whitespaces...)
                }

            } else if (fInStderr) {
                try {
                    fErrorContents = decode(new String(ch, start, length), fCapturedErrorEncoding);
                } catch (Exception e) {
                    // Ignore (we could be reading just whitespaces...)
                }

            } else if (fInLaunchMemento) {
                try {
                    fLaunchMementoContents = decode(new String(ch, start, length), fLaunchMementoEncoding);
                } catch (Exception e) {
                    // Ignore (we could be reading just whitespaces...)
                }

            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            //int len = attributes.getLength();
            //System.out.println("start: " + localName + " - " + qName + " attrs:" + len);
            //for (int i = 0; i < len; i++) {
            //    System.out.println("attr: " + attributes.getQName(i) + " - " + attributes.getValue(i));
            //}
            if ("stdout".equals(qName)) {
                this.fCapturedOutputEncoding = attributes.getValue("encoding");
                this.fInStdout = true;

            } else if ("stderr".equals(qName)) {
                this.fCapturedErrorEncoding = attributes.getValue("encoding");
                this.fInStderr = true;

            } else if ("launch_memento".equals(qName)) {
                this.fLaunchMementoEncoding = attributes.getValue("encoding");
                this.fInLaunchMemento = true;

            } else if ("launch".equals(qName)) {
                this.fLaunchMode = attributes.getValue("mode");

            } else if ("test".equals(qName)) {
                fStatus = attributes.getValue("status");
                fLocation = attributes.getValue("location");
                fTest = attributes.getValue("test");
                fTime = attributes.getValue("time");

            } else if ("summary".equals(qName)) {
                // name: auto
                // errors: auto
                // failures: auto
                // tests: numberOfRuns
                String totalNumberOfRuns = attributes.getValue("tests");
                if (totalNumberOfRuns != null) {
                    testRun.setTotalNumberOfRuns(totalNumberOfRuns);
                }
                // finished: If null not finished
                testRun.setFinished("true".equals(attributes.getValue("finished")));
                String totalTime = attributes.getValue("total_time");
                if (totalTime != null) {
                    testRun.setTotalTime(totalTime);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            //System.out.println("end: " + localName + " - " + qName);
            if ("stdout".equals(qName)) {
                this.fInStdout = false;
            } else if ("stderr".equals(qName)) {
                this.fInStderr = false;
            } else if ("launch_memento".equals(qName)) {
                this.fInLaunchMemento = false;
            } else if ("launch".equals(qName)) {
                IPyUnitLaunch fromIO = PyUnitLaunch.fromIO(fLaunchMode, fLaunchMementoContents);
                testRun.pyUnitLaunch = fromIO;
                fLaunchMementoContents = null;
            } else if ("test".equals(qName)) {
                if (fStatus == null) {
                    fStatus = "<no status>";
                }
                if (fLocation == null) {
                    fLocation = "<no location>";
                }
                if (fTest == null) {
                    fTest = "<no test>";
                }
                if (fTime == null) {
                    fTime = "<no time>";
                }
                PyUnitTestResult result = new PyUnitTestResult(testRun, fStatus, fLocation, fTest,
                        fCapturedOutput == null ? "<Unable to load stdout>" : fCapturedOutput,
                        fErrorContents == null ? "<Unable to load stderr>" : fErrorContents, fTime);
                testRun.addResult(result);
                fCapturedOutput = null;
                fErrorContents = null;
            } else if ("summary".equals(qName)) {
                // we only have attributes, so, it's filled at open.
            }
        }

        private String decode(String captured, String encoding) throws Exception {
            return XMLUtils.decodeFromEncoding(captured, encoding);
        }

    }

    public static PyUnitTestRun fromXML(String exportToClipboard) throws Exception {
        PyUnitTestRun testRun = new PyUnitTestRun(null); // the actuall launch will be filled during the parsing.
        SAXParser parser = XMLUtils.getSAXParser();
        FillTestRunXmlHandler handler = new FillTestRunXmlHandler(testRun);
        parser.parse(new ByteArrayInputStream(exportToClipboard.getBytes(StandardCharsets.UTF_8)), handler);
        return testRun;
    }

    /*default*/ IPyUnitLaunch getPyUnitLaunch() {
        return this.pyUnitLaunch;
    }

}
