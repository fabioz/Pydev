/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 21, 2006
 */
package org.python.pydev.jythontests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class JythonTest extends TestCase {

    final File[] foldersWithTestContentsOnSameProcess = new File[] {
            new File(TestDependent.TEST_PYDEV_JYTHON_PLUGIN_LOC + "jysrc/tests"),
            new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests/jysrc/tests"),
            new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/tests_runfiles"), };

    final File[] additionalPythonpathFolders = new File[] {
            new File(TestDependent.TEST_PYDEV_JYTHON_PLUGIN_LOC + "jysrc/"),
            new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/"), new File(TestDependent.JYTHON_ANT_JAR_LOCATION),
            new File(TestDependent.JYTHON_JUNIT_JAR_LOCATION), new File(TestDependent.JYTHON_LIB_LOCATION), };

    private static final boolean RUN_TESTS_ON_SEPARATE_PROCESS = true;
    private static final boolean RUN_TESTS_ON_SAME_PROCESS = true;

    public static void main(String[] args) {
        try {
            JythonTest builtins = new JythonTest();
            builtins.setUp();
            builtins.testJythonTests();
            builtins.testJythonTestsOnSeparateProcess();
            builtins.tearDown();

            junit.textui.TestRunner.run(JythonTest.class);

            System.out.println("Finished");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        JythonPlugin.setDebugReload(false);
        JythonPlugin.IN_TESTS = true;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        JythonPlugin.setDebugReload(true);
    }

    public void testJythonTests() throws Exception {
        if (RUN_TESTS_ON_SAME_PROCESS) {
            //unittest.TestCase format: the __main__ is required in the global namespace
            HashMap<String, Object> locals = new HashMap<String, Object>();
            locals.put("__name__", "__main__");
            IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(false, false);
            ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
            ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
            interpreter.setErr(stdErr);
            interpreter.setOut(stdOut);

            List<Throwable> errors = JythonPlugin.execAll(locals, "test", interpreter,
                    foldersWithTestContentsOnSameProcess, additionalPythonpathFolders);

            System.out.println(stdOut);
            System.out.println(stdErr);
            failIfErrorsRaised(errors, stdErr);
        }
    }

    public void testJythonTestsOnSeparateProcess() throws Exception {
        if (RUN_TESTS_ON_SEPARATE_PROCESS) {
            final Set<String> skip = new HashSet<>();
            skip.add("test_pydev_ipython_010.py");
            skip.add("test_pydev_ipython_011.py");
            FileFilter filter = new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    if (skip.contains(pathname.getName())) {
                        return false;
                    }
                    return true;
                }
            };
            //has to be run on a separate process because it'll call exit()
            List<Throwable> errors = JythonTest.execAll("test", new File[] { new File(
                    TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/tests"), }, filter);
            if (errors.size() > 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write("There have been errors while executing the test scripts in jython.\n\n".getBytes());
                for (Throwable throwable : errors) {
                    throwable.printStackTrace(new PrintStream(out));
                }
                fail(new String(out.toByteArray()));
            }
        }
    }

    private void failIfErrorsRaised(List<Throwable> errors, ByteArrayOutputStream stdErr) throws IOException {
        if (errors.size() > 0) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("There have been errors while executing the test scripts in jython.\n\n".getBytes());
            for (Throwable throwable : errors) {
                throwable.printStackTrace(new PrintStream(out));
            }
            fail(new String(out.toByteArray()));
        }
        if (stdErr != null) {
            String errStr = new String(stdErr.toByteArray());
            if (errStr.length() > 0) {
                if (errStr.indexOf("FAILED") != -1) {
                    fail(errStr);
                }
            }
        }
    }

    public static List<Throwable> execAll(final String startingWith, File[] beneathFolders) {
        return execAll(startingWith, beneathFolders, new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return true;
            }
        });
    }

    public static List<Throwable> execAll(final String startingWith, File[] beneathFolders, FileFilter filter) {
        List<Throwable> errors = new ArrayList<Throwable>();
        for (File file : beneathFolders) {
            if (file != null) {
                if (!file.exists()) {
                    String msg = "The folder:" + file + " does not exist and therefore cannot be used to "
                            + "find scripts to run starting with:" + startingWith;
                    Log.log(IStatus.ERROR, msg, null);
                    errors.add(new RuntimeException(msg));
                }
                File[] files = JythonPlugin.getFilesBeneathFolder(startingWith, file);
                for (File f : files) {
                    if (filter.accept(f)) {

                        Throwable throwable = exec(f);
                        if (throwable != null) {
                            errors.add(throwable);
                        }
                    }
                }
            }
        }
        return errors;
    }

    private static Throwable exec(File f) {
        System.out.println(StringUtils.format("Running: %s", f));

        String sep = SimpleRunner.getPythonPathSeparator();
        assertTrue(new File(TestDependent.JYTHON_ANT_JAR_LOCATION).exists());
        assertTrue(new File(TestDependent.JYTHON_JUNIT_JAR_LOCATION).exists());
        String pythonpath = TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/" + sep
                + TestDependent.JYTHON_ANT_JAR_LOCATION + sep + TestDependent.JYTHON_JUNIT_JAR_LOCATION;

        Tuple<String, String> output = new SimpleJythonRunner().runAndGetOutputWithJar(new File(
                TestDependent.JAVA_LOCATION), f.toString(), TestDependent.JYTHON_JAR_LOCATION, null, f.getParentFile(),
                null, null, pythonpath, "utf-8");

        System.out.println(StringUtils.format("stdout:%s\nstderr:%s", output.o1,
                output.o2));

        if (output.o2.toLowerCase().indexOf("failed") != -1 || output.o2.toLowerCase().indexOf("traceback") != -1) {
            throw new AssertionError(output.toString());
        }
        return null;
    }

}
