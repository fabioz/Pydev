/*
 * License: Common Public License v1.0
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.copiedfromeclipsesrc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.ICallback;

/**
 * copied from org.eclipse.jdt.internal.launching.StandardVMType
 * 
 * @author Fabio Zadrozny
 */
public class JavaVmLocationFinder {

    private JavaVmLocationFinder() {
        super();
    }

    
    /**
     * @return the default java executable configured in the jdt plugin
     */
    public static File findDefaultJavaExecutable() throws JDTNotAvailableException{
        try {
            return (File) callbackJavaExecutable.call(null);
        } catch (Exception e) {
            JavaVmLocationFinder.handleException(e);
            throw new RuntimeException("Should never get here", e);
        }
    }
    
    
    /**
     * @return the default java jars (rt.jar ... )
     */
    @SuppressWarnings("unchecked")
    public static List<File> findDefaultJavaJars() throws JDTNotAvailableException{
        try {
            return (List<File>) callbackJavaJars.call(null);
        } catch (Exception e) {
            JavaVmLocationFinder.handleException(e);
            throw new RuntimeException("Should never get here", e);
        }
    }
    
    

    /**
     * Might be changed for tests (if not in the eclipse env)
     */
    public static ICallback callbackJavaExecutable = new ICallback(){
        
        public Object call(Object args) throws Exception {
            try{
                IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
                File installLocation = defaultVMInstall.getInstallLocation();
                return StandardVMType.findJavaExecutable(installLocation);
            }catch(Throwable e){
                handleException(e);
                throw new RuntimeException("Should never get here", e);
            }
        }
    };
    
    /**
     * Might be changed for tests (if not in the eclipse env)
     */
    public static ICallback callbackJavaJars = new ICallback(){

        public Object call(Object args) throws Exception {
            try{
                IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
                LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(defaultVMInstall);
                
                ArrayList<File> jars = new ArrayList<File>();
                for (LibraryLocation location : libraryLocations) {
                    jars.add(location.getSystemLibraryPath().toFile());
                }
                return jars;
            }catch(Throwable e){
                JavaVmLocationFinder.handleException(e);
                throw new RuntimeException("Should never get here", e);
            }
        }
    };

    /**
     * Handles the exception and re-throws it as a JDTNotAvailableException (if it was a LinkageError or a 
     * ClassNotFoundException or a JDTNotAvailableException) or creates a RuntimeException and throws this exception
     * encapsulating the previous one
     * 
     * @param e the exception that should be transformed to a JDTNotAvailableException (if possible)
     * @throws JDTNotAvailableException
     */
    private static void handleException(Throwable e) throws JDTNotAvailableException {
        if(e instanceof LinkageError || e instanceof ClassNotFoundException){
            throw new JDTNotAvailableException();
            
        }else if(e instanceof JDTNotAvailableException){
            JDTNotAvailableException jdtNotAvailableException = (JDTNotAvailableException) e;
            throw jdtNotAvailableException;
            
        }else if(e instanceof RuntimeException){
            RuntimeException runtimeException = (RuntimeException) e;
            throw runtimeException;
        }
        
        PydevPlugin.log(e);
        throw new RuntimeException(e);
    }
}

