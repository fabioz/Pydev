/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimpleRunner;

public class JythonShell extends AbstractShell{

    public JythonShell() throws IOException, CoreException {
        super(PydevPlugin.getScriptWithinPySrc("pycompletionserver.py"));
    }
    

    /**
     * Will create the jython shell and return a string to be shown to the user with the jython shell command line.
     */
    @Override
    protected synchronized String createServerProcess(int pWrite, int pRead) throws IOException, JDTNotAvailableException {
        String args = pWrite+" "+pRead;
        String script = REF.getFileAbsolutePath(serverFile);
        String[] executableStr = SimpleJythonRunner.makeExecutableCommandStr(script, "", args);
        process = SimpleRunner.createProcess(executableStr, serverFile.getParentFile());
        
        return SimpleRunner.getArgumentsAsStr(executableStr);
    }


}
