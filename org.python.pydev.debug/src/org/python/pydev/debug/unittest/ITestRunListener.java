/*
 * Created on Sep 26, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.python.pydev.debug.unittest;

/**
 * @author ggheorg
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface ITestRunListener {
	void testsStarted(int testCount, String testFile);
	void testsFinished(String summary);
	void testStarted(String klass, String method);
	void testOK(String klass, String method);
	void testFailed(String klass, String method, String failureType, String trace);
}
