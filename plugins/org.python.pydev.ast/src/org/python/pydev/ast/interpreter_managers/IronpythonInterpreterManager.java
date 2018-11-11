/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.interpreter_managers;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.python.pydev.ast.runners.SimpleIronpythonRunner;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class IronpythonInterpreterManager extends AbstractInterpreterManager {

    public IronpythonInterpreterManager(IEclipsePreferences prefs) {
        super(prefs);
    }

    @Override
    protected String getPreferenceName() {
        return IRONPYTHON_INTERPRETER_PATH;
    }

    @Override
    public String getInterpreterUIName() {
        return "IronPython.";
    }

    @Override
    public Tuple<InterpreterInfo, String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor,
            boolean askUser) throws CoreException {
        return doCreateInterpreterInfo(executable, monitor, askUser);
    }

    @Override
    public String getPreferencesPageId() {
        return "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPageIronpython";
    }

    /**
     * @param executable the IronPython interpreter from where we should create the info
     * @param monitor a monitor to see the progress
     *
     * @return the created interpreter info
     * @throws CoreException
     */
    public static Tuple<InterpreterInfo, String> doCreateInterpreterInfo(String executable, IProgressMonitor monitor,
            boolean askUser) throws CoreException {
        boolean isJythonExecutable = InterpreterInfo.isJythonExecutable(executable);
        if (isJythonExecutable) {
            throw new RuntimeException("A jar cannot be used in order to get the info for the IronPython interpreter.");
        }

        File script = getInterpreterInfoPy();

        Tuple<String, String> outTup = new SimpleIronpythonRunner().runAndGetOutputWithInterpreter(executable,
                FileUtils.getFileAbsolutePath(script), null, null, null, monitor, "utf-8");

        InterpreterInfo info = createInfoFromOutput(monitor, outTup, askUser, executable, false);

        if (info == null) {
            //cancelled
            return null;
        }

        info.restoreCompiledLibs(monitor);

        return new Tuple<InterpreterInfo, String>(info, outTup.o1);
    }

    @Override
    public int getInterpreterType() {
        return IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON;
    }

    @Override
    public String getManagerRelatedName() {
        return "ironpython";
    }

}
