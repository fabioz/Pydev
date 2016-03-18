/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.PreferenceStore;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.PythonInterpreterManagerStub;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.interpreters.JythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class JythonInterpreterManagerStub extends PythonInterpreterManagerStub {

    public JythonInterpreterManagerStub(PreferenceStore prefs) {
        super(prefs);
    }

    @Override
    public String getDefaultInterpreter() {
        return TestDependent.JYTHON_JAR_LOCATION;
    }

    public String addInterpreter(String executable, IProgressMonitor monitor) {
        throw new RuntimeException("not impl");
    }

    /**
     * @throws MisconfigurationException 
     * @see org.python.pydev.core.IInterpreterManager#getInterpreterInfo(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor)
            throws MisconfigurationException {
        InterpreterInfo info = super.getInterpreterInfo(executable, monitor);
        if (!info.executableOrJar.equals(TestDependent.JYTHON_JAR_LOCATION)) {
            throw new RuntimeException("expected same");
        }
        return info;
    }

    /**
     * @see org.python.pydev.core.IInterpreterManager#getDefaultJavaLocation()
     */
    @Override
    public String getDefaultJavaLocation() {
        return TestDependent.JAVA_LOCATION;
    }

    @Override
    protected String getPreferenceName() {
        return "pref name";
    }

    @Override
    public String getInterpreterUIName() {
        return "Jython";
    }

    @Override
    public Tuple<InterpreterInfo, String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor,
            boolean askUser) throws CoreException, JDTNotAvailableException {
        return JythonInterpreterManager.doCreateInterpreterInfo(executable, monitor, askUser);
    }

    @Override
    public String getManagerRelatedName() {
        return "jython";
    }
}
