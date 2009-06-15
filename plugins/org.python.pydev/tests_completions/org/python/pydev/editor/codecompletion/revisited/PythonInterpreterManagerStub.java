/*
 * License: Common Public License v1.0
 * Created on 28/07/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
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


    @Override
    public IInterpreterInfo[] getInterpreterInfos() {
        String defaultInterpreter = getDefaultInterpreter();
        InterpreterInfo info = (InterpreterInfo) this.createInterpreterInfo(defaultInterpreter, new NullProgressMonitor());
        if(!InterpreterInfo.isJythonExecutable(defaultInterpreter) && !InterpreterInfo.isIronpythonExecutable(defaultInterpreter)){
            TestDependent.PYTHON_EXE = info.executableOrJar;
        }
        return new IInterpreterInfo[]{info};
    }
    
    /**
     * @see org.python.pydev.core.IInterpreterManager#getInterpreterInfo(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
        InterpreterInfo info = super.getInterpreterInfo(executable, monitor);
        if(info == null){
            throw new RuntimeException("Unable to get info for: "+executable+". Available: "+this.exeToInfo.keySet());
        }
        if(!InterpreterInfo.isJythonExecutable(executable) && !InterpreterInfo.isIronpythonExecutable(executable)){
            TestDependent.PYTHON_EXE = info.executableOrJar;
        }
        return info;
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
    protected String getNotConfiguredInterpreterMsg() {
        return "getNotConfiguredInterpreterMsg";
    }

    @Override
    public Tuple<InterpreterInfo,String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException, JDTNotAvailableException {
        return PythonInterpreterManager.doCreateInterpreterInfo(executable, monitor);
    }

    
    public int getInterpreterType() {
        return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
    }
    
    public String getManagerRelatedName() {
        return "python";
    }
}