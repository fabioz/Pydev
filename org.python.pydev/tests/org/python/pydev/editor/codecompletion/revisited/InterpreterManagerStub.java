/*
 * License: Common Public License v1.0
 * Created on 28/07/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.interpreters.AbstractInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class InterpreterManagerStub extends AbstractInterpreterManager implements IInterpreterManager {

    public InterpreterManagerStub(Preferences prefs) {
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
        TestDependent.PYTHON_EXE = info.executableOrJar;
        return info;
    }
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterManager#getDefaultJavaLocation()
     */
    public String getDefaultJavaLocation() {
        return TestDependent.JAVA_LOCATION;
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
    public InterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor) {
        throw new RuntimeException("not impl");
    }

    @Override
    public boolean canGetInfoOnNature(IPythonNature nature) {
        return true;
    }
}