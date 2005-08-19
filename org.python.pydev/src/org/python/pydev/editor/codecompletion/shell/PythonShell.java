/*
 * Created on Aug 16, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.runners.ThreadStreamReaderPrinter;

/**
 * @author Fabio Zadrozny
 */
public class PythonShell extends AbstractShell{

    
    /**
     * Initialize with the default python server file.
     * 
     * @throws IOException
     * @throws CoreException
     */
    public PythonShell() throws IOException, CoreException {
        super(PydevPlugin.getScriptWithinPySrc("pycompletionserver.py"));
    }


    @Override
    protected String createServerProcess(int pWrite, int pRead) throws IOException {
        String interpreter = PydevPlugin.getPythonInterpreterManager().getDefaultInterpreter();
        File file = new File(interpreter);
        if(file.exists() == false ){
            throw new RuntimeException("The interpreter location found does not exist. "+interpreter);
        }
        if(file.isDirectory() == true){
            throw new RuntimeException("The interpreter location found is a directory. "+interpreter);
        }


        String execMsg;
        if(SimpleRunner.isWindowsPlatform()){ //in windows, we have to put python "path_to_file.py"
            execMsg = interpreter+" \""+REF.getFileAbsolutePath(serverFile)+"\" "+pWrite+" "+pRead;
        }else{ //however in mac, or linux, this gives an error...
            execMsg = interpreter+" "+REF.getFileAbsolutePath(serverFile)+" "+pWrite+" "+pRead;
        }
        process = new SimplePythonRunner().createProcess(execMsg, serverFile.getParentFile());
        
        try {
            process.getOutputStream().close(); //we won't write to it...
        } catch (IOException e2) {
        }
        
        //will print things if we are debugging or just get it (and do nothing except emptying it)
        ThreadStreamReaderPrinter std = new ThreadStreamReaderPrinter(process.getInputStream());
        ThreadStreamReaderPrinter err = new ThreadStreamReaderPrinter(process.getErrorStream());

        std.start();
        err.start();

        return execMsg;
    }



}