/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.pep8;

import java.io.File;

import org.python.pydev.core.formatter.ExternalFormatterException;

import junit.framework.TestCase;

/**
 * Runs real subprocesses (the JVM running the test) to check how external-tool failures surface.
 */
public class RunnerCommonTest extends TestCase {

    private static String javaExecutable() {
        return new File(System.getProperty("java.home"), "bin" + File.separator + "java").getAbsolutePath();
    }

    public void testFailingToolRaisesAnErrorCarryingItsStderr() throws Exception {
        // A class that does not exist: the JVM exits with 1 and explains on stderr.
        ProcessBuilder builder = new ProcessBuilder(javaExecutable(), "-cp", ".", "NoSuchMainClassForPyDevTest");
        Process process = builder.start();

        try {
            RunnerCommon.writeContentsAndGetOutputOrThrow("x = 1\n".getBytes("utf-8"), "utf-8", process,
                    "java NoSuchMainClassForPyDevTest", "black");
            fail("Expected ExternalFormatterException");
        } catch (ExternalFormatterException e) {
            assertEquals("black", e.getToolName());
            assertTrue(e.getMessage(), e.getMessage().contains("NoSuchMainClassForPyDevTest"));
            assertTrue(e.getMessage(), e.getMessage().contains("java NoSuchMainClassForPyDevTest"));
        }
    }

    public void testSuccessfulToolReturnsItsStdout() throws Exception {
        // -version writes to stderr only and exits with 0: the tool "succeeded" with empty stdout.
        Process process = new ProcessBuilder(javaExecutable(), "-version").start();

        String output = RunnerCommon.writeContentsAndGetOutputOrThrow("x = 1\n".getBytes("utf-8"), "utf-8", process,
                "java -version", "black");

        assertEquals("", output);
    }
}
