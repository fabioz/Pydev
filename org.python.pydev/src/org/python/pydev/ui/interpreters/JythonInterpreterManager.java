/*
 * License: Common Public License v1.0
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.copiedfromeclipsesrc.JavaVmLocationFinder;
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
        return doCreateInterpreterInfo(executable, monitor);
    }

    /**
     * This is the method that creates the interpreter info for jython. It gets the info on the jython side and on the java side
     * 
     * @param executable the jar that should be used to get the info
     * @param monitor a monitor, to keep track of what's happening
     * @return the interpreter info, with the default libraries and jars
     * 
     * @throws CoreException
     */
    public static InterpreterInfo doCreateInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException {
        boolean isJythonExecutable = InterpreterInfo.isJythonExecutable(executable);
        
        if(!isJythonExecutable){
            throw new RuntimeException("In order to get the info for the jython interpreter, a jar is needed (e.g.: jython.jar)");
        }
        File script = PydevPlugin.getScriptWithinPySrc("interpreterInfo.py");
        
        //gets the info for the python side
        String output = new SimpleJythonRunner().runAndGetOutputWithJar(REF.getFileAbsolutePath(script), executable, null, null, null, monitor);
        
        InterpreterInfo info = InterpreterInfo.fromString(output);
        //the executable is the jar itself
        info.executableOrJar = executable;
        
        //we have to find the jars before we restore the compiled libs 
        List<File> jars = JavaVmLocationFinder.findDefaultJavaJars();
        for (File jar : jars) {
            info.libs.add(REF.getFileAbsolutePath(jar));
        }
        
        //java, java.lang, etc should be found now
        info.restoreCompiledLibs(monitor);
        

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
