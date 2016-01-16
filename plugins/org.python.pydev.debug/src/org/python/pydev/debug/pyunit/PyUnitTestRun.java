/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

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

    public String getExportToClipboard() {
        ArrayList<PyUnitTestResult> lst;
        synchronized (resultsLock) {
            lst = new ArrayList<>(results);
        }
        try {
            DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder icBuilder = icFactory.newDocumentBuilder();
            Document document = icBuilder.newDocument();
            ProcessingInstruction version = document.createProcessingInstruction("pydev-testrun", "version=\"1.0\""); //$NON-NLS-1$ //$NON-NLS-2$
            document.appendChild(version);

            Element root = document.createElement("pydev-testsuite");
            document.appendChild(root);

            Element summary = document.createElement("summary");
            summary.setAttribute("name", name);
            summary.setAttribute("errors", String.valueOf(this.getNumberOfErrors()));
            summary.setAttribute("failures", String.valueOf(this.getNumberOfFailures()));
            summary.setAttribute("tests", String.valueOf(this.getTotalNumberOfRuns()));
            summary.setAttribute("finished", String.valueOf(this.getFinished()));
            summary.setAttribute("total_time", String.valueOf(this.getTotalTime()));
            root.appendChild(summary);

            for (PyUnitTestResult pyUnitTestResult : lst) {
                Element test = document.createElement("test");
                test.setAttribute("status", pyUnitTestResult.status);
                test.setAttribute("location", pyUnitTestResult.location);
                test.setAttribute("test", pyUnitTestResult.test);
                test.setAttribute("time", pyUnitTestResult.time);

                Element stdout = document.createElement("stdout");
                test.appendChild(stdout);
                stdout.appendChild(document.createCDATASection(pyUnitTestResult.capturedOutput));

                Element stderr = document.createElement("stderr");
                test.appendChild(stderr);
                stderr.appendChild(document.createCDATASection(pyUnitTestResult.errorContents));
                root.appendChild(test);
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
            return new String(s.toByteArray(), "utf-8");
        } catch (Exception e) {
            Log.log(e);
        }
        return "";

    }

}
