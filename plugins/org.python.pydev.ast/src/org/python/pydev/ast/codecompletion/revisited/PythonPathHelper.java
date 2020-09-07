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
package org.python.pydev.ast.codecompletion.revisited;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.python.pydev.ast.codecompletion.IPythonModuleResolver;
import org.python.pydev.ast.codecompletion.revisited.ModulesFoundStructure.ZipContents;
import org.python.pydev.ast.listing_utils.PyFileListing;
import org.python.pydev.ast.listing_utils.PyFileListing.PyFileInfo;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForFolder;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.plugin.nature.IPythonPathHelper;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.OrderedMap;

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
    private List<IPath> searchPaths = Collections.unmodifiableList(new ArrayList<IPath>());

    /**
     * The array of module resolvers from all org.python.pydev.pydev_python_module_resolver extensions.
     * Initialized lazily by {@link getPythonModuleResolvers}.
     */
    private transient IPythonModuleResolver[] pythonModuleResolvers;
    private final Object pythonModuleResolversLock = new Object();

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
    public static PyFileListing getModulesBelow(File root, IProgressMonitor monitor,
            List<String> allPythonpathEntries) {
        if (!root.exists()) {
            return null;
        }

        if (root.isDirectory()) {
            Set<File> otherRoots = new HashSet<>();
            for (String p : allPythonpathEntries) {
                otherRoots.add(new File(p));
            }
            otherRoots.remove(root);

            FileFilter filter = new FileFilter() {

                @Override
                public boolean accept(File file) {
                    if (file.isFile()) {
                        return isValidFileMod(FileUtils.getFileAbsolutePath(file));
                    } else if (file.isDirectory()) {
                        if (otherRoots.contains(file)) {
                            // We should not go into other roots.
                            return false;
                        }
                        return isValidModuleLastPart(file.getName());
                    } else {
                        return false;
                    }
                }

            };
            return PyFileListing.getPyFilesBelow(root, filter, monitor);

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
        if (root.isFile() && FileTypesPreferences.isValidZipFile(fileName)) { //ok, it may be a jar file, so let's get its contents and get the available modules

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
        return isValidSourceFile(path, FileTypesPreferences.getDottedValidSourceFiles());
    }

    public static boolean isValidSourceFile(String path, String[] dottedValidSourceFiles) {
        int len = dottedValidSourceFiles.length;
        for (int i = 0; i < len; i++) {
            if (path.endsWith(dottedValidSourceFiles[i])) {
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
        String[] validSourceFiles = FileTypesPreferences.getValidSourceFiles();
        int len = validSourceFiles.length;
        for (int i = 0; i < len; i++) {
            String end = validSourceFiles[i];
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

        } else if (FileTypesPreferences.isValidDll(path)) {
            ret = true;
        }

        return ret;
    }

    public static String getValidName(String fileName) {
        return FullRepIterable.getFirstPart(fileName); // if we have something as cx_Oracle.cp36-win_amd64.pyd, we only want the cx_Oracle part.
    }

    /**
     * Resolves an absolute file system location of a module to its name, scoped to the paths in
     * {@link #getPythonpath()}.
     *
     * @param absoluteModuleLocation the location of the module. Only for directories, or .py, .pyd,
     *      .dll, .so, .pyo files.
     * @return a dot-separated qualified name of the Python module that the file or folder should
     *      represent. E.g.: {@code compiler.ast}.
     */
    @Override
    public String resolveModule(String absoluteModuleLocation, IProject project) {
        return resolveModule(absoluteModuleLocation, false, getPythonpath(), project);
    }

    /**
     * Resolves an absolute file system location of a a module to its name, scoped to the paths in
     * {@link #getPythonpath()} and in context to a given project.
     *
     * @param absoluteModuleLocation the location of the module. Only for directories, or .py, .pyd,
     *      .dll, .so, .pyo files.
     * @param requireFileToExist if {@code true}, requires the path to exist on the filesystem.
     * @param project the project context in which the module resolution is being performed.
     *      If resolution is to be done without respect to a project, then {@code null}.
     * @return a dot-separated qualified name of the Python module that the file or folder should
     *      represent. E.g.: {@code compiler.ast}.
     */
    public String resolveModule(String absoluteModuleLocation, final boolean requireFileToExist,
            IProject project) {
        return resolveModule(absoluteModuleLocation, requireFileToExist, getPythonpath(), project);
    }

    /**
     * Resolves an absolute file system location of a module to its name, scoped to the paths in
     * the search locations and in context to a given project.
     *
     * @param absoluteModuleLocation the location of the module. Only for directories, or .py, .pyd,
     *      .dll, .so, .pyo files.
     * @param requireFileToExist if {@code true}, requires the path to exist on the filesystem.
     * @param baseLocations the locations relative to which to resolve the Python module.
     * @param project the project context in which the module resolution is being performed.
     *      Can be {@code null} if resolution should to be done without respect to a project.
     * @return a dot-separated qualified name of the Python module that the file or folder should
     *      represent. E.g.: {@code compiler.ast}.
     */
    public String resolveModule(String absoluteModuleLocation, final boolean requireFileToExist,
            List<String> baseLocations, IProject project) {
        final File file = new File(absoluteModuleLocation);
        boolean fileExists = file.exists();
        if (requireFileToExist && !fileExists) {
            return null;
        }
        final IPath modulePath = Path.fromOSString(FileUtils.getFileAbsolutePath(file));

        // Try to consult each of the resolvers:
        IPythonModuleResolver[] pythonModuleResolvers = getPythonModuleResolvers();
        if (pythonModuleResolvers.length > 0) {
            List<IPath> convertedBasePaths = new ArrayList<>();
            for (String searchPath : baseLocations) {
                convertedBasePaths.add(Path.fromOSString(searchPath));
            }

            for (IPythonModuleResolver resolver : pythonModuleResolvers) {
                String resolved = resolver.resolveModule(project, modulePath, convertedBasePaths);
                if (resolved == null) {
                    // The null string represents delegation to the next resolver.
                    continue;
                }
                if (resolved.isEmpty()) {
                    // The empty string represents resolution failure.
                    return null;
                }
                return resolved;
            }
        }

        List<String> alternatives = new ArrayList<String>();
        // If all of the resolvers have delegated, then go forward with the default behavior.

        //go through our pythonpath and check the beginning
        OUT: for (String pathEntry : baseLocations) {

            IPath element = Path.fromOSString(FileUtils.getFileAbsolutePath(pathEntry));
            if (FileUtils.isPrefixOf(element, modulePath)) {
                IPath relative = modulePath.removeFirstSegments(element.segmentCount());
                if (relative.segmentCount() == 0) {
                    continue;
                }

                final String name = PythonPathHelper.getValidName(relative.lastSegment());
                if (!isValidModuleLastPart(name)) {
                    continue;
                }
                if (relative.segmentCount() > 1) {
                    IPath folderPath = relative.removeLastSegments(1);
                    int segmentCount = folderPath.segmentCount();
                    for (int i = 0; i < segmentCount; i++) {
                        if (!isValidModuleLastPart(folderPath.segment(i))) {
                            continue OUT;
                        }
                    }
                    String[] segments = relative.segments();
                    segments[segments.length - 1] = name;
                    alternatives.add(StringUtils.join(".", segments));
                } else {
                    alternatives.add(name);
                }
            }
        }
        int size = alternatives.size();
        if (size == 1) {
            return alternatives.get(0);
        }
        if (size == 0) {
            return null;
        }
        // We have multiple alternatives. Get the one with the shortest len (if we have multiple pythonpath entries
        // where one is beneath the other a name may be resolved to multiple names and the correct one is the shortest
        // name).
        String ret = null;
        for (String string : alternatives) {
            if (ret == null) {
                ret = string;
            } else if (string.length() < ret.length()) {
                ret = string;
            }
        }
        return ret;
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
    public static File getFolderInit(File root) {
        // Checking for existence of a specific file is much faster than listing a directory!
        String[] validInitFiles = FileTypesPreferences.getValidInitFiles();
        int len = validInitFiles.length;
        for (int i = 0; i < len; i++) {
            String init = validInitFiles[i];
            File f = new File(root, init);
            if (f.exists()) {
                return f;
            }
        }

        return null;
    }

    /**
     * @param root this is the folder we're checking
     * @return true if it is a folder with an __init__ python file
     */
    public static IFile getFolderInit(IContainer root) {
        // Checking for existence of a specific file is much faster than listing a directory!
        String[] validInitFiles = FileTypesPreferences.getValidInitFiles();
        int len = validInitFiles.length;
        for (int i = 0; i < len; i++) {
            String init = validInitFiles[i];
            IFile f = root.getFile(new Path(init));
            if (f.exists()) {
                return f;
            }
        }

        return null;
    }

    /**
     * @param item the file we want to check
     * @return true if the file is a valid __init__ file
     */
    public static boolean isValidInitFile(String path) {
        String[] validInitFiles = FileTypesPreferences.getValidInitFiles();
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
        this.fixSearchPaths();
    }

    /**
     * Sets searchPaths (a list of the pythonpath search directories as {@link IPath}s).
     */
    private void fixSearchPaths() {
        List<String> pathStrings = getPythonpath();
        ArrayList<IPath> searchPaths = new ArrayList<>(pathStrings.size());
        for (String searchPath : pathStrings) {
            searchPaths.add(Path.fromOSString(searchPath));
        }
        this.searchPaths = Collections.unmodifiableList(searchPaths);
    }

    /**
     * @param string with paths separated by |
     * @return
     */
    @Override
    public void setPythonPath(String string) {
        setPythonPath(parsePythonPathFromStr(string, new ArrayList<String>()));
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
    @Override
    public List<String> getPythonpath() {
        return pythonpath;
    }

    /**
     * Collects the Python modules.
     */
    @Override
    public ModulesFoundStructure getModulesFoundStructure(IProgressMonitor monitor) {
        return getModulesFoundStructure(null, monitor);
    }

    /**
     * Collects the Python modules.
     * <p>
     * Plugins that extend the {@code org.python.pydev.pydev_python_module_resolver} extension point
     * can extend the behavior of this method.  If no such extension exists, the default behavior
     * is to recursively traverse the directories in the PYTHONPATH.
     *
     * @param project the project scope, can be {@code null} to represent a system-wide collection.
     * @param monitor a project monitor, can be {@code null}.
     * @return a {@link ModulesFoundStructure} containing the encountered modules.
     */
    public ModulesFoundStructure getModulesFoundStructure(IProject project,
            IProgressMonitor monitor) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        IPythonModuleResolver[] pythonModuleResolvers = getPythonModuleResolvers();
        if (pythonModuleResolvers.length > 0) {
            List<IPath> searchPaths = this.searchPaths;
            for (IPythonModuleResolver finder : pythonModuleResolvers) {
                Collection<IPath> modulesAndZips = finder.findAllModules(project, monitor);
                if (modulesAndZips == null) {
                    continue;
                }
                ModulesFoundStructure modulesFoundStructure = new ModulesFoundStructure();
                for (IPath moduleOrZip : modulesAndZips) {
                    File moduleOrZipFile = moduleOrZip.toFile();
                    if (FileTypesPreferences.isValidZipFile(moduleOrZip.toOSString())) {
                        ModulesFoundStructure.ZipContents zipContents = getFromZip(moduleOrZipFile, monitor);
                        if (zipContents != null) {
                            modulesFoundStructure.zipContents.add(zipContents);
                        }
                    } else {
                        String qualifiedName = finder.resolveModule(project, moduleOrZip, searchPaths);
                        if (qualifiedName != null && !qualifiedName.isEmpty()) {
                            modulesFoundStructure.regularModules.put(moduleOrZipFile, qualifiedName);
                        }
                    }
                }
                return modulesFoundStructure;
            }
        }

        // The default behavior is to recursively traverse the directories in the PYTHONPATH to
        // collect all encountered Python modules.
        ModulesFoundStructure ret = new ModulesFoundStructure();

        List<String> pythonpathList = getPythonpath();
        FastStringBuffer tempBuf = new FastStringBuffer();
        for (Iterator<String> iter = pythonpathList.iterator(); iter.hasNext();) {
            String element = iter.next();

            if (monitor.isCanceled()) {
                break;
            }

            //the slow part is getting the files... not much we can do (I think).
            File root = new File(element);
            PyFileListing below = getModulesBelow(root, monitor, pythonpathList);
            if (below != null) {

                Iterator<PyFileInfo> e1 = below.getFoundPyFileInfos().iterator();
                while (e1.hasNext()) {
                    PyFileInfo pyFileInfo = e1.next();
                    File file = pyFileInfo.getFile();
                    String modName = pyFileInfo.getModuleName(tempBuf);
                    if (isValidModuleLastPart(FullRepIterable.getLastPart(modName))) {
                        // Only override if the new name is < than the previous name
                        // (a file may be found multiple times depending on the pythonpath).
                        String existing = ret.regularModules.get(file);
                        if (existing != null) {
                            if (existing.length() < modName.length()) {
                                ret.regularModules.put(file, modName);

                            }
                        } else {
                            ret.regularModules.put(file, modName);
                        }
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
        setPythonPath(StringUtils.split(fileContents, '\n'));
    }

    /**
     * @param pythonpatHelperFile
     */
    public void saveToFile(File pythonpatHelperFile) {
        FileUtils.writeStrToFile(StringUtils.join("\n", this.pythonpath),
                pythonpatHelperFile);
    }

    public static boolean canAddAstInfoForSourceModule(ModulesKey key) {
        if (key.file != null && key.file.exists()) {
            if (key instanceof ModulesKeyForFolder) {
                return true;
            }

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
                    if (FileUtils.isPrefixOf(resource.getFullPath(), existingInPath)) {
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

    /**
     * Returns an array of resolvers. If none can be found, returns an empty array.
     */
    private IPythonModuleResolver[] getPythonModuleResolvers() {
        // Make common case faster (unsynched).
        if (pythonModuleResolvers != null) {
            return pythonModuleResolvers;
        }

        synchronized (pythonModuleResolversLock) {
            // Who knows if it was changed in the meanwhile?
            if (pythonModuleResolvers != null) {
                return pythonModuleResolvers;
            }

            ArrayList<IPythonModuleResolver> tempPythonModuleResolvers = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<Object> resolvers = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PYTHON_MODULE_RESOLVER);
            for (Object resolver : resolvers) {
                if (resolver instanceof IPythonModuleResolver) {
                    tempPythonModuleResolvers.add((IPythonModuleResolver) resolver);
                }
            }
            pythonModuleResolvers = tempPythonModuleResolvers.toArray(new IPythonModuleResolver[0]);
            return pythonModuleResolvers;
        }
    }
}
