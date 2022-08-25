/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.runners;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.ast.runners.UniversalRunner;
import org.python.pydev.ast.runners.UniversalRunner.AbstractRunner;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.structure.Tuple;

public class PythonUniversalRunnerTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonUniversalRunnerTest.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.IRONPYTHON_LIB, false);
    }

    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.tearDown();
    }

    public void testUniversalRunnerWithJython() throws Exception {
        AbstractRunner runner = UniversalRunner.getRunner(nature);
        assertEquals(nature.getInterpreterType(), IPythonNature.INTERPRETER_TYPE_PYTHON);
        Tuple<String, String> output = runner.runCodeAndGetOutput(
                "import sys\nprint('test')\nprint('err', file=sys.stderr)", null, null, new NullProgressMonitor());
        assertEquals("test", output.o1.trim());
        assertEquals("err", output.o2.trim());

        Tuple<Process, String> createProcess = runner.createProcess(TestDependent.TEST_PYSRC_TESTING_LOC
                + "universal_runner_test.py", null, null, new NullProgressMonitor());
        output = SimpleRunner.getProcessOutput(createProcess.o1, "", new NullProgressMonitor(), "utf-8");
        assertEquals("stdout", output.o1.trim());
        assertEquals("stderr", output.o2.trim());
    }

}
