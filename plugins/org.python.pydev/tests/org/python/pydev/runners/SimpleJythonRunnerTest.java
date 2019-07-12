/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 07/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.python.pydev.ast.codecompletion.revisited.PythonInterpreterManagerStub;
import org.python.pydev.ast.codecompletion.revisited.jython.JythonCodeCompletionTestsBase;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimpleJythonRunner;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.io.FileUtils;

public class SimpleJythonRunnerTest extends JythonCodeCompletionTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SimpleJythonRunnerTest.class);
    }

    private IInterpreterManager curr;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        curr = InterpreterManagersAPI.getJythonInterpreterManager();
        InterpreterManagersAPI.setJythonInterpreterManager(new PythonInterpreterManagerStub(getPreferences()));
    }

    @Override
    public void tearDown() throws Exception {
        InterpreterManagersAPI.setJythonInterpreterManager(curr);
    }

    public void testRun() throws CoreException, IOException {
        SimpleJythonRunner runner = new SimpleJythonRunner();
        File absoluteFile = CorePlugin.getBundleInfo().getRelativePath(new Path("interpreterInfo.py"))
                .getAbsoluteFile();
        String string = runner.runAndGetOutputWithJar(FileUtils.getFileAbsolutePath(absoluteFile),
                TestDependent.JYTHON_JAR_LOCATION, null, null, null, new NullProgressMonitor(), "utf-8").o1;
        assertNotNull(string);
    }
}
