/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.pythontests;

import java.io.File;

import org.python.pydev.core.TestDependent;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class PythonTest extends AbstractBasicRunTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonTest.class);
    }

    @Override
    protected Throwable exec(File f) {
        System.out.println(StringUtils.format("Running: %s", f));
        Tuple<String, String> output = new SimplePythonRunner().runAndGetOutput(new String[] {
                TestDependent.PYTHON_EXE, "-u", FileUtils.getFileAbsolutePath(f) }, f.getParentFile(), null, null,
                "utf-8");

        System.out.println(StringUtils.format("stdout:%s\nstderr:%s", output.o1,
                output.o2));

        if (output.o2.toLowerCase().indexOf("failed") != -1 || output.o2.toLowerCase().indexOf("traceback") != -1) {
            throw new AssertionError(output.toString());
        }
        return null;
    }

    /**
     * Runs the python tests available in this plugin and in the debug plugin.
     */
    public void testPythonTests() throws Exception {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        execAllAndCheckErrors("test", new File[] { new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/tests"),
                new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/tests_runfiles"),
                new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/tests_python"), });
    }
}
