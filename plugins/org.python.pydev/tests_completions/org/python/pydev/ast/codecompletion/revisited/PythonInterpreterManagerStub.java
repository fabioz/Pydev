/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 28/07/2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.ast.interpreter_managers.AbstractInterpreterManager;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.interpreter_managers.PythonInterpreterManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.structure.Tuple;

public class PythonInterpreterManagerStub extends AbstractInterpreterManager implements IInterpreterManager {

    public PythonInterpreterManagerStub(IEclipsePreferences prefs) {
        super(prefs);
    }

    public String getDefaultInterpreter() {
        return TestDependent.PYTHON2_EXE;
    }

    @Override
    public IInterpreterInfo[] getInterpreterInfos() {
        String defaultInterpreter = getDefaultInterpreter();
        InterpreterInfo info = (InterpreterInfo) this.createInterpreterInfo(defaultInterpreter,
                new NullProgressMonitor(), false);
        if (!InterpreterInfo.isJythonExecutable(defaultInterpreter)
                && !InterpreterInfo.isIronpythonExecutable(defaultInterpreter)) {
            TestDependent.PYTHON2_EXE = info.executableOrJar;
        }
        return new IInterpreterInfo[] { info };
    }

    /**
     * @throws MisconfigurationException
     * @see org.python.pydev.core.IInterpreterManager#getInterpreterInfo(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor)
            throws MisconfigurationException {
        InterpreterInfo info = super.getInterpreterInfo(executable, monitor);
        if (!InterpreterInfo.isJythonExecutable(executable) && !InterpreterInfo.isIronpythonExecutable(executable)) {
            TestDependent.PYTHON2_EXE = info.executableOrJar;
        }
        return info;
    }

    @Override
    public String getPreferencesPageId() {
        return "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPagePython";
    }

    /**
     * @see org.python.pydev.core.IInterpreterManager#getDefaultJavaLocation()
     */
    public String getDefaultJavaLocation() {
        throw new RuntimeException("not impl");
    }

    @Override
    protected String getPreferenceName() {
        return "pref name";
    }

    @Override
    public String getInterpreterUIName() {
        return "Python";
    }

    @Override
    public Tuple<InterpreterInfo, String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor,
            boolean askUser) throws CoreException, JDTNotAvailableException {
        return PythonInterpreterManager.doCreateInterpreterInfo(executable, monitor, askUser);
    }

    @Override
    public int getInterpreterType() {
        return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
    }

    @Override
    public String getManagerRelatedName() {
        return "python";
    }
}