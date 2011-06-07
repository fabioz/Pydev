/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 05/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringSubstitution;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.plugin.PydevPlugin;

public class SimpleRunner {

    /**
     * Passes the commands directly to Runtime.exec (with the passed envp)
     */
    public static Process createProcess(String[] cmdarray, String[] envp, File workingDir) throws IOException {
        return Runtime.getRuntime().exec(getWithoutEmptyParams(cmdarray), getWithoutEmptyParams(envp), workingDir);
    }

    /**
     * @return a new array without any null/empty elements originally contained in the array.
     */
    private static String[] getWithoutEmptyParams(String[] cmdarray) {
        if(cmdarray == null){
            return null;
        }
        ArrayList<String> list = new ArrayList<String>();
        for (String string : cmdarray) {
            if(string != null && string.length() > 0){
                list.add(string);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * THIS CODE IS COPIED FROM org.eclipse.debug.internal.core.LaunchManager
     * 
     * changed so that we always set the PYTHONPATH in the environment
     * 
     * @return the system environment with the PYTHONPATH env variable added for a given project (if it is null, return it with the
     * default PYTHONPATH added).
     */
    public static String[] getEnvironment(
            IPythonNature pythonNature, IInterpreterInfo interpreter, IInterpreterManager manager, boolean removePythonpathFromDefaultEnv) throws CoreException {
        String[] env;
        
        if(pythonNature == null){ //no associated nature in the project... just get the default env
            env = getDefaultSystemEnvAsArray(null, removePythonpathFromDefaultEnv);
        }else{
            String pythonPathEnvStr = "";
            try {
                
                if (interpreter != null){ //check if we have a default interpreter.
                    pythonPathEnvStr = makePythonPathEnvString(pythonNature, interpreter, manager);
                }
                env = createEnvWithPythonpath(pythonPathEnvStr, pythonNature, manager);
                
            } catch (Exception e) {
                PydevPlugin.log(e);
                //We cannot get it. Log it and keep with the default.
                env = getDefaultSystemEnvAsArray(pythonNature, removePythonpathFromDefaultEnv);
            }
        }
        
        if(interpreter != null){
            env = interpreter.updateEnv(env);
        }
        
        

        return env;
    }

    /**
     * Same as the getEnvironment, but with a pre-specified pythonpath.
     * @throws MisconfigurationException 
     */
    public static String[] createEnvWithPythonpath(String pythonPathEnvStr, String interpreter, IInterpreterManager manager, IPythonNature nature) throws CoreException, MisconfigurationException {
        String[] env = createEnvWithPythonpath(pythonPathEnvStr, nature, manager);
        IInterpreterInfo info = manager.getInterpreterInfo(interpreter, new NullProgressMonitor());
        env = info.updateEnv(env);
        return env;
    }
    
    private static String[] createEnvWithPythonpath(String pythonPathEnvStr, IPythonNature nature, IInterpreterManager manager) throws CoreException {
        DebugPlugin defaultPlugin = DebugPlugin.getDefault();
        if(defaultPlugin != null){
            Map<String,String> env = getDefaultSystemEnv(defaultPlugin, nature, false); //no need to remove as it'll be updated        
    
            env.put("PYTHONPATH", pythonPathEnvStr); //put the environment
            switch(manager.getInterpreterType()){
            
                case IPythonNature.INTERPRETER_TYPE_JYTHON:
                    env.put("CLASSPATH", pythonPathEnvStr); //put the environment
                    env.put("JYTHONPATH", pythonPathEnvStr); //put the environment
                    break;
                    
                case IPythonNature.INTERPRETER_TYPE_IRONPYTHON:
                    env.put("IRONPYTHONPATH", pythonPathEnvStr); //put the environment
                    
                break;
            }
            return getMapEnvAsArray(env);
        }else{
            //should only happen in tests.
            return null;
        }
    }

    /**
     * @return an array with the env variables for the system with the format xx=yy  
     */
    public static String[] getDefaultSystemEnvAsArray(IPythonNature nature, boolean removePythonpathFromDefaultEnv) throws CoreException {
        Map<String,String> defaultSystemEnv = getDefaultSystemEnv(nature, removePythonpathFromDefaultEnv);
        if(defaultSystemEnv != null){
            return getMapEnvAsArray(defaultSystemEnv);
        }
        return null;
    }

    /**
     * @return a map with the env variables for the system  
     */
    public static Map<String,String> getDefaultSystemEnv(IPythonNature nature, boolean removePythonpathFromDefaultEnv) throws CoreException {
        DebugPlugin defaultPlugin = DebugPlugin.getDefault();
        return getDefaultSystemEnv(defaultPlugin, nature, removePythonpathFromDefaultEnv);
    }

    /**
     * @return a map with the env variables for the system  
     */
    @SuppressWarnings("unchecked")
    private static Map<String,String> getDefaultSystemEnv(DebugPlugin defaultPlugin, IPythonNature nature, boolean removePythonpathFromDefaultEnv) throws CoreException {
        if(defaultPlugin != null){
            ILaunchManager launchManager = defaultPlugin.getLaunchManager();
    
            // build base environment
            Map<String,String> env = new HashMap<String,String>();
            env.putAll(launchManager.getNativeEnvironment());
            
            // Add variables from config
            boolean win32= REF.isWindowsPlatform();
            for(Iterator iter= env.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry entry= (Map.Entry) iter.next();
                String key= (String) entry.getKey();
                if (win32) {
                    // Win32 vars are case insensitive. Uppercase everything so
                    // that (for example) "pAtH" will correctly replace "PATH"
                    key= key.toUpperCase();
                }
                String value = (String) entry.getValue();
                // translate any string substitution variables
                String translated = value;
                try {
                    StringSubstitution stringSubstitution = new StringSubstitution(nature);
                    translated = stringSubstitution.performStringSubstitution(value, false);
                } catch (Exception e) {
                    Log.log(e);
                }
                env.put(key, translated);
            }
            
            //Always remove PYTHONHOME from the default system env, as it doesn't work well with multiple interpreters.
            env.remove("PYTHONHOME");
            if(removePythonpathFromDefaultEnv){
                env.remove("PYTHONPATH");
            }
            return env;
        }
        return null;
    }

    
    /**
     * copied from org.eclipse.jdt.internal.launching.StandardVMRunner
     * @param args - other arguments to be added to the command line (may be null)
     * @return
     */
    public static String getArgumentsAsStr(String[] commandLine, String ... args) {
        if(args != null && args.length > 0){
            String[] newCommandLine = new String[commandLine.length + args.length];
            System.arraycopy(commandLine, 0, newCommandLine, 0, commandLine.length);
            System.arraycopy(args, 0, newCommandLine, commandLine.length, args.length);
            commandLine = newCommandLine;
        }
        
        
        if (commandLine.length < 1)
            return ""; //$NON-NLS-1$
        FastStringBuffer buf= new FastStringBuffer();
        FastStringBuffer command= new FastStringBuffer();
        for (int i= 0; i < commandLine.length; i++) {
            if(commandLine[i] == null){
                continue; //ignore nulls (changed from original code)
            }
            
            buf.append(' ');
            char[] characters= commandLine[i].toCharArray();
            command.clear();
            boolean containsSpace= false;
            for (int j = 0; j < characters.length; j++) {
                char character= characters[j];
                if (character == '\"') {
                    command.append('\\');
                } else if (character == ' ') {
                    containsSpace = true;
                }
                command.append(character);
            }
            if (containsSpace) {
                buf.append('\"');
                buf.append(command.toString());
                buf.append('\"');
            } else {
                buf.append(command.toString());
            }
        }   
        return buf.toString();
    }   

    /**
     * Creates a string that can be passed as the PYTHONPATH 
     * 
     * @param project the project we want to get the settings from. If it is null, the system pythonpath is returned 
     * @param interpreter this is the interpreter to be used to create the env.
     * @return a string that can be used as the PYTHONPATH env variable
     */
    public static String makePythonPathEnvString(IPythonNature pythonNature, IInterpreterInfo interpreter, IInterpreterManager manager) {
        if(pythonNature == null){
            return makePythonPathEnvFromPaths(new ArrayList<String>()); //no pythonpath can be gotten (set to empty, so that the default is gotten)
        }
        
        List<String> paths;
        
        //if we have a project, get its complete pythonpath
        IPythonPathNature pythonPathNature = pythonNature.getPythonPathNature();
        if(pythonPathNature == null){
            IProject project = pythonNature.getProject();
            String projectName;
            if(project == null){
                projectName = "null?";
            }else{
                projectName = project.getName();
            }
            throw new RuntimeException("The project "+projectName+" does not have the pythonpath configured, \n" +
                    "please configure it correcly (please check the pydev getting started guide at \n" +
                    "http://pydev.org/manual_101_root.html for better information on how to do it).");
        }
        paths = pythonPathNature.getCompleteProjectPythonPath(interpreter, manager);
    
        return makePythonPathEnvFromPaths(paths);
    }

    /**
     * @param paths the paths to be added
     * @return a String suitable to be added to the PYTHONPATH environment variable.
     */
    public static String makePythonPathEnvFromPaths(Collection<String> inPaths) {
        ArrayList<String> paths = new ArrayList<String>(inPaths);
        try {
            //whenever we launch a file from pydev, we must add the sitecustomize to the pythonpath so that
            //the default encoding (for the console) can be set.
            //see: http://sourceforge.net/tracker/index.php?func=detail&aid=1580766&group_id=85796&atid=577329
            
            paths.add(0, REF.getFileAbsolutePath(PydevPlugin.getScriptWithinPySrc("pydev_sitecustomize")));
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
        
        String separator = getPythonPathSeparator();
        StringBuffer pythonpath = new StringBuffer();
        boolean first = true;
        for (String path:paths) {
            if (first){
                first = false;
            }else{
                pythonpath.append(separator);
            }
            
            pythonpath.append(path);
        }
        return pythonpath.toString();
    }
    
    /**
     * @return the separator for the pythonpath variables (system dependent)
     */
    public static String getPythonPathSeparator(){
        return System.getProperty( "path.separator" ); //is system dependent, and should cover for all cases...
//        boolean win32= isWindowsPlatform();
//        String separator = ";";
//        if(!win32){
//            separator = ":"; //system dependent
//        }
//        return separator;
    }

    /**
     * @param env a map that will have its values formatted to xx=yy, so that it can be passed in an exec
     * @return an array with the formatted map
     */
    private static String[] getMapEnvAsArray(Map<String,String> env) {
        List<String> strings= new ArrayList<String>(env.size());
        for(Iterator<Map.Entry<String, String>> iter= env.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, String> entry = iter.next();
            StringBuffer buffer= new StringBuffer(entry.getKey());
            buffer.append('=').append(entry.getValue());
            strings.add(buffer.toString());
        }
        
        return strings.toArray(new String[strings.size()]);
    }


    /**
     * @return a tuple with the process created and a string representation of the cmdarray.
     */
    public Tuple<Process, String> run(String[] cmdarray, File workingDir, IPythonNature nature, IProgressMonitor monitor) {
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }
        String executionString = getArgumentsAsStr(cmdarray);
        monitor.setTaskName("Executing: "+executionString);
        monitor.worked(5);
        Process process = null;
        try {
            monitor.setTaskName("Making pythonpath environment..."+executionString);
            String[] envp = null;
            if(nature != null){
                envp = getEnvironment(nature, nature.getProjectInterpreter(), nature.getRelatedInterpreterManager(), false); //Don't remove as it *should* be updated based on the nature)
            }
            //Otherwise, use default (used when configuring the interpreter for instance).
            monitor.setTaskName("Making exec..."+executionString);
            if(workingDir != null){
                if(!workingDir.isDirectory()){
                    throw new RuntimeException(StringUtils.format("Working dir must be an existing directory (received: %s)", workingDir));
                }
            }
            process = createProcess(cmdarray, envp, workingDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Tuple<Process, String>(process, executionString);
    }
    
    /**
     * Runs the given command line and returns a tuple with the output (stdout and stderr) of executing it.
     * 
     * @param cmdarray array with the commands to be passed to Runtime.exec
     * @param workingDir the working dir (may be null)
     * @param project the project (used to get the pythonpath and put it into the environment) -- if null, no environment is passed.
     * @param monitor the progress monitor to be used -- may be null
     * 
     * @return a tuple with stdout and stderr
     */
    public Tuple<String, String> runAndGetOutput(String[] cmdarray, File workingDir, IPythonNature nature, IProgressMonitor monitor) {
        Tuple<Process, String> r = run(cmdarray, workingDir, nature, monitor);
        
        return getProcessOutput(r.o1, r.o2, monitor);
    }

    /**
     * @param process process from where the output should be gotten
     * @param executionString string to execute (only for errors)
     * @param monitor monitor for giving progress
     * @return a tuple with the output of stdout and stderr
     */
    public static Tuple<String, String> getProcessOutput(Process process,
            String executionString, IProgressMonitor monitor) {
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }
        if (process != null) {

            try {
                process.getOutputStream().close(); //we won't write to it...
            } catch (IOException e2) {
            }

            monitor.setTaskName("Reading output...");
            monitor.worked(5);
            //No need to synchronize as we'll waitFor() the process before getting the contents.
            ThreadStreamReader std = new ThreadStreamReader(process.getInputStream(), false);
            ThreadStreamReader err = new ThreadStreamReader(process.getErrorStream(), false);

            std.start();
            err.start();
            
            boolean interrupted = true;
            while(interrupted){
                interrupted = false;
                try {
                    monitor.setTaskName("Waiting for process to finish.");
                    monitor.worked(5);
                    process.waitFor(); //wait until the process completion.
                } catch (InterruptedException e1) {
                    interrupted = true;
                }
            }

            try {
                //just to see if we get something after the process finishes (and let the other threads run).
            	Object sync = new Object();
                synchronized (sync) {
                    sync.wait(50);
                }
            } catch (Exception e) {
                //ignore
            }
            return new Tuple<String, String>(std.getContents(), err.getContents());
            
        } else {
            try {
                throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error creating process - got null process("
                        + executionString + ")", new Exception("Error creating process - got null process.")));
            } catch (CoreException e) {
                PydevPlugin.log(IStatus.ERROR, e.getMessage(), e);
            }

        }
        return new Tuple<String, String>("","Error creating process - got null process("+ executionString + ")"); //no output
    }



    /**
     * @param pythonpath the pythonpath string to be used 
     * @return a list of strings with the elements of the pythonpath
     */
    public static List<String> splitPythonpath(String pythonpath) {
        ArrayList<String> splitted = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(pythonpath, getPythonPathSeparator());
        while(tokenizer.hasMoreTokens()){
            splitted.add(tokenizer.nextToken());
        }
        return splitted;
        
    }

}
