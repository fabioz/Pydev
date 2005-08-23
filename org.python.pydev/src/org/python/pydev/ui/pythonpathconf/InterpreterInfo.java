/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.plugin.PydevPlugin;


public class InterpreterInfo implements Serializable{
    
    /**
     * check note on http://java.sun.com/j2se/1.5.0/docs/guide/serialization/spec/version.html#6678
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * For jython, this is the jython.jar
     * 
     * For python, this is the path to the python executable 
     */
    public String executableOrJar; 
    
    /**
     * folders - they should be passed to the pythonpath
     */
    public java.util.List<String> libs = new ArrayList<String>(); 
    
    /**
     * Those libraries are not really used in python (they are found in the system pythonpath), and 
     * don't need to be passed in the pythonpath (they are only here so that the user can see
     * this information)
     *  
     * files: .pyd, .dll, etc.
     * 
     * for jython, the jars should appear here.
     */
    public java.util.List dllLibs = new ArrayList(); 
    
    /**
     * __builtin__, os, math, etc for python 
     * 
     * check sys.builtin_module_names and others that should
     * be forced to use code completion as builtins, such os, math, etc.
     * 
     * for jython, this should 
     */
    public Set forcedLibs = new TreeSet(); 
    
    /**
     * module management for the system is always binded to an interpreter (binded in this class)
     */
    public SystemModulesManager modulesManager = new SystemModulesManager(forcedLibs);
    
    public InterpreterInfo(String exe, Collection libs0){
        this.executableOrJar = exe;
        libs.addAll(libs0);
    }
    
    public InterpreterInfo(String exe, Collection libs0, Collection dlls){
        this(exe, libs0);
        dllLibs.addAll(dlls);
    }
    
    public InterpreterInfo(String exe, List libs0, List dlls, List forced) {
        this(exe, libs0, dlls);
        forcedLibs.addAll(forced);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (!(o instanceof InterpreterInfo)){
            return false;
        }

        InterpreterInfo info = (InterpreterInfo) o;
        if(info.executableOrJar.equals(this.executableOrJar) == false){
            return false;
        }
        
        if(info.libs.equals(this.libs) == false){
            return false;
        }
        
        if(info.dllLibs.equals(this.dllLibs) == false){
            return false;
        }
        
        if(info.forcedLibs.equals(this.forcedLibs) == false){
            return false;
        }
        
        return true;
    }

    /**
     * Format we receive should be:
     * 
     * Executable:python.exe|lib1|lib2|lib3@dll1|dll2|dll3$forcedBuitin1|forcedBuiltin2
     * 
     * Symbols ': @ $'
     */
    public static InterpreterInfo fromString(String received) {
        String[] forcedSplit = received.split("\\$");
        String[] libsSplit = forcedSplit[0].split("\\@");
        String exeAndLibs = libsSplit[0];
        
        
        
        String[] exeAndLibs1 = exeAndLibs.split("\\|");
        String executable = exeAndLibs1[0].substring(exeAndLibs1[0].indexOf(":")+1, exeAndLibs1[0].length());
        ArrayList l = new ArrayList();
        for (int i = 1; i < exeAndLibs1.length; i++) { //start at 1 (o is exe)
            String trimmed = exeAndLibs1[i].trim();
            if(trimmed.length() > 0){
                l.add(trimmed);
            }
        }

        ArrayList l1 = new ArrayList();
        if(libsSplit.length > 1){
	        String dllLibs = libsSplit[1];
	        String[] dllLibs1 = dllLibs.split("\\|");
	        for (int i = 0; i < dllLibs1.length; i++) {
	            String trimmed = dllLibs1[i].trim();
	            if(trimmed.length() > 0){
	                l1.add(trimmed);
	            }
	        }
        }
	        
        ArrayList l2 = new ArrayList();
        if(forcedSplit.length > 1){
	        String forcedLibs = forcedSplit[1];
	        String[] forcedLibs1 = forcedLibs.split("\\|");
	        for (int i = 0; i < forcedLibs1.length; i++) {
	            String trimmed = forcedLibs1[i].trim();
	            if(trimmed.length() > 0){
	                l2.add(trimmed);
	            }
	        }
        }	            
        return new InterpreterInfo(executable, l, l1, l2);
	        
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Executable:");
        buffer.append(executableOrJar);
        for (Iterator iter = libs.iterator(); iter.hasNext();) {
            buffer.append("|");
            buffer.append(iter.next().toString());
        }
        buffer.append("@");
        if(dllLibs.size() > 0){
	        for (Iterator iter = dllLibs.iterator(); iter.hasNext();) {
	            buffer.append("|");
	            buffer.append(iter.next().toString());
	        }
        }
        
        buffer.append("$");
        if(forcedLibs.size() > 0){
	        for (Iterator iter = forcedLibs.iterator(); iter.hasNext();) {
	            buffer.append("|");
	            buffer.append(iter.next().toString());
	        }
        }
        
        return buffer.toString();
    }

    /**
     * Adds the compiled libs (dlls)
     */
    public void restoreCompiledLibs(IProgressMonitor monitor) {
        FileFilter filter = new FileFilter() {
            
	        public boolean accept(File pathname) {
	            if(pathname.isFile()){
	                return PythonPathHelper.isValidDll(REF.getFileAbsolutePath(pathname));
	            }else{
	                return false;
	            }
	        }
	
	    };

	    List dlls = new ArrayList();
	    for (Iterator iter = libs.iterator(); iter.hasNext();) {
            String folder = iter.next().toString();
            
            List[] below = PydevPlugin.getPyFilesBelow(new File(folder), filter, monitor, false);
            dlls.addAll(below[0]);
        }
	    
	    dllLibs.clear();
	    for (Iterator iter = dlls.iterator(); iter.hasNext();) {
            File f = (File) iter.next();
            
            this.dllLibs.add(REF.getFileAbsolutePath(f));
        }
	    
	    //the compiled with the interpreter should be already gotten.
	    forcedLibs.add("os"); //we have it in source, but want to interpret it, source info (ast) does not give us much
	    forcedLibs.add("__builtin__"); //just to be sure of it... (it's a set, so, adding twice should be no problem).
        

        if(isJythonInfo()){
            //by default, we don't want to force anything to python.
        }else{
            //those are sources, but we want to get runtime info on them.
            forcedLibs.add("OpenGL");
            forcedLibs.add("wxPython");
        }
        
    }

    /**
     * Restores the path given non-standard libraries
     * @param path
     */
    public void restorePythonpath(String path, IProgressMonitor monitor) {
        //no managers involved here...
        modulesManager.setBuiltins(forcedLibs);
        modulesManager.changePythonPath(path, null, monitor);
    }
    
    /**
     * Restores the path with the discovered libs
     * @param path
     */
    public void restorePythonpath(IProgressMonitor monitor) {
        StringBuffer buffer = new StringBuffer();
        for (Iterator iter = libs.iterator(); iter.hasNext();) {
            String folder = (String) iter.next();
            buffer.append(folder);
            buffer.append("|");
        }
        restorePythonpath(buffer.toString(), monitor);
    }
    
    /**
     * @return whether this info belongs to jython 
     */
    public boolean isJythonInfo() {
        return isJythonExecutable(executableOrJar);
    }

    /**
     * @param executable the executable we want to know about
     * @return if the executable is the jython jar.
     */
    public static boolean isJythonExecutable(String executable) {
        return executable.endsWith(".jar");
    }
    
    
}