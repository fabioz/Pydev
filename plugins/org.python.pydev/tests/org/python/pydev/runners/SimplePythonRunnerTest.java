/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.TestDependent;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.ui.BundleInfoStub;

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
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setBundleInfo(new BundleInfoStub());
        CorePlugin.setBundleInfo(new BundleInfoStub());
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        PydevPlugin.setBundleInfo(null);
        CorePlugin.setBundleInfo(null);
    }

    /**
     * @throws CoreException
     * @throws IOException
     *
     */
    public void testEnv() throws CoreException, IOException {

        File relativePath = CorePlugin.getBundleInfo().getRelativePath(new Path("pysrc/interpreterInfo.py"));
        String string = new SimplePythonRunner().runAndGetOutput(
                new String[] { TestDependent.PYTHON2_EXE, FileUtils.getFileAbsolutePath(relativePath) }, null, null,
                null,
                "utf-8").o1;
        assertNotNull(string);
        //System.out.println(string);
    }
}
