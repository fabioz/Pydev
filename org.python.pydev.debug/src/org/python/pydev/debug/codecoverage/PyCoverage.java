/*
 * Created on Oct 7, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * This class is used to make the code coverage.
 * 
 * It works in this way: when the user requests the coverage for the execution of a module, we create a python 
 * process and execute the module using the code coverage module that is packed with pydev.
 * 
 * Other options are:
 * - Erasing the results obtained;
 * - Getting the results when requested (cached in this class).
 * 
 * @author Fabio Zadrozny
 */
public class PyCoverage {
    
    private PyCoverage(){
        
    }
    
    private static PyCoverage pyCoverage;
    
    /**
     * @return Returns the pyCoverage.
     */
    public static PyCoverage getPyCoverage() {
        if(pyCoverage == null){
            pyCoverage = new PyCoverage();
        }
        return pyCoverage;
    }

    
    public static String getCoverageFileLocation(){
        try {
            File pySrcPath = PydevDebugPlugin.getPySrcPath();
            return pySrcPath.getAbsolutePath()+"/.coverage";
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param envp
     * @return
     */
    public static String[] setCoverageFileEnviromentVariable(String[] envp) {
        boolean added = false;
    
        for (int i = 0; i < envp.length; i++) {
            
            if (envp[i].startsWith("COVERAGE_FILE")){
                envp[i] = "COVERAGE_FILE="+getCoverageFileLocation();
                added = true;
            }
            
        }
        if (!added){
            List list = Arrays.asList(envp);
            list.add("COVERAGE_FILE="+getCoverageFileLocation());
            envp = (String[]) list.toArray(new String[0]);
        }
        return envp;
    }


    
    
    
    
}
