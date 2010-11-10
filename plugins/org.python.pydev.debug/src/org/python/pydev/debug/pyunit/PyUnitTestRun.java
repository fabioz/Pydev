package org.python.pydev.debug.pyunit;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.plugin.PydevPlugin;

public class PyUnitTestRun {

    private final ArrayList<PyUnitTestResult> results;
    public final String name;

    private static int currentRun = 0;
    private static Object lock = new Object();
    private int numberOfErrors;
    private int numberOfFailures;
    private String totalNumberOfRuns="0";
    private boolean finished;
    private IPyUnitLaunch pyUnitLaunch;
    
    public PyUnitTestRun(IPyUnitLaunch server) {
        synchronized (lock) {
            this.name = "Test Run:"+currentRun;
            currentRun += 1;
        }
        this.pyUnitLaunch = server;
        this.results = new ArrayList<PyUnitTestResult>();
    }

    public void setTotalNumberOfRuns(String totalNumberOfRuns) {
        this.totalNumberOfRuns = totalNumberOfRuns;
    }
    
    public synchronized void addResult(PyUnitTestResult result) {
        if(result.status.equals("fail")){
            numberOfFailures += 1;
            
        } else if(result.status.equals("error")){
            numberOfErrors += 1;
            
        } else if(result.status.equals("ok")){
            //ignore
            
        }else{
            PydevPlugin.log("Unexpected status: "+result.status);
        }
        results.add(result);
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
        if(this.pyUnitLaunch != null){
            IPyUnitLaunch s = this.pyUnitLaunch;
            if(s != null){
                s.stop();
            }
        }
        
    }

    public void relaunch() {
        if(this.pyUnitLaunch != null){
            IPyUnitLaunch s = this.pyUnitLaunch;
            if(s != null){
                s.relaunch();
            }
        }
    }
    
    @Override
    public String toString() {
        return "PyUnitTestResult.\n" +
        		"    Finished: "+this.finished+"\n" +
				"    Number of runs: "+this.results.size()+"" +
				"    Number of failures:"+this.numberOfFailures+"\n" +
				"    Number of errors: "+this.numberOfErrors+"\n" +
				"";
    }

    public void relaunchOnlyErrors() {
        IPyUnitLaunch s = this.pyUnitLaunch;
        if(s != null){
            ArrayList<PyUnitTestResult> arrayList = new ArrayList<PyUnitTestResult>(this.results.size());
            for (PyUnitTestResult pyUnitTestResult : this.results) {
                if(!pyUnitTestResult.status.equals("ok")){
                    arrayList.add(pyUnitTestResult);
                }
            }
            s.relaunchTestResults(arrayList);
        }
    }

}
