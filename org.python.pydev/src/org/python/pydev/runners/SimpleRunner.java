/*
 * Created on 05/08/2005
 */
package org.python.pydev.runners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.service.environment.Constants;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.IPythonPathNature;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class SimpleRunner {

    /**
     * Just execute the string. does nothing else...
     */
    public Process createProcess(String executionString, File workingDir) throws IOException {
        return Runtime.getRuntime().exec(executionString, null, workingDir);
    }

    /**
     * THIS CODE IS COPIED FROM org.eclipse.debug.internal.core.LaunchManager
     * 
     * changed so that we always set the PYTHONPATH in the environment
     * 
     * @param configuration
     * @return
     * @throws CoreException
     */
    public String[] getEnvironment(IProject project) throws CoreException {
        if(project == null){ //no associated project... just get the default env
            return getDefaultSystemEnvAsArray();
        }
        
        String pythonPathEnvStr = "";
    	try {
            if (PydevPlugin.getInterpreterManager().hasInfoOnDefaultInterpreter()){ //check if we have a default interpreter.
                pythonPathEnvStr = makePythonPathEnvString(project);
            }
        } catch (Exception e) {
            return null; //we cannot get it
        }
    
    	DebugPlugin defaultPlugin = DebugPlugin.getDefault();
    	if(defaultPlugin != null){
            Map<String,String> env = getDefaultSystemEnv(defaultPlugin);		
    
    		env.put("PYTHONPATH", pythonPathEnvStr); //put the environment
    		return getMapEnvAsArray(env);
    	}else{
    	    return null;
    	}
    }

    public String[] getDefaultSystemEnvAsArray() throws CoreException {
        Map defaultSystemEnv = getDefaultSystemEnv();
        if(defaultSystemEnv != null){
            return getMapEnvAsArray(defaultSystemEnv);
        }
        return null;
    }

    public Map getDefaultSystemEnv() throws CoreException {
        DebugPlugin defaultPlugin = DebugPlugin.getDefault();
        return getDefaultSystemEnv(defaultPlugin);
    }

    /**
     * @param defaultPlugin
     * @return
     * @throws CoreException
     */
    public Map<String,String> getDefaultSystemEnv(DebugPlugin defaultPlugin) throws CoreException {
        if(defaultPlugin != null){
            ILaunchManager launchManager = defaultPlugin.getLaunchManager();
    
            // build base environment
            Map<String,String> env = new HashMap<String,String>();
            env.putAll(launchManager.getNativeEnvironment());
            
            // Add variables from config
            boolean win32= isWindowsPlatform();
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
            	String translated = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
            	env.put(key, translated);
            }
            return env;
        }
        return null;
    }

    /**
     * @return whether we are in windows or not
     */
    public static boolean isWindowsPlatform() {
        return Platform.getOS().equals(Constants.OS_WIN32);
    }

    /**
     * Formats the script to the windows environment (if needed), adding '"' to the start and end of the script
     * 
     * @param param the parameter that might be formatted
     * 
     * @return the formatted parameter
     */
    protected String formatParamToExec(String param) {
        if(isWindowsPlatform()){ //in windows, we have to put python "path_to_file.py"
            if(param.startsWith("\"") == false){
                param = "\""+param+"\"";
            }
        }
        return param;
    }

    /**
     * @param project
     * @return
     */
    public String makePythonPathEnvString(IProject project) {
        List paths;
        if(project != null){
            //if we have a project, get its complete pythonpath
            IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
        	paths = pythonPathNature.getCompleteProjectPythonPath();
        }else{
            //otherwise, get the system pythonpath
            InterpreterInfo info = PydevPlugin.getInterpreterManager().getDefaultInterpreterInfo(new NullProgressMonitor());
            paths = new ArrayList(info.libs);
        }
    
        boolean win32= isWindowsPlatform();
        String separator = ";";
        if(!win32){
            separator = ":"; //system dependent
        }
    
    	StringBuffer pythonpath = new StringBuffer();
        for (int i = 0; i < paths.size(); i++) {
    		if (i > 0){
    			pythonpath.append(separator);
    		}
    		pythonpath.append(REF.getFileAbsolutePath(new File((String) paths.get(i))));
    	}
        return pythonpath.toString();
    }

    /**
     * @param env a map that will have its values formatted to xx=yy, so that it can be passed in an exec
     */
    private String[] getMapEnvAsArray(Map env) {
        List<String> strings= new ArrayList<String>(env.size());
    	for(Iterator iter= env.entrySet().iterator(); iter.hasNext(); ) {
    		Map.Entry entry = (Map.Entry) iter.next();
    		StringBuffer buffer= new StringBuffer((String) entry.getKey());
    		buffer.append('=').append((String) entry.getValue());
    		strings.add(buffer.toString());
    	}
    	
    	return strings.toArray(new String[strings.size()]);
    }

}
