/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.IronpythonInterpreterManagerStub;
import org.python.pydev.editor.codecompletion.revisited.jython.JythonCodeCompletionTestsBase;
import org.python.pydev.plugin.PydevPlugin;

public class SimpleIronpythonRunnerTest extends JythonCodeCompletionTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SimpleIronpythonRunnerTest.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setIronpythonInterpreterManager(new IronpythonInterpreterManagerStub(getPreferences()));

    }

    public void testRun() throws CoreException, IOException {
        SimpleIronpythonRunner runner = new SimpleIronpythonRunner();
        File absoluteFile = PydevPlugin.getBundleInfo().getRelativePath(new Path("interpreterInfo.py"))
                .getAbsoluteFile();
        String string = runner.runAndGetOutputWithInterpreter(TestDependent.IRONPYTHON_EXE,
                absoluteFile.getCanonicalPath(), null, null, null, new NullProgressMonitor(), "utf-8").o1;
        //        String string = runner.runAndGetOutput(absoluteFile.getCanonicalPath(), (String)null, null);
        assertNotNull(string);
        //        System.out.println(string);
    }
}
