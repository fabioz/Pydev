/*
 * Created on 05/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.copiedfromeclipsesrc.JavaVmLocationFinder;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.Tuple;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class SimpleJythonRunner extends SimpleRunner{

    public Tuple<String,String> runAndGetOutputWithJar(String script, String jythonJar, String args, File workingDir, IProject project, IProgressMonitor monitor) {
        //"C:\Program Files\Java\jdk1.5.0_04\bin\java.exe" "-Dpython.home=C:\bin\jython21" 
        //-classpath "C:\bin\jython21\jython.jar;%CLASSPATH%" org.python.util.jython %ARGS%
        //used just for getting info without any classpath nor pythonpath
        
        try {
            String javaLoc = JavaVmLocationFinder.findDefaultJavaExecutable().getCanonicalPath();

            String[] s = new String[]{
                javaLoc,
                "-classpath",
                jythonJar,
                "org.python.util.jython" 
                ,script
            };
            String executionString = getCommandLineAsString(s);

            return runAndGetOutput(executionString, workingDir, project, monitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
    @Override
    public Tuple<String,String> runAndGetOutput(String script, String[] args, File workingDir, IProject project) {
        //"java.exe" -classpath "C:\bin\jython21\jython.jar" -Dpython.path xxx;xxx;xxx org.python.util.jython script %ARGS%

        try {
            String executionString = makeExecutableCommandStr(script);
            
            return runAndGetOutput(executionString, workingDir, project);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }

    /**
     * @param script
     * @return
     * @throws IOException
     */
    public static String makeExecutableCommandStr(String script) throws IOException {
        IInterpreterManager interpreterManager = PydevPlugin.getJythonInterpreterManager();
        String javaLoc = JavaVmLocationFinder.findDefaultJavaExecutable().getCanonicalPath();
        
        File file = new File(javaLoc);
        if(file.exists() == false ){
            throw new RuntimeException("The java location found does not exist. "+javaLoc);
        }
        if(file.isDirectory() == true){
            throw new RuntimeException("The java location found is a directory. "+javaLoc);
        }

        
        
        String jythonJar = interpreterManager.getDefaultInterpreter();
        InterpreterInfo info = (InterpreterInfo) interpreterManager.getInterpreterInfo(jythonJar, new NullProgressMonitor());

        StringBuffer jythonPath = new StringBuffer();
        String pathSeparator = SimpleRunner.getPythonPathSeparator();
        for (String lib : info.libs) {
            if(jythonPath.length() != 0){
                jythonPath.append(pathSeparator); 
            }
            jythonPath.append(lib);
        }
        
        
        //may have the dir or be null
        String cacheDir = null;
        try{
            cacheDir = PydevPlugin.getChainedPrefStore().getString(IInterpreterManager.JYTHON_CACHE_DIR);
        }catch(NullPointerException e){
            //this may happen while running the tests... it should be ok.
            cacheDir = null;
        }
        if(cacheDir != null && cacheDir.trim().length()==0){
            cacheDir = null;
        }
        if(cacheDir != null){
            cacheDir = "-Dpython.cachedir="+ cacheDir.trim();
            
        }
        String[] s = new String[]{
            javaLoc ,
            cacheDir,
            "-Dpython.path="+ jythonPath.toString(),
            "-classpath",
            jythonJar+pathSeparator+jythonPath,
            "org.python.util.jython",
            script
        };
        String executionString = getCommandLineAsString(s);

        return executionString;
    }

}

