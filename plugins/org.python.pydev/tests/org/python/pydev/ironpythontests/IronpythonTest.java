/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ironpythontests;

import java.io.File;

import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.pythontests.AbstractBasicRunTestCase;
import org.python.pydev.runners.SimpleIronpythonRunner;

public class IronpythonTest extends AbstractBasicRunTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IronpythonTest.class);
    }

    protected Throwable exec(File f) {
        System.out.println(StringUtils.format("Running: %s", f));
        Tuple<String, String> output = new SimpleIronpythonRunner().runAndGetOutput(
                new String[] { TestDependent.IRONPYTHON_EXE, "-u",
                        IInterpreterManager.IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS, REF.getFileAbsolutePath(f) },
                f.getParentFile(), null, null, "utf-8");

        System.out.println(StringUtils.format("stdout:%s\nstderr:%s", output.o1, output.o2));

        if (output.o2.toLowerCase().indexOf("failed") != -1 || output.o2.toLowerCase().indexOf("traceback") != -1) {
            throw new AssertionError(output.toString());
        }
        return null;
    }

    /**
     * Runs the python tests available in this plugin and in the debug plugin.
     */
    public void testPythonTests() throws Exception {
        execAllAndCheckErrors("test", new File[] { new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/tests"), });
    }

}
