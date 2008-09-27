/*
 * Created on Nov 10, 2004
 *
 */
package org.python.pydev.pyunit;

/**
 * @author ggheorg
 *
 */
public class TestResult {
    public final static int OK = 0;
    public final static int FAILED = 1;
    public String klass;
    public String method;
    public int status;
    public long startTime;
    public long endTime;
    
    public TestResult(String klass, String method, int status, long startTime) {
        this.klass = klass;
        this.method = method;
        this.status = status;
        this.startTime = startTime;
    }
    
    public void testFailed() {
        status = FAILED;
    }
    
    public boolean isFailure() {
        return status == FAILED;
    }

    public void testFinished() {
        endTime = System.currentTimeMillis();
    }
    
    public long testDuration() {
        return endTime - startTime;
    }
}
