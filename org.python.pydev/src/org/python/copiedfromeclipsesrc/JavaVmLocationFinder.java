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

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
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
     * Convenience handle to the system-specific file separator character
     */                                                         
    private static final char fgSeparator = File.separatorChar;

    /**
     * The list of locations in which to look for the java executable in candidate
     * VM install locations, relative to the VM install location.
     */
    private static final String[] fgCandidateJavaLocations = {
                            "bin" + fgSeparator + "javaw",                                //$NON-NLS-2$ //$NON-NLS-1$
                            "bin" + fgSeparator + "javaw.exe",                            //$NON-NLS-2$ //$NON-NLS-1$
                            "jre" + fgSeparator + "bin" + fgSeparator + "javaw",          //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
                            "jre" + fgSeparator + "bin" + fgSeparator + "javaw.exe",      //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$                                 
                            "bin" + fgSeparator + "java",                                 //$NON-NLS-2$ //$NON-NLS-1$
                            "bin" + fgSeparator + "java.exe",                             //$NON-NLS-2$ //$NON-NLS-1$
                            "jre" + fgSeparator + "bin" + fgSeparator + "java",           //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
                            "jre" + fgSeparator + "bin" + fgSeparator + "java.exe"};      //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$                         
    
    /**
     * Starting in the specified VM install location, attempt to find the 'java' executable
     * file.  If found, return the corresponding <code>File</code> object, otherwise return
     * <code>null</code>.
     */
    private static File findJavaExecutable(File vmInstallLocation) {
        
        // Try each candidate in order.  The first one found wins.  Thus, the order
        // of fgCandidateJavaLocations is significant.
        for (int i = 0; i < fgCandidateJavaLocations.length; i++) {
            File javaFile = new File(vmInstallLocation, fgCandidateJavaLocations[i]);
            if (javaFile.isFile()) {
                return javaFile;
            }
        }       
        return null;                            
    }
    
    /**
     * @return the default java executable configured in the jdt plugin
     */
    public static File findDefaultJavaExecutable(){
        return (File) callbackJavaExecutable.call(null);
    }

    /**
     * might be changed for tests (if not in the eclipse env)
     */
    public static ICallback callbackJavaExecutable = new ICallback(){
        
        public Object call(Object args) {
            IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
            File installLocation = defaultVMInstall.getInstallLocation();
            return findJavaExecutable(installLocation);
        }
        
    };
    
    
    
    
    /**
     * @return the default java jars (rt.jar ... )
     */
    public static List<File> findDefaultJavaJars(){
        return (List<File>) callbackJavaJars.call(null);
    }
    
    /**
     * might be changed for tests (if not in the eclipse env)
     */
    public static ICallback callbackJavaJars = new ICallback(){

        public Object call(Object args) {
            IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
            LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(defaultVMInstall);
            
            ArrayList<File> jars = new ArrayList<File>();
            for (LibraryLocation location : libraryLocations) {
                jars.add(location.getSystemLibraryPath().toFile());
            }
            return jars;
        }
        
    };
}
