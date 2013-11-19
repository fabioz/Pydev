/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.ide.IDE;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure.ZipContents;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.plugin.nature.IPythonPathHelper;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.OrderedMap;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;
import org.python.pydev.utils.PyFileListing;
import org.python.pydev.utils.PyFileListing.PyFileInfo;

/**
 * This is not a singleton because we may have a different pythonpath for each project (even though
 * we have a default one as the original pythonpath).
 *
 * @author Fabio Zadrozny
 */
public final class PythonPathHelper implements IPythonPathHelper {

    /**
     * This is a list of Files containing the pythonpath. It's always an immutable list. The instance must
     * be changed to change the pythonpath.
     */
    private volatile List<String> pythonpath = Collections.unmodifiableList(new ArrayList<String>());

    /**
     * Returns the default path given from the string.
     * @param str
     * @return a trimmed string with all the '\' converted to '/'
     */
    public static String getDefaultPathStr(String str) {
        //this check is no longer done... could result in other problems
        // if(acceptPoint == false && str.indexOf(".") == 0){ //cannot start with a dot
        //         throw new RuntimeException("The pythonpath can only have absolute paths (cannot start with '.', therefore, the path: '"+str+"' is not valid.");
        // }
        return StringUtils.replaceAllSlashes(str.trim());
    }

    public PythonPathHelper() {
    }

