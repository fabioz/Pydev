/*
 * Created on Sep 26, 2004
 *
 */
package org.python.pydev.pyunit;

/**
 * @author ggheorg
 *
 */
public interface ITestRunListener {
    void testsStarted(int testCount);
    void testsFinished();
    void testStarted(String klass, String method);
    void testFailed(String klass, String method, String trace);
}
