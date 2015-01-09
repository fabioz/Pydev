/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ironpythontests;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.TestDependent;
import org.python.pydev.pythontests.AbstractBasicRunTestCase;
import org.python.pydev.runners.SimpleIronpythonRunner;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class IronpythonTest extends AbstractBasicRunTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IronpythonTest.class);
    }

    @Override
    protected Throwable exec(File f) {
        System.out.println(StringUtils.format("Running: %s", f));
        Tuple<String, String> output = new SimpleIronpythonRunner()
                .runAndGetOutput(
                        new String[] { TestDependent.IRONPYTHON_EXE, "-u",
                                IInterpreterManager.IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS,
                                FileUtils.getFileAbsolutePath(f) },
                        f.getParentFile(), null, null, "utf-8");

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
    public void testIronPythonTests() throws Exception {
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
        execAllAndCheckErrors("test", new File[] { new File(TestDependent.TEST_PYDEV_PLUGIN_LOC + "pysrc/tests"), },
                filter);
    }

}
