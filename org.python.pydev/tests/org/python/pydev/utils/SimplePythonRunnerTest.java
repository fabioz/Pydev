/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class SimplePythonRunnerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SimplePythonRunnerTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * 
     */
    public void testEnv() {
        
        ArrayList list = new ArrayList();
        String string = SimplePythonRunner.runAndGetOutput("python D:/dev_programs/eclipse_3/eclipse/workspace/org.python.pydev/PySrc/interpreterInfo.py", null);
        System.out.println(string);
    }
}
