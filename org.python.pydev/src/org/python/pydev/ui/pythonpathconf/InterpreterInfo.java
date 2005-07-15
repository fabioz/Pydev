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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.ref.REF;


public class InterpreterInfo implements Serializable{
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public String executable;
    public java.util.List libs = new ArrayList(); //folders
    public java.util.List dllLibs = new ArrayList(); //.pyd, .dll, etc.
    public Set forcedLibs = new HashSet(); //__builtin__, os, math
    
    /**
     * module management for the system is always binded to an interpreter (binded in this class)
     */
    public SystemModulesManager modulesManager = new SystemModulesManager(forcedLibs);
    
    public InterpreterInfo(String exe, Collection libs0){
        this.executable = exe;
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
        if(info.executable.equals(this.executable) == false){
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

        if(libsSplit.length > 1){
	        String dllLibs = libsSplit[1];
	        String[] dllLibs1 = dllLibs.split("\\|");
	        ArrayList l1 = new ArrayList();
	        for (int i = 0; i < dllLibs1.length; i++) {
	            String trimmed = dllLibs1[i].trim();
	            if(trimmed.length() > 0){
	                l1.add(trimmed);
	            }
	        }
	        
	        
	        if(forcedSplit.length > 1){
		        String forcedLibs = forcedSplit[1];
		        String[] forcedLibs1 = forcedLibs.split("\\|");
		        ArrayList l2 = new ArrayList();
		        for (int i = 0; i < forcedLibs1.length; i++) {
		            String trimmed = forcedLibs1[i].trim();
		            if(trimmed.length() > 0){
		                l2.add(trimmed);
		            }
		        }
	            
	            return new InterpreterInfo(executable, l, l1, l2);
	        
	        }else{
	            return new InterpreterInfo(executable, l, l1);
	        }
        }else{
            return new InterpreterInfo(executable, l);
        }
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Executable:");
        buffer.append(executable);
        buffer.append("|");
        for (Iterator iter = libs.iterator(); iter.hasNext();) {
            buffer.append(iter.next().toString());
            buffer.append("|");
        }
        if(dllLibs.size() > 0){
	        buffer.append("@");
	        for (Iterator iter = dllLibs.iterator(); iter.hasNext();) {
	            buffer.append(iter.next().toString());
	            buffer.append("|");
	        }
        }
        
        if(forcedLibs.size() > 0){
	        buffer.append("$");
	        for (Iterator iter = forcedLibs.iterator(); iter.hasNext();) {
	            buffer.append(iter.next().toString());
	            buffer.append("|");
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
	    
	    forcedLibs.clear();
	    forcedLibs.add("os");
	    forcedLibs.add("math");
	    forcedLibs.add("__builtin__");
	    forcedLibs.add("sys");
	    forcedLibs.add("datetime");
	    forcedLibs.add("OpenGL");
	    forcedLibs.add("wxPython");
	    forcedLibs.add("itertools");
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
}