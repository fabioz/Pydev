/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.SimpleRunner;

public class JythonShell extends AbstractShell{

    public JythonShell() throws IOException, CoreException {
        super(PydevPlugin.getScriptWithinPySrc("pycompletionserver.py"));
    }
    

    /**
     * Will create the jython shell and return a string to be shown to the user with the jython shell command line.
     */
    @Override
    protected synchronized String createServerProcess(IInterpreterInfo jythonJar, int pWrite, int pRead) throws IOException, JDTNotAvailableException {
        String script = REF.getFileAbsolutePath(serverFile);
        String[] executableStr = SimpleJythonRunner.makeExecutableCommandStr(jythonJar.getExecutableOrJar(), script, "", String.valueOf(pWrite), String.valueOf(pRead));
        
        IInterpreterManager manager = PydevPlugin.getJythonInterpreterManager();
        
        String[] envp = null;
        try {
            envp = new SimplePythonRunner().getEnvironment(null, jythonJar, manager);
        } catch (CoreException e) {
            Log.log(e);
        }

        process = SimpleRunner.createProcess(executableStr, envp, serverFile.getParentFile());
        
        return SimpleRunner.getArgumentsAsStr(executableStr);
    }


}
