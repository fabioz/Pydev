/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class PythonInterpreterManager extends AbstractInterpreterManager {

    public PythonInterpreterManager(IPreferenceStore preferences) {
        super(preferences);
    }

    @Override
    protected String getPreferenceName() {
        return PYTHON_INTERPRETER_PATH;
    }

    @Override
    public String getInterpreterUIName() {
        return "Python";
    }

    @Override
    public Tuple<InterpreterInfo, String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor,
            boolean askUser) throws CoreException {
        return doCreateInterpreterInfo(executable, monitor, askUser);
    }

    @Override
    protected String getPreferencesPageId() {
        return "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPagePython";
    }

    /**
     * @param executable the python interpreter from where we should create the info
     * @param monitor a monitor to see the progress
     * 
     * @return the created interpreter info
     * @throws CoreException
     */
    public static Tuple<InterpreterInfo, String> doCreateInterpreterInfo(String executable, IProgressMonitor monitor,
            boolean askUser) throws CoreException {
        boolean isJythonExecutable = InterpreterInfo.isJythonExecutable(executable);
        if (isJythonExecutable) {
            throw new RuntimeException("A jar cannot be used in order to get the info for the python interpreter.");
        }

        File script = getInterpreterInfoPy();

        Tuple<String, String> outTup = new SimplePythonRunner().runAndGetOutputWithInterpreter(executable,
                FileUtils.getFileAbsolutePath(script), null, null, null, monitor, "utf-8");

        InterpreterInfo info = createInfoFromOutput(monitor, outTup, askUser, executable, true);

        if (info == null) {
            //cancelled
            return null;
        }

        info.restoreCompiledLibs(monitor);

        return new Tuple<InterpreterInfo, String>(info, outTup.o1);
    }

    public int getInterpreterType() {
        return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
    }

    public String getManagerRelatedName() {
        return "python";
    }

}
