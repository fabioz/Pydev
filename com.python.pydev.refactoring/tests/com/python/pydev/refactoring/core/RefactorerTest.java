/*
 * Created on May 9, 2005
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.refactoring.core;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class RefactorerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RefactorerTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        Refactorer refactorer = new Refactorer();
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
    public void testExtract() {
    }
}
