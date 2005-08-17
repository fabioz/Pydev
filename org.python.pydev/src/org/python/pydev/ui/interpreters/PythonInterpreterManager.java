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
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class PythonInterpreterManager extends AbstractInterpreterManager{

    public PythonInterpreterManager(Preferences prefs) {
        super(prefs);
    }

    @Override
    protected String getPreferenceName() {
        return PYTHON_INTERPRETER_PATH;
    }
    
    @Override
    protected String getNotConfiguredInterpreterMsg() {
        return "Interpreter is not properly configured! Please go to window->preferences->PyDev->Python Interpreters and configure it.";
    }

    @Override
    public InterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException {
        return doCreateInterpreterInfo(executable, monitor);
    }

    /**
     * @param executable the python interpreter from where we should create the info
     * @param monitor a monitor to see the progress
     * 
     * @return the created interpreter info
     * @throws CoreException
     */
    public static InterpreterInfo doCreateInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException {
        boolean isJythonExecutable = InterpreterInfo.isJythonExecutable(executable);
        if(isJythonExecutable){
            throw new RuntimeException("A jar cannot be used in order to get the info for the python interpreter.");
        }                

        File script = PydevPlugin.getScriptWithinPySrc("interpreterInfo.py");

        String output = new SimplePythonRunner().runAndGetOutputWithInterpreter(executable, REF.getFileAbsolutePath(script), null, null, null, monitor);
        
        InterpreterInfo info = InterpreterInfo.fromString(output);
        info.restoreCompiledLibs(monitor);
        
        return info;
    }

    @Override
    public boolean canGetInfoOnNature(IPythonNature nature) {
        try {
            return nature.isPython();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isJython() {
        return false;
    }

    public boolean isPython() {
        return true;
    }



}
