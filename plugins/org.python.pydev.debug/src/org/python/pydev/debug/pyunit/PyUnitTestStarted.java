package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

public class PyUnitTestStarted {

    public final String location;
    public final String test;

    private WeakReference<PyUnitTestRun> testRun;

    public PyUnitTestStarted(PyUnitTestRun testRun, String location, String test) {
        this.testRun = new WeakReference<PyUnitTestRun>(testRun);
        this.location = location;
        this.test = test;
    }
    

    public PyUnitTestRun getTestRun() {
        return this.testRun.get();
    }

}
