/*
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleIronpythonRunner;

/**
 * @author Fabio Zadrozny
 */
public class IronpythonShell extends AbstractShell{

    
    /**
     * Initialize with the default python server file.
     * 
     * @throws IOException
     * @throws CoreException
     */
    public IronpythonShell() throws IOException, CoreException {
        super(PydevPlugin.getScriptWithinPySrc("pycompletionserver.py"));
    }


    @Override
    protected synchronized String createServerProcess(IInterpreterInfo interpreter, int pWrite, int pRead) throws IOException {
        File file = new File(interpreter.getExecutableOrJar());
        if(file.exists() == false ){
            throw new RuntimeException("The interpreter location found does not exist. "+interpreter);
        }
        if(file.isDirectory() == true){
            throw new RuntimeException("The interpreter location found is a directory. "+interpreter);
        }


        String execMsg;
        if(REF.isWindowsPlatform()){ //in windows, we have to put python "path_to_file.py"
            execMsg = interpreter+" \""+REF.getFileAbsolutePath(serverFile)+"\" "+pWrite+" "+pRead;
        }else{ //however in mac, or linux, this gives an error...
            execMsg = interpreter+" "+REF.getFileAbsolutePath(serverFile)+" "+pWrite+" "+pRead;
        }
        String[] parameters = SimpleIronpythonRunner.preparePythonCallParameters(
                interpreter.getExecutableOrJar(), REF.getFileAbsolutePath(serverFile), new String[]{""+pWrite, ""+pRead});
        
        IInterpreterManager manager = PydevPlugin.getPythonInterpreterManager();
        
        String[] envp = null;
        try {
            envp = new SimpleIronpythonRunner().getEnvironment(null, interpreter, manager);
        } catch (CoreException e) {
            Log.log(e);
        }
        
        process = SimpleIronpythonRunner.createProcess(parameters, envp, serverFile.getParentFile());

        return execMsg;
    }



}