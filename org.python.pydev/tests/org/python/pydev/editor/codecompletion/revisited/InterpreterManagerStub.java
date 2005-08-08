/*
 * License: Common Public License v1.0
 * Created on 28/07/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.ui.IInterpreterManager;
import org.python.pydev.ui.InterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class InterpreterManagerStub extends InterpreterManager implements IInterpreterManager {

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
     * @see org.python.pydev.ui.IInterpreterManager#getInterpreterInfo(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
        
        InterpreterInfo info = super.getInterpreterInfo(executable, monitor);
        TestDependent.PYTHON_EXE = info.executable;
        return info;
    }
    /**
     * @see org.python.pydev.ui.IInterpreterManager#getDefaultJythonJar()
     */
    public String getDefaultJythonJar() {
        return TestDependent.JYTHON_JAR_LOCATION;
    }

    /**
     * @see org.python.pydev.ui.IInterpreterManager#getDefaultJythonHome()
     */
    public String getDefaultJythonHome() {
        return TestDependent.JYTHON_HOME_LOCATION;
    }

    /**
     * @see org.python.pydev.ui.IInterpreterManager#getDefaultJythonPath()
     */
    public String getDefaultJythonPath() {
        return TestDependent.JYTHON_JYTHON_PATH;
    }
    
    /**
     * @see org.python.pydev.ui.IInterpreterManager#getDefaultJavaLocation()
     */
    public String getDefaultJavaLocation() {
        return TestDependent.JAVA_LOCATION;
    }
}