    /**
     * This method returns all modules that can be obtained from a root File.
     * @param monitor keep track of progress (and cancel)
     * @return the listing with valid module files considering that root is a root path in the pythonpath.
     * May return null if the passed file does not exist or is not a directory (e.g.: zip file)
     */
    public static PyFileListing getModulesBelow(File root, IProgressMonitor monitor) {
        if (!root.exists()) {
            return null;
        }

        if (root.isDirectory()) {
            FileFilter filter = new FileFilter() {

                public boolean accept(File pathname) {
                    if (pathname.isFile()) {
                        return isValidFileMod(FileUtils.getFileAbsolutePath(pathname));
                    } else if (pathname.isDirectory()) {
                        return isFolderWithInit(pathname);
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
    protected static ModulesFoundStructure.ZipContents getFromZip(File root, IProgressMonitor monitor) {

        String fileName = root.getName();
        if (root.isFile() && FileTypesPreferencesPage.isValidZipFile(fileName)) { //ok, it may be a jar file, so let's get its contents and get the available modules

            //the major difference from handling jars from regular python files is that we don't have to check for __init__.py files
            ModulesFoundStructure.ZipContents zipContents = new ModulesFoundStructure.ZipContents(root);

            //by default it's a zip (for python) -- may change if a .class is found.
            zipContents.zipContentsType = ZipContents.ZIP_CONTENTS_TYPE_PY_ZIP;

            try {
                String zipFileName = root.getName();

                ZipFile zipFile = new ZipFile(root);
                try {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    int i = 0;
                    FastStringBuffer buffer = new FastStringBuffer();
                    //ok, now that we have the zip entries, let's map them to modules
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!entry.isDirectory()) {
                            if (isValidFileMod(name) || name.endsWith(".class")) {

                                if (name.endsWith(".class")) {
                                    zipContents.zipContentsType = ZipContents.ZIP_CONTENTS_TYPE_JAR;
                                }

                                //it is a valid python file
                                if (i % 15 == 0) {
                                    if (monitor.isCanceled()) {
                                        return null;
                                    }
                                    buffer.clear();
                                    monitor.setTaskName(buffer.append("Found in ").append(zipFileName)
                                            .append(" module ").append(name).toString());
                                    monitor.worked(1);
                                }

                                if (isValidInitFile(name)) {
                                    zipContents.pyInitFilesLowerWithoutExtension.add(StringUtils.stripExtension(name)
                                            .toLowerCase());
                                }
                                zipContents.pyFilesLowerToRegular.put(name.toLowerCase(), name);
                            }

                        } else { //!isDirectory
                            zipContents.pyfoldersLower.add(name.toLowerCase());
                        }
                        i++;
                    }
                } finally {
                    zipFile.close();
                }

                //now, on to actually filling the structure if we have a zip file (just add the ones that are actually under
                //the pythonpath)
                zipContents.consolidatePythonpathInfo(monitor);

                return zipContents;

            } catch (Exception e) {
                //that's ok, it is probably not a zip file after all...
                Log.log(e);
            }
        }
        return null;
    }

    /**
     * @return if the path passed belongs to a valid python source file (checks for the extension)
     */
    public static boolean isValidSourceFile(String path) {
        for (String end : FileTypesPreferencesPage.getDottedValidSourceFiles()) {
            if (path.endsWith(end)) {
                return true;
            }
        }
        if (path.endsWith(".pypredef")) {
            return true;
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
        for (String end : FileTypesPreferencesPage.getValidSourceFiles()) {
            if (ext.equals(end)) {
                return true;
            }
        }
        if (ext.equals(".pypredef")) {
            return true;
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

        } else if (FileTypesPreferencesPage.isValidDll(path)) {
            ret = true;
        }

        return ret;
    }

    public String resolveModule(String fullPath) {
        return resolveModule(fullPath, false);
    }

    public String resolveModule(String fullPath, ArrayList<String> pythonPathToUse) {
        return resolveModule(fullPath, false, pythonPathToUse);
    }

    public String resolveModule(String fullPath, final boolean requireFileToExist) {
        return resolveModule(fullPath, requireFileToExist, getPythonpath());
    }

    /**
     * DAMN... when I started thinking this up, it seemed much better... (and easier)
     *
     * @param module - this is the full path of the module. Only for directories or py,pyd,dll,pyo files.
     * @return a String with the module that the file or folder should represent. E.g.: compiler.ast
     */
    public String resolveModule(String fullPath, final boolean requireFileToExist, List<String> pythonPathCopy) {
        fullPath = FileUtils.getFileAbsolutePath(fullPath);
        fullPath = getDefaultPathStr(fullPath);
        String fullPathWithoutExtension;

        if (isValidSourceFile(fullPath) || FileTypesPreferencesPage.isValidDll(fullPath)) {
            fullPathWithoutExtension = FullRepIterable.headAndTail(fullPath)[0];
        } else {
            fullPathWithoutExtension = fullPath;
        }

        final File moduleFile = new File(fullPath);

        if (requireFileToExist && !moduleFile.exists()) {
            return null;
        }

        boolean isFile = moduleFile.isFile();

        //go through our pythonpath and check the beginning
        for (String pathEntry : pythonPathCopy) {

            String element = getDefaultPathStr(pathEntry);
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

                if (!isValidModuleLastPart(sWithoutExtension)) {
                    continue;
                }

                s = s.replaceAll("/", ".");
                if (s.indexOf(".") != -1) {
                    File root = new File(element);
                    if (root.exists() == false) {
                        continue;
                    }

                    final List<String> temp = StringUtils.dotSplit(s);
                    String[] modulesParts = temp.toArray(new String[temp.size()]);

                    //this means that more than 1 module is specified, so, in order to get it,
                    //we have to go and see if all the folders to that module have __init__.py in it...
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
                        root = new File(FileUtils.getFileAbsolutePath(root) + "/" + modulesParts[i]);

                        //check if file is in root...
                        if (isValidFileMod(modulesParts[i])) {
                            if (root.exists() && root.isFile()) {
                                break;
                            }

                        } else {
                            //this part is a folder part... check if it is a valid module (has init).
                            if (isFolderWithInit(root) == false) {
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
                    if (!isFile && moduleFile.isDirectory() && isFolderWithInit(moduleFile) == false) {
                        return null;
                    }
                    return s;
                }
            }

        }
        //ok, it was not found in any existing way, so, if we don't require the file to exist, let's just do some simpler search and get the
        //first match (if any)... this is useful if the file we are looking for has just been deleted
        if (!requireFileToExist) {
            //we have to remove the last part (.py, .pyc, .pyw)
            for (String element : pythonPathCopy) {
                element = getDefaultPathStr(element);
                if (fullPathWithoutExtension.startsWith(element)) {
                    String s = fullPathWithoutExtension.substring(element.length());
                    if (s.startsWith("/")) {
                        s = s.substring(1);
                    }
                    if (!isValidModuleLastPart(s)) {
                        continue;
                    }
                    s = s.replaceAll("/", ".");
                    return s;
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
    public static boolean isFolderWithInit(File root) {
        // Checking for existence of a specific file is much faster than listing a directory!
        String[] validInitFiles = FileTypesPreferencesPage.getValidInitFiles();
        int len = validInitFiles.length;
        for (int i = 0; i < len; i++) {
            String init = validInitFiles[i];
            File f = new File(root, init);
            if (f.exists()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param item the file we want to check
     * @return true if the file is a valid __init__ file
     */
    public static boolean isValidInitFile(String path) {
        String[] validInitFiles = FileTypesPreferencesPage.getValidInitFiles();
        int len = validInitFiles.length;
        for (int i = 0; i < len; i++) {
            String init = validInitFiles[i];
            if (path.endsWith(init)) {
                int index = (path.length() - init.length()) - 1;
                if (index >= 0) {
                    //if the char before exists and is not a separator, it's not a valid
                    //__init__ file.
                    char c = (path.charAt(index));
                    if (c != '/' && c != '\\') {
                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
     * @param s
     * @return
     */
    public static boolean isValidModuleLastPart(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-' || c == ' ' || c == '.' || c == '+') {
                return false;
            }
        }
        return true;
    }

    public void setPythonPath(List<String> newPythonpath) {
        this.pythonpath = Collections.unmodifiableList(new ArrayList<String>(newPythonpath));
    }

    /**
     * @param string with paths separated by |
     * @return
     */
    public void setPythonPath(String string) {
        this.pythonpath = Collections.unmodifiableList(parsePythonPathFromStr(string, new ArrayList<String>()));
    }

    /**
     * @param string this is the string that has the pythonpath (separated by |)
     * @param lPath OUT: this list is filled with the pythonpath (if null an ArrayList is created to fill the pythonpath).
     * @return
     */
    public static List<String> parsePythonPathFromStr(String string, List<String> lPath) {
        if (lPath == null) {
            lPath = new ArrayList<String>();
        }
        String[] strings = string.split("\\|");
        for (int i = 0; i < strings.length; i++) {
            String defaultPathStr = getDefaultPathStr(strings[i]);
            if (defaultPathStr != null && defaultPathStr.trim().length() > 0) {
                File file = new File(defaultPathStr);
                if (file.exists()) {
                    //we have to get it with the appropriate cases and in a canonical form
                    String path = FileUtils.getFileAbsolutePath(file);
                    lPath.add(path);
                } else {
                    lPath.add(defaultPathStr);
                }
            }
        }
        return lPath;
    }

    /**
     * @return a list with the pythonpath, such that each element of the list is a part of
     * the pythonpath
     * @note returns a list that's not modifiable!
     */
    public List<String> getPythonpath() {
        return pythonpath;
    }

    /**
     * This method should traverse the pythonpath passed and return a structure
     * with the info that could be collected about the files that are related to
     * python modules.
     */
    public ModulesFoundStructure getModulesFoundStructure(IProgressMonitor monitor) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        List<String> pythonpathList = getPythonpath();

        ModulesFoundStructure ret = new ModulesFoundStructure();

        FastStringBuffer tempBuf = new FastStringBuffer();
        for (Iterator<String> iter = pythonpathList.iterator(); iter.hasNext();) {
            String element = iter.next();

            if (monitor.isCanceled()) {
                break;
            }

            //the slow part is getting the files... not much we can do (I think).
            File root = new File(element);
            PyFileListing below = getModulesBelow(root, monitor);
            if (below != null) {

                Iterator<PyFileInfo> e1 = below.getFoundPyFileInfos().iterator();
                while (e1.hasNext()) {
                    PyFileInfo pyFileInfo = e1.next();
                    File file = pyFileInfo.getFile();
                    String modName = pyFileInfo.getModuleName(tempBuf);
                    if (isValidModuleLastPart(FullRepIterable.getLastPart(modName))) {
                        ret.regularModules.put(file, modName);
                    }
                }

            } else { //ok, it was null, so, maybe this is not a folder, but zip file with java classes...
                ModulesFoundStructure.ZipContents zipContents = getFromZip(root, monitor);
                if (zipContents != null) {
                    ret.zipContents.add(zipContents);
                }
            }
        }
        return ret;
    }

    /**
     * @param workspaceMetadataFile
     * @throws IOException
     */
    public void loadFromFile(File pythonpatHelperFile) throws IOException {
        String fileContents = FileUtils.getFileContents(pythonpatHelperFile);
        if (fileContents == null || fileContents.trim().length() == 0) {
            throw new IOException("No loaded contents from: " + pythonpatHelperFile);
        }
        this.pythonpath = Collections.unmodifiableList(StringUtils.split(fileContents, '\n'));
    }

    /**
     * @param pythonpatHelperFile
     */
    public void saveToFile(File pythonpatHelperFile) {
        FileUtils.writeStrToFile(org.python.pydev.shared_core.string.StringUtils.join("\n", this.pythonpath),
                pythonpatHelperFile);
    }

    public static boolean canAddAstInfoFor(ModulesKey key) {
        if (key.file != null && key.file.exists()) {

            if (PythonPathHelper.isValidSourceFile(key.file.getName())) {
                return true;
            }

            boolean isZipModule = key instanceof ModulesKeyForZip;
            if (isZipModule) {
                ModulesKeyForZip modulesKeyForZip = (ModulesKeyForZip) key;
                if (PythonPathHelper.isValidSourceFile(modulesKeyForZip.zipModulePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return true if PyEdit.EDITOR_ID is set as the persistent property (only if the file does not have an extension).
     */
    public static boolean markAsPyDevFileIfDetected(IFile file) {
        String name = file.getName();
        if (name == null || name.indexOf('.') != -1) {
            return false;
        }

        String editorID;
        try {
            editorID = file.getPersistentProperty(IDE.EDITOR_KEY);
            if (editorID == null) {
                InputStream contents = file.getContents(true);
                Reader inputStreamReader = new InputStreamReader(new BufferedInputStream(contents));
                if (FileUtils.hasPythonShebang(inputStreamReader)) {
                    IDE.setDefaultEditor(file, PyEdit.EDITOR_ID);
                    return true;
                }
            } else {
                return PyEdit.EDITOR_ID.equals(editorID);
            }

        } catch (Exception e) {
            if (file.exists()) {
                Log.log(e);
            }
        }
        return false;
    }

    public static final int OPERATION_MOVE = 1;
    public static final int OPERATION_COPY = 2;
    public static final int OPERATION_DELETE = 3;

    private static OrderedMap<String, String> getResourcePythonPathMap(
            Map<IProject, OrderedMap<String, String>> projectSourcePathMapsCache, IResource resource) {
        IProject project = resource.getProject();
        OrderedMap<String, String> sourceMap = projectSourcePathMapsCache.get(project);
        if (sourceMap == null) {
            IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
            // Ignore resources that come from a non-Python project.
            if (pythonPathNature == null) {
                sourceMap = new OrderedMap<String, String>();
            } else {
                try {
                    sourceMap = pythonPathNature
                            .getProjectSourcePathResolvedToUnresolvedMap();
                } catch (CoreException e) {
                    sourceMap = new OrderedMap<String, String>();
                    Log.log(e);
                }
            }
            projectSourcePathMapsCache.put(project, sourceMap);
        }
        return sourceMap;
    }

    /**
     * Helper to update the pythonpath when a copy, move or delete operation is done which could affect a source folder
     * (so, should work when moving/copying/deleting the parent folder of a source folder for instance).
     *
     * Note that the destination may be null in a delete operation.
     */
    public static void updatePyPath(IResource[] copiedResources, IContainer destination, int operation) {
        try {

            Map<IProject, OrderedMap<String, String>> projectSourcePathMapsCache = new HashMap<IProject, OrderedMap<String, String>>();
            List<String> addToDestProjects = new ArrayList<String>();

            //Step 1: remove source folders from the copied projects
            HashSet<IProject> changed = new HashSet<IProject>();
            for (IResource resource : copiedResources) {
                if (!(resource instanceof IFolder)) {
                    continue;
                }
                OrderedMap<String, String> sourceMap = PythonPathHelper.getResourcePythonPathMap(
                        projectSourcePathMapsCache, resource);

                Set<String> keySet = sourceMap.keySet();
                for (Iterator<String> it = keySet.iterator(); it.hasNext();) {
                    String next = it.next();
                    IPath existingInPath = Path.fromPortableString(next);
                    if (resource.getFullPath().isPrefixOf(existingInPath)) {
                        if (operation == PythonPathHelper.OPERATION_MOVE
                                || operation == PythonPathHelper.OPERATION_DELETE) {
                            it.remove(); //Remove from that project (but not on copy)
                            changed.add(resource.getProject());
                        }

                        if (operation == PythonPathHelper.OPERATION_COPY
                                || operation == PythonPathHelper.OPERATION_MOVE) {
                            //Add to new project (but not on delete)
                            String addToNewProjectPath = destination
                                    .getFullPath()
                                    .append(existingInPath
                                            .removeFirstSegments(resource.getFullPath().segmentCount() - 1))
                                    .toPortableString();
                            addToDestProjects.add(addToNewProjectPath);
                        }
                    }
                }
            }

            if (operation != PythonPathHelper.OPERATION_DELETE) {
                //Step 2: add source folders to the project it was copied to
                OrderedMap<String, String> destSourceMap = PythonPathHelper.getResourcePythonPathMap(
                        projectSourcePathMapsCache, destination);
                // Get the PYTHONPATH of the destination project. It may be modified to include the pasted resources.
                IProject destProject = destination.getProject();

                for (String addToNewProjectPath : addToDestProjects) {
                    String destActualPath = PyStructureConfigHelpers.convertToProjectRelativePath(
                            destProject.getFullPath().toPortableString(),
                            addToNewProjectPath);
                    destSourceMap.put(addToNewProjectPath, destActualPath);
                    changed.add(destProject);
                }
            }

            //Step 3: update the target project
            for (IProject project : changed) {
                OrderedMap<String, String> sourceMap = PythonPathHelper.getResourcePythonPathMap(
                        projectSourcePathMapsCache, project);
                PythonNature nature = PythonNature.getPythonNature(project);
                if (nature == null) {
                    continue; //don't change non-pydev projects
                }
                nature.getPythonPathNature().setProjectSourcePath(
                        StringUtils.join("|", sourceMap.values()));
                nature.rebuildPath();
            }

        } catch (Exception e) {
            Log.log(IStatus.ERROR, "Unexpected error setting project properties", e);
        }
    }
}
