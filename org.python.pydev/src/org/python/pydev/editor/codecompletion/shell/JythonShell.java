/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.ThreadStreamReaderPrinter;

public class JythonShell extends AbstractShell{

    public JythonShell() throws IOException, CoreException {
        super(PydevPlugin.getScriptWithinPySrc("jycompletionserver.py"));
    }
    

    @Override
    protected String createServerProcess(int pWrite, int pRead) throws IOException {
        String args = pWrite+" "+pRead;
        String script = REF.getFileAbsolutePath(serverFile);
        String executableStr = SimpleJythonRunner.makeExecutableCommandStr(script);
        executableStr += " "+args;
        process = new SimplePythonRunner().createProcess(executableStr, serverFile.getParentFile());
        
        try {
            process.getOutputStream().close(); //we won't write to it...
        } catch (IOException e2) {
        }
        
        //will print things if we are debugging or just get it (and do nothing except emptying it)
        ThreadStreamReaderPrinter std = new ThreadStreamReaderPrinter(process.getInputStream());
        ThreadStreamReaderPrinter err = new ThreadStreamReaderPrinter(process.getErrorStream());

        std.start();
        err.start();
        
        return executableStr;
    }


}
