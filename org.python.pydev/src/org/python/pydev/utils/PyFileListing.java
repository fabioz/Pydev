package org.python.pydev.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;

/**
 * Helper class for finding out about python files below some source folder.
 * 
 * @author Fabio
 */
public class PyFileListing {
    
    /**
     * Class containing the information we discovered when searching for valid files beneath some folder.
     * 
     * @author Fabio
     */
    public static class PyFileListingInfo{
        
        /**
         * The files we found as being valid for the given filter
         */
        public List<File> filesFound = new ArrayList<File>();
        
        /**
         * The folders we found as being valid for the given filter
         */
        public List<File> foldersFound = new ArrayList<File>();
        
        /**
         * The relative path (composed with the parent folder names) where the file was found 
         * (from the root passed) -- we should be able to determine the name of the module
         * of the corresponding file in the filesFound from this info. 
         * 
         * For each file found a string should be added in this list (so, the same path will be added 
         * multiple times for different files).
         */
        public List<String> fileParentPathNamesRelativeToRoot = new ArrayList<String>();
        
        /**
         * Add the info from the passed listing to this one.
         */
        public void extendWith(PyFileListingInfo other) {
            filesFound.addAll(other.filesFound);
            foldersFound.addAll(other.foldersFound);
            
            fileParentPathNamesRelativeToRoot.addAll(other.fileParentPathNamesRelativeToRoot);
        };
    }

    /**
     * Returns the directories and python files in a list.
     * 
     * @param file
     * @param addSubFolders: indicates if sub-folders should be added
     * @return tuple with files in pos 0 and folders in pos 1
     */
    @SuppressWarnings("unchecked")
    public static PyFileListingInfo getPyFilesBelow(File file, FileFilter filter, IProgressMonitor monitor, boolean addSubFolders, 
            int level, boolean checkHasInit, String currModuleRep) {
        

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        
        PyFileListingInfo ret = new PyFileListingInfo();
    
        if (file != null && file.exists()) {
            //only check files that actually exist
    
            if (file.isDirectory()) {
                if(level != 0){
                    FastStringBuffer newModuleRep = new FastStringBuffer(currModuleRep, 128); 
                    if(newModuleRep.length() != 0){
                        newModuleRep.append(".");
                    }
                    newModuleRep.append(file.getName());
                    currModuleRep = newModuleRep.toString();
                }
                
                File[] files = null;
    
                if (filter != null) {
                    files = file.listFiles(filter);
                } else {
                    files = file.listFiles();
                }
    
                boolean hasInit = false;
    
                List<File> foldersLater = new LinkedList<File>();
                
                for (int i = 0; i < files.length; i++) {
                    File file2 = files[i];
                    
                    if(file2.isFile()){
                        ret.filesFound.add(file2);
                        ret.fileParentPathNamesRelativeToRoot.add(currModuleRep);

                        monitor.worked(1);
                        monitor.setTaskName("Found:" + file2.toString());
                        
                        if (checkHasInit && hasInit == false){
                            //only check if it has __init__ if really needed
    	                    if(PythonPathHelper.isValidInitFile(file2.getName())){
    	                        hasInit = true;
    	                    }
                        }
                        
                    }else{
                        foldersLater.add(file2);
                    }
                }
                
                if(!checkHasInit || hasInit || level == 0){
                    ret.foldersFound.add(file);
    
                    for (Iterator iter = foldersLater.iterator(); iter.hasNext();) {
                        File file2 = (File) iter.next();
                        if(file2.isDirectory() && addSubFolders){
                            
    	                    ret.extendWith(getPyFilesBelow(file2, filter, monitor, addSubFolders, level+1, 
    	                            checkHasInit, currModuleRep));
    	                    
    	                    monitor.worked(1);
                        }
                    }
                }
    
                
            } else if (file.isFile()) {
                ret.fileParentPathNamesRelativeToRoot.add(currModuleRep);
                ret.filesFound.add(file);
                
            } else{
                throw new RuntimeException("Not dir nor file... what is it?");
            }
        }
        
        return ret;
    }

    public static PyFileListingInfo getPyFilesBelow(File file, FileFilter filter, IProgressMonitor monitor, boolean addSubFolders, boolean checkHasInit) {
        return getPyFilesBelow(file, filter, monitor, addSubFolders, 0, checkHasInit, "");
    }

    public static PyFileListingInfo getPyFilesBelow(File file, FileFilter filter, IProgressMonitor monitor, boolean checkHasInit) {
        return getPyFilesBelow(file, filter, monitor, true, checkHasInit);
    }

    /**
     * @param includeDirs determines if we can include subdirectories
     * @return a file filter only for python files (and other dirs if specified)
     */
    public static FileFilter getPyFilesFileFilter(final boolean includeDirs) {
    	return new FileFilter() {
    
            public boolean accept(File pathname) {
                if (includeDirs){
                	if(pathname.isDirectory()){
                		return true;
                	}
                	if(PythonPathHelper.isValidSourceFile(pathname.toString())){
                		return true;
                	}
                	return false;
                }else{
                	if(pathname.isDirectory()){
                		return false;
                	}
                	if(PythonPathHelper.isValidSourceFile(pathname.toString())){
                		return true;
                	}
                	return false;
                }
            }
    
        };
    }

    /**
     * Returns the directories and python files in a list.
     * 
     * @param file
     * @return tuple with files in pos 0 and folders in pos 1
     */
    public static PyFileListingInfo getPyFilesBelow(File file, IProgressMonitor monitor, final boolean includeDirs, boolean checkHasInit) {
        FileFilter filter = getPyFilesFileFilter(includeDirs);
        return getPyFilesBelow(file, filter, monitor, true, checkHasInit);
    }

    /**
     * @return All the IFiles below the current folder that are python files (does not check if it has an __init__ path)
     */
    public static List<IFile> getAllIFilesBelow(IFolder member) {
    	final ArrayList<IFile> ret = new ArrayList<IFile>();
    	try {
    		member.accept(new IResourceVisitor(){
    
    			public boolean visit(IResource resource) {
    				if(resource instanceof IFile){
    					ret.add((IFile) resource);
    					return false; //has no members
    				}
    				return true;
    			}
    			
    		});
    	} catch (CoreException e) {
    		throw new RuntimeException(e);
    	}
    	return ret;
    }

}
