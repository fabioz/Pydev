/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.plugin.PydevPlugin;

/**
 * This is not a singleton because we may have a different pythonpath for each project (even though
 * we have a default one as the original pythonpath).
 * 
 * @author Fabio Zadrozny
 */
public class PythonPathHelper implements Serializable{
    
    /**
     * This is a list of Files containg the pythonpath.
     */
    public List pythonpath = new ArrayList();
    
    /**
     * These are the modules that we have.
     * Instances should be subclasses of AbstractModule.
     */
    public List modules = new ArrayList();
    
    /**
     * Returns the default path given from the string.
     * @param str
     * @param acceptPoint
     * @return
     */
    public String getDefaultPathStr(String str, boolean acceptPoint){
        if(str.indexOf(".") != -1 && acceptPoint == false){
            throw new RuntimeException("The pythonpath can only have full paths without . or ..");
        }
        return str.trim().replaceAll("\\\\","/");//.toLowerCase();
    }
    
    public String getDefaultPathStr(String str){
        return getDefaultPathStr(str, false);
    }
    
    /**
     * This method returns all modules that can be obtained from a root File.
     * @return the files in position 0 and folders in position 1.
     */
    public List[] getModulesBelow(File root){
        FileFilter filter = new FileFilter() {
            
	        public boolean accept(File pathname) {
	            if(pathname.isFile()){
	                return isValidFileMod(pathname.getAbsolutePath());
	            }else if(pathname.isDirectory()){
	                return isFileOrFolderWithInit(pathname);
	            }else{
	                return false;
	            }
	        }
	
	    };
	    return PydevPlugin.getPyFilesBelow(root, filter, null);
    }
    

    /**
     * 
     * @param path
     * @return if the paths maps to a valid python module (depending on its extension).
     */
    public boolean isValidFileMod(String path){
        if(path.endsWith(".py")   || 
//           path.endsWith(".pyc")  || - we don't want pyc files, only source files and compiled extensions
           path.endsWith(".pyd")  ||
           path.endsWith(".dll")  
//           path.endsWith(".pyo") - we don't want pyo files, only source files and compiled extensions
           ){
             return true;
        }
        return false;
    }
    
    /**
     * DAMN... when I started thinking this up, it seemed much better... (and easier)
     * 
     * @param module - this is the full path of the module. Only for directories or py,pyd,dll,pyo files.
     * @return a String with the module that the file or folder should represent.
     */
    public String resolveModule(String fullPath){
        fullPath = getDefaultPathStr(fullPath, true);
        File moduleFile = new File(fullPath);
        if(moduleFile.exists() == false){
            return null;
        }
        if(moduleFile.isFile()){
            
            if(isValidFileMod(moduleFile.getAbsolutePath()) == false){
                return null;
            }
        }
        
        //go through our pythonpath and check the beggining
        for (Iterator iter = pythonpath.iterator(); iter.hasNext();) {
            
            String element = (String) iter.next();
            if(fullPath.startsWith(element)){
                String s = fullPath.substring(element.length());
                if(s.startsWith("/")){
                    s = s.substring(1);
                }
                s = s.replaceAll("/",".");
                
            
                //if it is a valid module, let's find out if it exists...
                if(isValidModule(s)){
                    if(s.indexOf(".") != -1){
                        File root = new File(element);
                        if(root.exists() == false){
                            continue;
                        }
                        
                        //this means that more than 1 module is specified, so, in order to get it,
                        //we have to go and see if all the folders to that module have __init__.py in it...
                        String[] modulesParts = s.split("\\.");
                        
                        if(modulesParts.length > 1 && moduleFile.isFile()){
                            String[] t = new String[modulesParts.length -1];
                            
                            for (int i = 0; i < modulesParts.length-1; i++) {
                                t[i] = modulesParts[i];
                            }
                            t[t.length -1] = t[t.length -1]+"."+modulesParts[modulesParts.length-1];
                            modulesParts = t;
                        }
                        
                        //here, in modulesParts, we have something like 
                        //["compiler", "ast.py"] - if file
                        //["pywin","debugger"] - if folder
                        //
                        //root starts with the pythonpath folder that starts with the same
                        //chars as the full path passed in.
                        boolean isValid = true;
                        for (int i = 0; i < modulesParts.length && root != null; i++) {
                            
                            //check if file is in root...
                            if(isValidFileMod(modulesParts[i])){
                                root = new File(root.getAbsolutePath() + "/" + modulesParts[i]);
                                if(root.exists() && root.isFile()){
                                    break;
                                }
                                
                            }else{
                                //this part is a folder part... check if it is a valid module (has init).
	                            root = new File(root.getAbsolutePath() + "/" + modulesParts[i]);
	                            if(isFileOrFolderWithInit(root) == false){
	                                isValid = false;
	                                break;
	                            }
	                            //go on and check the next part.
                            }                            
                        }
                        if(isValid){
                            if(moduleFile.isFile()){
                                s = stripExtension(s);
                            }
	                        return s;
                        }
                    }else{
                        //simple part, we don't have to go into subfolders to check validity...
                        if(moduleFile.isFile()){
                            throw new RuntimeException("This should never happen... if it is a file, it always has a dot, so, this should not happen...");
                        }else if (moduleFile.isDirectory() && isFileOrFolderWithInit(moduleFile) == false){
                            return null;
                        }
                        return s;
                    }
                }
            }
            
        }
        return null;
    }

    /**
     * Note that this function is not completely safe...beware when using it.
     * @param s
     * @return
     */
    private String stripExtension(String s) {
        String[] strings = s.split("\\.");
        return s.substring(0, s.length() - strings[strings.length -1].length() -1);
    }

    /**
     * @param root
     * @param string
     * @return
     */
    private boolean isFileOrFolderWithInit(File root) {
        if(root.isDirectory() == false){
            return true;
        }
        
        File[] files = root.listFiles();
        
        if(files == null){
            return false;
        }
        
        //check for an __init__.py
        String[] items = root.list();
        for (int j = 0; j < items.length; j++) {
            if(items[j].toLowerCase().equals("__init__.py")){
                return true;
            }
        }
        
        return false;
    }

    /**
     * @param s
     * @return
     */
    private boolean isValidModule(String s) {
        return s.indexOf("-") == -1;
    }

    /**
     * @param string
     * @return
     */
    public List setPythonPath(String string) {
        String[] strings = string.split("\\|");
        for (int i = 0; i < strings.length; i++) {
            pythonpath.add(getDefaultPathStr(strings[i]));
        }
        return Arrays.asList(strings);
    }
}
