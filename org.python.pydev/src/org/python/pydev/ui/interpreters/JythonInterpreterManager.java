/*
 * License: Common Public License v1.0
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class JythonInterpreterManager extends AbstractInterpreterManager{

    public JythonInterpreterManager(Preferences prefs) {
        super(prefs);
    }

    @Override
    protected String getPreferenceName() {
        return JYTHON_INTERPRETER_PATH;
    }
    
    @Override
    protected String getNotConfiguredInterpreterMsg() {
        return "Interpreter is not properly configured! Please go to window->preferences->PyDev->Jython Interpreters and configure it.";
    }

    @Override
    public InterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException {
        File script = PydevPlugin.getScriptWithinPySrc("interpreterInfo.py");
        boolean isJythonExecutable = isJythonExecutable(executable);
        
        if(!isJythonExecutable){
            throw new RuntimeException("In order to get the info for the jython interpreter, a jar is needed (e.g.: jython.jar)");
        }
        
        String output = new SimpleJythonRunner().runAndGetOutputWithJar(REF.getFileAbsolutePath(script), executable, null, null, null, monitor);
        
        InterpreterInfo info = InterpreterInfo.fromString(output);
        info.restoreCompiledLibs(monitor);
        
        //the executable is the jar itself
        info.executableOrJar = executable;

        return info;
    }

    @Override
    public boolean canGetInfoOnNature(IPythonNature nature) {
        try {
            return nature.isJython();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }


}
