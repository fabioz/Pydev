/*
 * License: Common Public License v1.0
 * Created on 28/07/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.interpreters.AbstractInterpreterManager;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class PythonInterpreterManagerStub extends AbstractInterpreterManager implements IInterpreterManager {

    public PythonInterpreterManagerStub(Preferences prefs) {
        super(prefs);
    }

    public String getDefaultInterpreter() {
        return TestDependent.PYTHON_EXE;
    }

    public String[] getInterpreters() {
        return new String[]{TestDependent.PYTHON_EXE};
    }

    public String addInterpreter(String executable, IProgressMonitor monitor) {
        throw new RuntimeException("not impl");
    }

    public String[] getInterpretersFromPersistedString(String persisted) {
        throw new RuntimeException("not impl");
    }

    public String getStringToPersist(String[] executables) {
        throw new RuntimeException("not impl");
    }
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getInterpreterInfo(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
        
        InterpreterInfo info = super.getInterpreterInfo(executable, monitor);
        if(!InterpreterInfo.isJythonExecutable(executable)){
            TestDependent.PYTHON_EXE = info.executableOrJar;
        }
        return info;
    }
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getDefaultJavaLocation()
     */
    public String getDefaultJavaLocation() {
        throw new RuntimeException("not impl");
    }

    @Override
    protected String getPreferenceName() {
        return "pref name";
    }

    @Override
    protected String getNotConfiguredInterpreterMsg() {
        return "getNotConfiguredInterpreterMsg";
    }

    @Override
    public InterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException {
        return PythonInterpreterManager.doCreateInterpreterInfo(executable, monitor);
    }

    @Override
    public boolean canGetInfoOnNature(IPythonNature nature) {
        return true;
    }

    public boolean isJython() {
        return false;
    }

    public boolean isPython() {
        return true;
    }
}