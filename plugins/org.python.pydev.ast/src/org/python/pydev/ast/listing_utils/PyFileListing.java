/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.listing_utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.ast.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

/**
 * Helper class for finding out about python files below some source folder.
 *
 * @author Fabio
 */
public class PyFileListing {

    public static interface PyFileListingFilter {
        boolean accept(Path path, File file, boolean isDirectory);
    }

    /**
     * Information about a python file found (the actual file and the way it was resolved as a python module)
     */
    public static final class PyFileInfo {

        private final String relPath;

        private final File file;

        public PyFileInfo(File file, String relPath) {
            this.file = file;
            this.relPath = relPath;
        }

        /** File object. */
        public File getFile() {
            return file;
        }

        /** Returns fully qualified name of the package. */
        public String getPackageName() {
            return relPath;
        }

        @Override
        public String toString() {
            return StringUtils.join("", "PyFileInfo:", file, " - ", relPath);
        }

        /**
         * @return the name of the module represented by this info.
         */
        public String getModuleName(FastStringBuffer temp) {
            String scannedModuleName = this.getPackageName();

            String modName;
            String name = PythonPathHelper.getValidName(file.getName());
            if (scannedModuleName.length() != 0) {
                modName = temp.clear().append(scannedModuleName).append('.')
                        .append(name).toString();
            } else {
                modName = name;
            }
            return modName;
        }
    }

    private static void getPyFilesBelowDirectory(PyFileListing result, final Path path, PyFileListingFilter filter,
            IProgressMonitor monitor, boolean addSubFolders, int level, String currModuleRep,
            Set<File> canonicalFolders) {
        FastStringBuffer buf = new FastStringBuffer(currModuleRep, 128);
        if (level != 0) {
            FastStringBuffer newModuleRep = buf;
            if (newModuleRep.length() != 0) {
                newModuleRep.append('.');
            }
            newModuleRep.append(path.getFileName().toString());
            currModuleRep = newModuleRep.toString();
        }

        // check if it is a symlink loop
        try {
            if (Files.isSymbolicLink(path)) {
                File file = path.toFile();
                File canonicalizedDir = file.getCanonicalFile();
                if (!canonicalizedDir.equals(file)) {
                    if (canonicalFolders.contains(canonicalizedDir)) {
                        return;
                    }
                }
                canonicalFolders.add(canonicalizedDir);
            }
        } catch (IOException e) {
            // See: https://www.brainwy.com/tracker/PyDev/921
            // java.io.IOException: Too many levels of symbolic links at java.io.UnixFileSystem.canonicalize0(Native Method)
            // at java.io.UnixFileSystem.canonicalize(UnixFileSystem.java:172)
            // at java.io.File.getCanonicalPath(File.java:618)
            // at java.io.File.getCanonicalFile(File.java:643)
            // at org.python.pydev.ast.listing_utils.PyFileListing.getPyFilesBelow(PyFileListing.java:117)
            return;
        }
        result.foldersFound.add(path.toFile());

        List<Path> foldersLater = new LinkedListWarningOnSlowOperations<Path>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path subpath : stream) {
                if (monitor.isCanceled()) {
                    break;
                }
                File subpathFile = subpath.toFile();
                boolean isDirectory;
                if (Files.isSymbolicLink(subpath)) {
                    isDirectory = subpathFile.isDirectory();
                } else {
                    isDirectory = Files.isDirectory(subpath);
                }
                if (filter != null) {
                    if (!filter.accept(subpath, subpathFile, isDirectory)) {
                        continue;
                    }
                }
                if (!isDirectory) {
                    result.addPyFileInfo(new PyFileInfo(subpathFile, currModuleRep));

                    monitor.worked(1);
                    monitor.setTaskName(buf.clear().append("Found:").append(subpathFile.toString()).toString());
                } else {
                    if (addSubFolders) {
                        foldersLater.add(subpath);
                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }

        for (Path folder : foldersLater) {
            if (monitor.isCanceled()) {
                break;
            }
            getPyFilesBelowDirectory(result, folder, filter, monitor, addSubFolders, level + 1, currModuleRep,
                    canonicalFolders);
            monitor.worked(1);
        }
    }

    /**
     * Returns the directories and python files in a list.
     *
     * @param addSubFolders indicates if sub-folders should be added
     * @param canonicalFolders used to know if we entered a loop in the listing (with symlinks)
     * @return An object with the results of making that listing.
     */
    private static PyFileListing getPyFilesBelowInitial(PyFileListing result, File file, PyFileListingFilter filter,
            IProgressMonitor monitor, boolean addSubFolders, int level, String currModuleRep,
            Set<File> canonicalFolders) {

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        if (file != null && file.exists()) {
            //only check files that actually exist

            if (file.isDirectory()) {
                getPyFilesBelowDirectory(result, file.toPath(), filter, monitor, addSubFolders, level, currModuleRep,
                        canonicalFolders);

            } else { // not dir: must be file
                result.addPyFileInfo(new PyFileInfo(file, currModuleRep));

            }
        }

        return result;
    }

    private static PyFileListing getPyFilesBelow(File file, PyFileListingFilter filter, IProgressMonitor monitor,
            boolean addSubFolders) {
        PyFileListing result = new PyFileListing();
        return getPyFilesBelowInitial(result, file, filter, monitor, addSubFolders, 0, "", new HashSet<File>());
    }

    public static PyFileListing getPyFilesBelow(File file, PyFileListingFilter filter, IProgressMonitor monitor) {
        return getPyFilesBelow(file, filter, monitor, true);
    }

    /**
     * @param includeDirs determines if we can include subdirectories
     * @return a file filter only for python files (and other dirs if specified)
     */
    public static PyFileListingFilter getPyFilesFileFilter(final boolean includeDirs) {

        return new PyFileListingFilter() {

            private final String[] dottedValidSourceFiles = FileTypesPreferences.getDottedValidSourceFiles();

            @Override
            public boolean accept(Path pathname, File file, boolean isDirectory) {
                if (isDirectory) {
                    if (includeDirs) {
                        return true;
                    }
                    return false;
                }
                if (PythonPathHelper.isValidSourceFile(file.toString(), dottedValidSourceFiles)) {
                    return true;
                }
                return false;
            }

        };
    }

    /**
     * Returns the directories and python files in a list.
     *
     * @param file
     * @return tuple with files in pos 0 and folders in pos 1
     */
    public static PyFileListing getPyFilesBelow(File file, IProgressMonitor monitor, final boolean includeDirs) {
        PyFileListingFilter filter = getPyFilesFileFilter(includeDirs);
        return getPyFilesBelow(file, filter, monitor, true);
    }

    /**
     * @return All the IFiles below the current folder that are python files (does not check if it has an __init__ path)
     */
    public static List<IFile> getAllIFilesBelow(IContainer member) {
        final ArrayList<IFile> ret = new ArrayList<IFile>();
        try {
            member.accept(new IResourceVisitor() {

                @Override
                public boolean visit(IResource resource) {
                    if (resource instanceof IFile) {
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

    /**
     * The files we found as being valid for the given filter
     */
    private final List<PyFileInfo> pyFileInfos = new ArrayList<PyFileInfo>();

    /**
     * The folders we found as being valid for the given filter
     */
    private List<File> foldersFound = new ArrayList<File>();

    public PyFileListing() {
    }

    public Collection<PyFileInfo> getFoundPyFileInfos() {
        return pyFileInfos;
    }

    public Collection<File> getFoundFolders() {
        return foldersFound;
    }

    private void addPyFileInfo(PyFileInfo info) {
        pyFileInfos.add(info);
    }

}
