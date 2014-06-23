/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.structure.Tuple;

public class PyUnitTestRun {

    private final ArrayList<PyUnitTestResult> results;
    private final Map<Tuple<String, String>, PyUnitTestStarted> testsRunning;

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
            this.name = "Test Run:" + currentRun;
            currentRun += 1;
        }
        this.pyUnitLaunch = server;
        this.results = new ArrayList<PyUnitTestResult>();
        this.testsRunning = new LinkedHashMap<Tuple<String, String>, PyUnitTestStarted>();
    }

    public Collection<PyUnitTestStarted> getTestsRunning() {
        return testsRunning.values();
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
        this.testsRunning.remove(key);//when a result is added, it should be removed from the tests running.
        results.add(result);
    }

    public void addStartTest(PyUnitTestStarted result) {
        Tuple<String, String> key = new Tuple<String, String>(result.location, result.test);
        this.testsRunning.put(key, result);
    }

    /**
     * @return the same instance that's used internally to back up the results (use with care outside of this api
     * mostly for testing).
     */
    public List<PyUnitTestResult> getSharedResultsList() {
        return results;
    }

    public int getNumberOfRuns() {
        return results.size();
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

}
