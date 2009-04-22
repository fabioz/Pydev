/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.PythonInterpreterManagerStub;
import org.python.pydev.ui.interpreters.JythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class JythonInterpreterManagerStub extends PythonInterpreterManagerStub{

    public JythonInterpreterManagerStub(Preferences prefs) {
        super(prefs);
    }

    public String getDefaultInterpreter() {
        return TestDependent.JYTHON_JAR_LOCATION;
    }


    public String addInterpreter(String executable, IProgressMonitor monitor) {
        throw new RuntimeException("not impl");
    }

    /**
     * @see org.python.pydev.core.IInterpreterManager#getInterpreterInfo(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
        InterpreterInfo info = super.getInterpreterInfo(executable, monitor);
        if(info == null){
            throw new RuntimeException("Unable to get info for: "+executable+". Available: "+this.exeToInfo.keySet());
        }
        if(!info.executableOrJar.equals(TestDependent.JYTHON_JAR_LOCATION)){
            throw new RuntimeException("expected same");
        }
        return info;
    }
    
    /**
     * @see org.python.pydev.core.IInterpreterManager#getDefaultJavaLocation()
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
    public Tuple<InterpreterInfo,String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException, JDTNotAvailableException {
        return JythonInterpreterManager.doCreateInterpreterInfo(executable, monitor);
    }

    @Override
    public boolean canGetInfoOnNature(IPythonNature nature) {
        return true;
    }
    

    public boolean isJython() {
        return true;
    }

    public boolean isPython() {
        return false;
    }

    @Override
    public String getManagerRelatedName() {
        return "jython";
    }
}
