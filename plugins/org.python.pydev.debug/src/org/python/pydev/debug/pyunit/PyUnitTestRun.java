package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;
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
    private boolean finished;
    private WeakReference<IPyUnitServer> server;
    
    public PyUnitTestRun(IPyUnitServer server) {
        synchronized (lock) {
            this.name = "Test Run:"+currentRun;
            currentRun += 1;
        }
        if(server != null){
            this.server = new WeakReference<IPyUnitServer>(server);
        }
        this.results = new ArrayList<PyUnitTestResult>();
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

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
    
    public boolean getFinished() {
        return finished;
    }

    public void stop() {
        if(this.server != null){
            IPyUnitServer s = this.server.get();
            if(s != null){
                s.stop();
            }
        }
        
    }
}
