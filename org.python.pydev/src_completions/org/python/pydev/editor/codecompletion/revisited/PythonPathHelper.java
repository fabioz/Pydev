/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure.ZipContents;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.PyFileListing;

/**
 * This is not a singleton because we may have a different pythonpath for each project (even though
 * we have a default one as the original pythonpath).
 * 
 * @author Fabio Zadrozny
 */
public class PythonPathHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * This is a list of Files containg the pythonpath.
     */
    private volatile List<String> pythonpath = new ArrayList<String>();

    /**
     * Returns the default path given from the string.
     * @param str
     * @param acceptPoint says if we can have dots in the str that has the path to be analyzed
     * @return a trimmed string with all the '\' converted to '/'
     */
    public String getDefaultPathStr(String str) {
        //this check is no longer done... could result in other problems
        // if(acceptPoint == false && str.indexOf(".") == 0){ //cannot start with a dot
        // 		throw new RuntimeException("The pythonpath can only have absolute paths (cannot start with '.', therefore, the path: '"+str+"' is not valid.");
        // }
        return StringUtils.replaceAllSlashes(str.trim());
    }

    /**
     * This method returns all modules that can be obtained from a root File.
     * @param monitor keep track of progress (and cancel)
     * @return the listing with valid module files considering that root is a root path in the pythonpath.
     * May return null if the passed file does not exist or is not a directory (e.g.: zip file)
     */
    public PyFileListing.PyFileListingInfo getModulesBelow(File root, IProgressMonitor monitor) {
        if (!root.exists()) {
            return null;
        }

        if (root.isDirectory()) {
            FileFilter filter = new FileFilter() {

                public boolean accept(File pathname) {
                    if (pathname.isFile()) {
                        return isValidFileMod(REF.getFileAbsolutePath(pathname));
                    } else if (pathname.isDirectory()) {
                        return isFileOrFolderWithInit(pathname);
                    } else {
                        return false;
                    }
                }

            };
            return PyFileListing.getPyFilesBelow(root, filter, monitor, true);

        }
        return null;
    }

    /**
     * @param root the zip file to analyze
     * @param monitor the monitor, to keep track of what is happening
     * @return a list with the name of the found modules in the jar
     */
    private static ModulesFoundStructure.ZipContents getFromJar(File root, IProgressMonitor monitor) {

        String fileName = root.getName();
        if (root.isFile() && isValidZipFile(fileName)) { //ok, it may be a jar file, so let's get its contents and get the available modules

            //the major difference from handling jars from regular python files is that we don't have to check for __init__.py files
            boolean isValidJarFile = isValidJarFile(fileName);

            ModulesFoundStructure.ZipContents zipContents = new ModulesFoundStructure.ZipContents(root);
            
            if(isValidJarFile){
                zipContents.zipContentsType = ZipContents.ZIP_CONTENTS_TYPE_JAR;
            }else{
                zipContents.zipContentsType = ZipContents.ZIP_CONTENTS_TYPE_PY_ZIP;
            }
            
            try {
                String zipFileName = root.getName();
                
                
                ZipFile zipFile = new ZipFile(root);
                try{
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    
                    int i=0;
                    //ok, now that we have the zip entries, let's map them to modules
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!entry.isDirectory()) {
                            if(isValidFileMod(name) || name.endsWith(".class")){
                                //it is a valid python file
                                if(i % 15 == 0){
                                    if(monitor.isCanceled()){
                                        return null;
                                    }
                                    monitor.setTaskName(new StringBuffer("Found in ").append(zipFileName).append(" module ").append(name).toString());
                                    monitor.worked(1);
                                }
                                
                                if(isValidInitFile(name)){
                                    zipContents.pyInitFilesLowerWithoutExtension.add(StringUtils.stripExtension(name).toLowerCase());
                                }
                                zipContents.pyFilesLowerToRegular.put(name.toLowerCase(), name);
                            }
                            
                        }else{ //!isDirectory
                            zipContents.pyfoldersLower.add(name.toLowerCase());
                        }
                        i++;
                    }
                }finally{
                    zipFile.close();
                }
                
                //now, on to actually filling the structure if we have a zip file (just add the ones that are actually under
                //the pythonpath)
                zipContents.consolidatePythonpathInfo(monitor);
                
                return zipContents;

            } catch (Exception e) {
                //that's ok, it is probably not a zip file after all...
                PydevPlugin.log(e);
            }
        }
        return null;
    }

    /**
     * @return true if the given filename should be considered a jar file.
     */
    private static boolean isValidJarFile(String fileName) {
        return fileName.endsWith(".jar");
    }

    /**
     * @return true if the given filename should be considered a zip file.
     */
    public static boolean isValidZipFile(String fileName) {
        return fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".egg");
    }

    /**
     * @param path the path we want to analyze
     * @return if the path passed belongs to a valid python compiled extension
     */
    public static boolean isValidDll(String path) {
        if (path.endsWith(".pyd") || path.endsWith(".so") || path.endsWith(".dll")) {
            return true;
        }
        return false;
    }

    public final static String[] WILDCARD_JYTHON_VALID_ZIP_FILES = new String[] { "*.jar", "*.zip" };
    
    public final static String[] WILDCARD_PYTHON_VALID_ZIP_FILES = new String[] { "*.egg", "*.zip" };
    
    public final static String[] WILDCARD_VALID_SOURCE_FILES = new String[] { "*.py", "*.pyw" };

    public final static String[] DOTTED_VALID_SOURCE_FILES = new String[] { ".py", ".pyw" };

    public final static String[] VALID_SOURCE_FILES = new String[] { "py", "pyw" };

    public final static String[] getDottedValidSourceFiles() {
        return DOTTED_VALID_SOURCE_FILES;
    }

    public final static String[] getValidSourceFiles() {
        return VALID_SOURCE_FILES;
    }

    public final static String getDefaultDottedPythonExtension() {
        return ".py";
    }

    /**
     * @return if the path passed belongs to a valid python source file (checks for the extension)
     */
    public static boolean isValidSourceFile(String path) {
        path = path.toLowerCase();
        for (String end : getDottedValidSourceFiles()) {
            if (path.endsWith(end)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether an IFile is a valid source file given its extension
     */
    public static boolean isValidSourceFile(IFile file) {
        String ext = file.getFileExtension();
        if (ext == null) { // no extension
            return false;
        }
        ext = ext.toLowerCase();
        for (String end : getValidSourceFiles()) {
            if (ext.equals(end)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return if the paths maps to a valid python module (depending on its extension).
     */
    public static boolean isValidFileMod(String path) {

        boolean ret = false;
        if (isValidSourceFile(path)) {
            ret = true;

        } else if (isValidDll(path)) {
            ret = true;
        }

        return ret;
    }

    public String resolveModule(String fullPath) {
        return resolveModule(fullPath, false);
    }

    /**
     * DAMN... when I started thinking this up, it seemed much better... (and easier)
     * 
     * @param module - this is the full path of the module. Only for directories or py,pyd,dll,pyo files.
     * @return a String with the module that the file or folder should represent. E.g.: compiler.ast
     */
    public String resolveModule(String fullPath, final boolean requireFileToExist) {
        fullPath = REF.getFileAbsolutePath(fullPath);
        fullPath = getDefaultPathStr(fullPath);
        String fullPathWithoutExtension;

        if (isValidSourceFile(fullPath) || isValidDll(fullPath)) {
            fullPathWithoutExtension = FullRepIterable.headAndTail(fullPath)[0];
        } else {
            fullPathWithoutExtension = fullPath;
        }

        final File moduleFile = new File(fullPath);

        if (requireFileToExist) {
            if (moduleFile.exists() == false) {
                return null;
            }
        }

        boolean isFile = moduleFile.isFile();

        synchronized (pythonpath) {
            //go through our pythonpath and check the beggining
            for (Iterator<String> iter = pythonpath.iterator(); iter.hasNext();) {

                String element = getDefaultPathStr(iter.next());
                if (fullPath.startsWith(element)) {
                    int len = element.length();
                    String s = fullPath.substring(len);
                    String sWithoutExtension = fullPathWithoutExtension.substring(len);

                    if (s.startsWith("/")) {
                        s = s.substring(1);
                    }
                    if (sWithoutExtension.startsWith("/")) {
                        sWithoutExtension = sWithoutExtension.substring(1);
                    }

                    if (!isValidModule(sWithoutExtension)) {
                        continue;
                    }

                    s = s.replaceAll("/", ".");
                    if (s.indexOf(".") != -1) {
                        File root = new File(element);
                        if (root.exists() == false) {
                            continue;
                        }

                        //this means that more than 1 module is specified, so, in order to get it,
                        //we have to go and see if all the folders to that module have __init__.py in it...
                        String[] modulesParts = FullRepIterable.dotSplit(s);

                        if (modulesParts.length > 1 && isFile) {
                            String[] t = new String[modulesParts.length - 1];

                            for (int i = 0; i < modulesParts.length - 1; i++) {
                                t[i] = modulesParts[i];
                            }
                            t[t.length - 1] = t[t.length - 1] + "." + modulesParts[modulesParts.length - 1];
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
                            root = new File(REF.getFileAbsolutePath(root) + "/" + modulesParts[i]);

                            //check if file is in root...
                            if (isValidFileMod(modulesParts[i])) {
                                if (root.exists() && root.isFile()) {
                                    break;
                                }

                            } else {
                                //this part is a folder part... check if it is a valid module (has init).
                                if (isFileOrFolderWithInit(root) == false) {
                                    isValid = false;
                                    break;
                                }
                                //go on and check the next part.
                            }
                        }
                        if (isValid) {
                            if (isFile) {
                                s = stripExtension(s);
                            } else if (moduleFile.exists() == false) {
                                //ok, it does not exist, so isFile will not work, let's just check if it is
                                //a valid module (ends with .py or .pyw) and if it is, strip the extension
                                if (isValidFileMod(s)) {
                                    s = stripExtension(s);
                                }
                            }
                            return s;
                        }
                    } else {
                        //simple part, we don't have to go into subfolders to check validity...
                        if (isFile) {
                            throw new RuntimeException(
                                    "This should never happen... if it is a file, it always has a dot, so, this should not happen...");
                        } else if (moduleFile.isDirectory() && isFileOrFolderWithInit(moduleFile) == false) {
                            return null;
                        }
                        return s;
                    }
                }

            }
            //ok, it was not found in any existing way, so, if we don't require the file to exist, let's just do some simpler search and get the 
            //first match (if any)... this is useful if the file we are looking for has just been deleted
            if (requireFileToExist == false) {
                //we have to remove the last part (.py, .pyc, .pyw)
                for (String element : pythonpath) {
                    element = getDefaultPathStr(element);
                    if (fullPathWithoutExtension.startsWith(element)) {
                        String s = fullPathWithoutExtension.substring(element.length());
                        if (s.startsWith("/")) {
                            s = s.substring(1);
                        }
                        if (!isValidModule(s)) {
                            continue;
                        }
                        s = s.replaceAll("/", ".");
                        return s;
                    }
                }
            }
            return null;

        }
    }

    /**
     * Note that this function is not completely safe...beware when using it.
     * @param s
     * @return
     */
    public static String stripExtension(String s) {
        if (s != null) {
            return StringUtils.stripExtension(s);
        }
        return null;
    }

    /**
     * @param root this is the folder we're checking
     * @return true if it is a folder with an __init__ python file
     */
    private boolean isFileOrFolderWithInit(File root) {
        //check for an __init__ in a dir (we do not check if it is a file, becase if it is, it should return null)
        String[] items = root.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (isValidInitFile(name)) {
                    return true;
                }
                return false;
            }

        });
        if (items == null || items.length < 1) {
            return false;
        }

        return true;
    }

    /**
     * @param item the file we want to check
     * @return true if the file is a valid __init__ file
     */
    public static boolean isValidInitFile(String item) {
        return item.toLowerCase().indexOf("__init__.") != -1 && isValidSourceFile(item);
    }

    /**
     * @param s
     * @return
     */
    private boolean isValidModule(String s) {
        return s.indexOf("-") == -1 && s.indexOf(" ") == -1 && s.indexOf(".") == -1;
    }

    /**
     * @param string with paths separated by |
     * @return
     */
    public List<String> setPythonPath(String string) {
        synchronized (pythonpath) {
            pythonpath.clear();
            getPythonPathFromStr(string, pythonpath);
            return new ArrayList<String>(pythonpath);
        }
    }

    /**
     * @param string this is the string that has the pythonpath (separated by |)
     * @param lPath OUT: this list is filled with the pythonpath.
     */
    public void getPythonPathFromStr(String string, List<String> lPath) {
        String[] strings = string.split("\\|");
        for (int i = 0; i < strings.length; i++) {
            String defaultPathStr = getDefaultPathStr(strings[i]);
            if (defaultPathStr != null && defaultPathStr.trim().length() > 0) {
                File file = new File(defaultPathStr);
                if (file.exists()) {
                    //we have to get it with the appropriate cases and in a canonical form
                    String path = REF.getFileAbsolutePath(file);
                    lPath.add(path);
                }
            }
        }
    }

    public List<String> getPythonpath() {
        return new ArrayList<String>(this.pythonpath);
    }

    /**
     * This method should traverse the pythonpath passed and return a structure with the info that could be collected
     * about the files that are related to python modules.
     */
    public ModulesFoundStructure getModulesFoundStructure(List<String> pythonpathList, IProgressMonitor monitor) {
        ModulesFoundStructure ret = new ModulesFoundStructure();

        for (Iterator<String> iter = pythonpathList.iterator(); iter.hasNext() && monitor.isCanceled() == false;) {
            String element = iter.next();

            //the slow part is getting the files... not much we can do (I think).
            File root = new File(element);
            PyFileListing.PyFileListingInfo below = getModulesBelow(root, monitor);
            if (below != null) {

                Iterator<File> e1 = below.filesFound.iterator();
                Iterator<String> e2 = below.fileParentPathNamesRelativeToRoot.iterator();
                //both must have the same size... so, just check hasNext in one of those
                while (e1.hasNext()) {
                    File o1 = e1.next();
                    String o2 = e2.next();

                    String modName;
                    if (o2.length() != 0) {
                        modName = new StringBuffer(o2).append(".").append(stripExtension(o1.getName())).toString();
                    } else {
                        modName = stripExtension(o1.getName());
                    }
                    ret.regularModules.put(o1, modName);
                }

            } else { //ok, it was null, so, maybe this is not a folder, but zip file with java classes...
                ModulesFoundStructure.ZipContents zipContents = getFromJar(root, monitor);
                if (zipContents != null) {
                    ret.zipContents.add(zipContents);
                }
            }
        }
        return ret;
    }

}
