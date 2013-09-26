/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IronpythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class IronPythonCodeCompletionTestsBase extends CodeCompletionTestsBase {

    protected boolean isInTestFindDefinition = false;

    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getIronpythonInterpreterManager();
    }

    @Override
    protected void setInterpreterManager(String path) {
        IronpythonInterpreterManager interpreterManager = new IronpythonInterpreterManager(this.getPreferences());

        InterpreterInfo info = (InterpreterInfo) interpreterManager.createInterpreterInfo(TestDependent.IRONPYTHON_EXE,
                new NullProgressMonitor(), false);
        TestDependent.IRONPYTHON_EXE = info.executableOrJar;

        if (path != null) {
            info = new InterpreterInfo(info.getVersion(), info.executableOrJar,
                    PythonPathHelper.parsePythonPathFromStr(path, new ArrayList<String>()));
        }

        interpreterManager.setInfos(new IInterpreterInfo[] { info }, null, null);
        PydevPlugin.setIronpythonInterpreterManager(interpreterManager);
    }

    @Override
    protected PythonNature createNature() {
        return new PythonNature() {
            @Override
            public int getInterpreterType() throws CoreException {
                return IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON;
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

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(null, false);
    }

    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.tearDown();
    }
}
