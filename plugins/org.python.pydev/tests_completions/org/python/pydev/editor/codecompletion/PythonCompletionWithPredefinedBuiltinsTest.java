/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.codecompletion.shell.PythonShell;
import org.python.pydev.editor.codecompletion.shell.PythonShellTest;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class PythonCompletionWithPredefinedBuiltinsTest extends CodeCompletionTestsBase {

    protected boolean isInTestFindDefinition = false;

    public static void main(String[] args) {
        try {
            PythonCompletionWithPredefinedBuiltinsTest builtins = new PythonCompletionWithPredefinedBuiltinsTest();
            builtins.setUp();
            builtins.testPredefinedPaths();
            builtins.tearDown();

            junit.textui.TestRunner.run(PythonCompletionWithPredefinedBuiltinsTest.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected PythonNature createNature() {
        return new PythonNature() {
            @Override
            public int getInterpreterType() throws CoreException {
                return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
            }

            @Override
            public int getGrammarVersion() {
                return IPythonNature.LATEST_GRAMMAR_VERSION;
            }

            @Override
            public String resolveModule(File file) throws MisconfigurationException {
                if (isInTestFindDefinition) {
                    return null;
                }
                return super.resolveModule(file);
            }
        };
    }

    private static PythonShell shell;

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        ADD_MX_TO_FORCED_BUILTINS = false;

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.GetCompletePythonLib(true) + "|" + TestDependent.PYTHON_WXPYTHON_PACKAGES
                + "|" + TestDependent.PYTHON_MX_PACKAGES + "|" + TestDependent.PYTHON_NUMPY_PACKAGES + "|"
                + TestDependent.PYTHON_DJANGO_PACKAGES

                , false);

        codeCompletion = new PyCodeCompletion();

        //we don't want to start it more than once
        if (shell == null) {
            shell = PythonShellTest.startShell();
        }
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), shell);

    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.tearDown();
        AbstractShell.putServerShell(nature, AbstractShell.getShellId(), null);
    }

    @Override
    protected void beforeRestore(InterpreterInfo info) {
        String path = TestDependent.TEST_PYDEV_PLUGIN_LOC + "tests_completions/predefined_completions/";
        assertTrue(new File(path).exists());
        assertTrue(new File(path + "PyQtTest.QtCore.pypredef").exists());
        info.addPredefinedCompletionsPath(path);
    }

    public void testPredefinedPaths() throws Exception {
        String s = "import PyQtTest.QtCore\n" + "PyQtTest.QtCore.";

        requestCompl(s, -1, new String[] { "QAbstractEventDispatcher", "Bool", "QPersistentModelIndex" });
    }

}
