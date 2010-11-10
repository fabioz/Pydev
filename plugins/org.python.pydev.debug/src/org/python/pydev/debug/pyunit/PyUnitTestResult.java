package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;


public class PyUnitTestResult {

    public final String status;
    public final String location;
    public final String test;
    public final String capturedOutput;
    public final String errorContents;
    public final String time;
    private WeakReference<PyUnitTestRun> testRun;
    
    
    public PyUnitTestResult(PyUnitTestRun testRun, String status, String location, String test, String capturedOutput, String errorContents, String time) {
        //note that the parent has a strong reference to the children.
        this.testRun = new WeakReference<PyUnitTestRun>(testRun);
        this.status = status;
        this.location = location;
        this.test = test;
        this.capturedOutput = capturedOutput;
        this.errorContents = errorContents;
        this.time = time;
    }


    public PyUnitTestRun getTestRun() {
        return this.testRun.get();
    }
}
