/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editorinput;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.core.IPyStackFrame;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * Refactored from the PydevPlugin: helpers to find some IFile / IEditorInput 
 * from a Path (or java.io.File) 
 * 
 * @author fabioz
 */
public class PySourceLocatorBase {

    /**
     * This method will try to find the most likely file that matches the given path,
     * considering:
     * - The workspace files
     * - The open editors
     * 
     * and if all fails, it'll still ask the user which path should be used.
     * 
     * 
     * @param path
     * @return
     */
    public IEditorInput createEditorInput(IPath path, IProject project) {
        return createEditorInput(path, true, null, project);
    }

    public IEditorInput createEditorInput(IPath path) {
        return createEditorInput(path, true, null, null);
    }

    public IFile[] getFilesForLocation(IPath location, IProject project, boolean stopOnFirst) {
        return GetFiles.getFilesForLocation(location, project, stopOnFirst);

    }

    public IFile getFileForLocation(IPath location, IProject project) {
        return GetFiles.getFileForLocation(location, project);
    }

    /**
     * @param file the file we want to get in the workspace
     * @return a workspace file that matches the given file.
     */
    public IFile getWorkspaceFile(File file, IProject project) {
        return getFileForLocation(Path.fromOSString(file.getAbsolutePath()), project);
    }

    /**
     * @param file the file we want to get in the workspace
     * @return a workspace file that matches the given file.
     */
    public IFile[] getWorkspaceFiles(File file) {
        boolean stopOnFirst = false;
        IFile[] files = getFilesForLocation(Path.fromOSString(file.getAbsolutePath()), null, stopOnFirst);
        if (files == null || files.length == 0) {
            return null;
        }

        return files;
    }

    public IContainer getContainerForLocation(IPath location, IProject project) {
        return GetContainers.getContainerForLocation(location, project);
    }

    public IContainer[] getContainersForLocation(IPath location) {
        boolean stopOnFirst = false;
        return GetContainers.getContainersForLocation(location, null, stopOnFirst);
    }

    //---------------------------------- PRIVATE API BELOW --------------------------------------------

    /**
     * Creates the editor input from a given path.
     * 
     * @param path the path for the editor input we're looking
     * @param askIfDoesNotExist if true, it'll try to ask the user/check existing editors and look
     * in the workspace for matches given the name
     * @param project if provided, and if a matching file is found in this project, that file will be
     * opened before asking the user to select from a list of all matches
     * 
     * @return the editor input found or none if None was available for the given path
     */
    public IEditorInput createEditorInput(IPath path, boolean askIfDoesNotExist, IPyStackFrame pyStackFrame,
            IProject project) {
        int onSourceNotFound = PySourceLocatorPrefs.getOnSourceNotFound();
        IEditorInput edInput = null;

        String pathTranslation = PySourceLocatorPrefs.getPathTranslation(path);
        if (pathTranslation != null) {
            if (!pathTranslation.equals(PySourceLocatorPrefs.DONTASK)) {
                //change it for the registered translation
                path = Path.fromOSString(pathTranslation);
            } else {
                //DONTASK!!
                askIfDoesNotExist = false;
            }
        }

        IFile fileForLocation = getFileForLocation(path, project);
        if (fileForLocation != null && fileForLocation.exists()) {
            //if a project was specified, make sure the file found comes from that project
            return new FileEditorInput(fileForLocation);
        }

        //getFileForLocation() will search all projects starting with the one we pass and references,
        //so, if not found there, it is probably an external file
        File systemFile = path.toFile();
        if (systemFile.exists()) {
            edInput = PydevFileEditorInput.create(systemFile, true);
        }

        if (edInput == null) {
            //here we can do one more thing: if the file matches some opened editor, let's use it...
            //(this is done because when debugging, we don't want to be asked over and over
            //for the same file)
            IEditorInput input = getEditorInputFromExistingEditors(systemFile.getName());
            if (input != null) {
                return input;
            }

            if (askIfDoesNotExist
                    && (onSourceNotFound == PySourceLocatorPrefs.ASK_FOR_FILE || onSourceNotFound == PySourceLocatorPrefs.ASK_FOR_FILE_GET_FROM_SERVER)) {

                //this is the last resort... First we'll try to check for a 'good' match,
                //and if there's more than one we'll ask it to the user
                IWorkspace w = ResourcesPlugin.getWorkspace();
                List<IFile> likelyFiles = getLikelyFiles(path, w);
                IFile iFile = selectWorkspaceFile(likelyFiles.toArray(new IFile[0]));
                if (iFile != null) {
                    IPath location = iFile.getLocation();
                    if (location != null) {
                        PySourceLocatorPrefs.addPathTranslation(path, location);
                        return new FileEditorInput(iFile);
                    }
                }

                //ok, ask the user for any file in the computer
                IEditorInput pydevFileEditorInput = selectFilesystemFileForPath(path);
                input = pydevFileEditorInput;
                if (input != null) {
                    File file = PydevFileEditorInput.getFile(pydevFileEditorInput);
                    if (file != null) {
                        PySourceLocatorPrefs.addPathTranslation(path,
                                Path.fromOSString(FileUtils.getFileAbsolutePath(file)));
                        return input;
                    }
                }

                PySourceLocatorPrefs.setIgnorePathTranslation(path);
            }
        }

        if (edInput == null
                && (onSourceNotFound == PySourceLocatorPrefs.ASK_FOR_FILE_GET_FROM_SERVER || onSourceNotFound == PySourceLocatorPrefs.GET_FROM_SERVER)) {
            if (pyStackFrame != null) {
                try {
                    String fileContents = pyStackFrame.getFileContents();
                    if (fileContents != null && fileContents.length() > 0) {
                        String lastSegment = path.lastSegment();
                        File workspaceMetadataFile = PydevPlugin.getWorkspaceMetadataFile("temporary_files");
                        if (!workspaceMetadataFile.exists()) {
                            workspaceMetadataFile.mkdirs();
                        }
                        File file = new File(workspaceMetadataFile, lastSegment);
                        try {
                            if (file.exists()) {
                                file.delete();
                            }
                        } catch (Exception e) {
                        }
                        FileUtils.writeStrToFile(fileContents, file);
                        try {
                            file.setReadOnly();
                        } catch (Exception e) {
                        }
                        edInput = PydevFileEditorInput.create(file, true);
                    }

                } catch (Exception e) {
                    Log.log(e);
                }

            }
        }
        return edInput;
    }

    /**
     * @param matchName the name to match in the editor
     * @return an editor input from an existing editor available
     */
    private IEditorInput getEditorInputFromExistingEditors(final String matchName) {
        final Tuple<IWorkbenchWindow, IEditorInput> workbenchAndReturn = new Tuple<IWorkbenchWindow, IEditorInput>(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow(), null);

        Runnable r = new Runnable() {

            public void run() {
                IWorkbenchWindow workbenchWindow = workbenchAndReturn.o1;
                if (workbenchWindow == null) {
                    workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                }

                if (workbenchWindow == null) {
                    return;
                }

                IWorkbenchPage activePage = workbenchWindow.getActivePage();
                if (activePage == null) {
                    return;
                }

                IEditorReference[] editorReferences = activePage.getEditorReferences();
                for (IEditorReference editorReference : editorReferences) {
                    IEditorPart editor = editorReference.getEditor(false);
                    if (editor != null) {
                        if (editor instanceof PyEdit) {
                            PyEdit pyEdit = (PyEdit) editor;
                            IEditorInput editorInput = pyEdit.getEditorInput();
                            if (editorInput instanceof IPathEditorInput) {
                                IPathEditorInput pathEditorInput = (IPathEditorInput) editorInput;
                                IPath localPath = pathEditorInput.getPath();
                                if (localPath != null) {
                                    String considerName = localPath.segment(localPath.segmentCount() - 1);
                                    if (matchName.equals(considerName)) {
                                        workbenchAndReturn.o2 = editorInput;
                                        return;
                                    }
                                }
                            } else {
                                File editorFile = pyEdit.getEditorFile();
                                if (editorFile != null) {
                                    if (editorFile.getName().equals(matchName)) {
                                        workbenchAndReturn.o2 = editorInput;
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };

        if (workbenchAndReturn.o1 == null) { //not ui-thread
            Display.getDefault().syncExec(r);
        } else {
            r.run();
        }

        return workbenchAndReturn.o2;
    }

    /**
     * This is the last resort... pointing to some filesystem file to get the editor for some path.
     */
    protected IEditorInput selectFilesystemFileForPath(final IPath path) {
        final List<String> l = new ArrayList<String>();
        Runnable r = new Runnable() {

            public void run() {
                Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                FileDialog dialog = new FileDialog(shell);
                dialog.setText(path + " - select correspondent filesystem file.");
                String[] wildcardValidSourceFiles = FileTypesPreferencesPage.getWildcardValidSourceFiles();
                wildcardValidSourceFiles = StringUtils.addString(wildcardValidSourceFiles, "*");
                dialog.setFilterExtensions(wildcardValidSourceFiles);
                String string = dialog.open();
                if (string != null) {
                    l.add(string);
                }
            }
        };
        if (Display.getCurrent() == null) { //not ui-thread
            Display.getDefault().syncExec(r);
        } else {
            r.run();
        }
        if (l.size() > 0) {
            String fileAbsolutePath = FileUtils.getFileAbsolutePath(l.get(0));
            return PydevFileEditorInput.create(new File(fileAbsolutePath), true);
        }
        return null;
    }

    /**
     * This method will pass all the files in the workspace and check if there's a file that might
     * be a match to some path (use only as an almost 'last-resort').
     */
    private List<IFile> getLikelyFiles(IPath path, IWorkspace w) {
        List<IFile> ret = new ArrayList<IFile>();
        try {
            IResource[] resources = w.getRoot().members();
            getLikelyFiles(path, ret, resources);
        } catch (CoreException e) {
            Log.log(e);
        }
        return ret;
    }

    /**
     * Used to recursively get the likely files given the first set of containers
     */
    private void getLikelyFiles(IPath path, List<IFile> ret, IResource[] resources) throws CoreException {
        String strPath = path.removeFileExtension().lastSegment().toLowerCase(); //this will return something as 'foo'

        for (IResource resource : resources) {
            if (resource instanceof IFile) {
                IFile f = (IFile) resource;

                if (PythonPathHelper.isValidSourceFile(f)) {
                    if (resource.getFullPath().removeFileExtension().lastSegment().toLowerCase().equals(strPath)) {
                        ret.add((IFile) resource);
                    }
                }
            } else if (resource instanceof IContainer) {
                getLikelyFiles(path, ret, ((IContainer) resource).members());
            }
        }
    }

    /**
     * Ask the user to select one file of the given list of files (if some is available)
     * 
     * @param files the files available for selection.
     * @return the selected file (from the files passed) or null if there was no file available for
     * selection or if the user canceled it.
     */
    private IFile selectWorkspaceFile(final IFile[] files) {
        if (files == null || files.length == 0) {
            return null;
        }
        if (files.length == 1) {
            return files[0];
        }
        final List<IFile> selected = new ArrayList<IFile>();

        Runnable r = new Runnable() {
            public void run() {
                Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new PyFileLabelProvider());
                dialog.setElements(files);
                dialog.setTitle("Select Workspace File");
                dialog.setMessage("File may be matched to multiple files in the workspace.");
                if (dialog.open() == Window.OK) {
                    selected.add((IFile) dialog.getFirstResult());
                }
            }

        };
        if (Display.getCurrent() == null) { //not ui-thread
            Display.getDefault().syncExec(r);
        } else {
            r.run();
        }
        if (selected.size() > 0) {
            return selected.get(0);
        }
        return null;
    }

    private static class GetFiles {
        /**
         * This method is a workaround for w.getRoot().getFileForLocation(path); which does not work consistently because
         * it filters out files which should not be filtered (i.e.: if a project is not in the workspace but imported).
         * 
         * Also, it can fail to get resources in linked folders in the pythonpath.
         * 
         * @param project is optional (may be null): if given we'll search in it dependencies first.
         */
        private static IFile getFileForLocation(IPath location, IProject project) {
            boolean stopOnFirst = true;
            IFile[] filesForLocation = getFilesForLocation(location, project, stopOnFirst);
            if (filesForLocation != null && filesForLocation.length > 0) {
                return filesForLocation[0];
            }
            return null;
        }

        /**
         * This method is a workaround for w.getRoot().getFilesForLocation(path); which does not work consistently because
         * it filters out files which should not be filtered (i.e.: if a project is not in the workspace but imported).
         * 
         * Also, it can fail to get resources in linked folders in the pythonpath.
         * 
         * @param project is optional (may be null): if given we'll search in it dependencies first.
         */
        private static IFile[] getFilesForLocation(IPath location, IProject project, boolean stopOnFirst) {
            ArrayList<IFile> lst = new ArrayList<>();
            HashSet<IProject> checked = new HashSet<>();
            IWorkspace w = ResourcesPlugin.getWorkspace();
            if (project != null) {
                checked.add(project);
                IFile f = getFileInProject(location, project);
                if (f != null) {
                    if (stopOnFirst) {
                        return new IFile[] { f };
                    } else {
                        lst.add(f);
                    }
                }
                try {
                    IProject[] referencedProjects = project.getDescription().getReferencedProjects();
                    for (int i = 0; i < referencedProjects.length; i++) {
                        IProject p = referencedProjects[i];
                        checked.add(p);
                        f = getFileInProject(location, p);
                        if (f != null) {
                            if (stopOnFirst) {
                                return new IFile[] { f };
                            } else {
                                lst.add(f);
                            }
                        }

                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }

            IProject[] projects = w.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
            for (int i = 0; i < projects.length; i++) {
                IProject p = projects[i];
                if (checked.contains(p)) {
                    continue;
                }
                checked.add(p);
                IFile f = getFileInProject(location, p);
                if (f != null) {
                    if (stopOnFirst) {
                        return new IFile[] { f };
                    } else {
                        lst.add(f);
                    }
                }
            }
            return lst.toArray(new IFile[lst.size()]);
        }

        /**
         * Gets an IFile inside a container given a path in the filesystem (resolves the full path of the container and
         * checks if the location given is under it).
         */
        private static IFile getFileInContainer(IPath location, IContainer container) {
            IPath projectLocation = container.getLocation();
            if (projectLocation != null) {
                if (projectLocation.isPrefixOf(location)) {
                    int segmentsToRemove = projectLocation.segmentCount();
                    IPath removingFirstSegments = location.removeFirstSegments(segmentsToRemove);
                    if (removingFirstSegments.segmentCount() == 0) {
                        //It's equal: as we want a file in the container, and the path to the file is equal to the
                        //container, we have to return null (because it's equal to the container it cannot be a file).
                        return null;
                    }
                    IFile file = container.getFile(removingFirstSegments);
                    if (file.exists()) {
                        return file;
                    }
                }
            } else {
                if (container instanceof IProject) {
                    Log.logInfo("Info: Project: " + container + " has no associated location.");
                }
            }
            return null;
        }

        /**
         * Tries to get a file from a project. Considers source folders (which could be linked) or resources directly beneath
         * the project.
         * @param location this is the path location to be gotten.
         * @param project this is the project we're searching.
         * @return the file found or null if it was not found.
         */
        private static IFile getFileInProject(IPath location, IProject project) {
            IFile file = getFileInContainer(location, project);
            if (file != null) {
                return file;
            }
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature != null) {
                IPythonPathNature pythonPathNature = nature.getPythonPathNature();
                try {
                    //Paths
                    Set<IResource> projectSourcePathSet = pythonPathNature.getProjectSourcePathFolderSet();
                    for (IResource iResource : projectSourcePathSet) {
                        if (iResource instanceof IContainer) {
                            //I.e.: don't consider zip files
                            IContainer iContainer = (IContainer) iResource;
                            file = getFileInContainer(location, iContainer);
                            if (file != null) {
                                return file;
                            }
                        }
                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
            return null;
        }

    }

    private static class GetContainers {
        /**
         * This method is a workaround for w.getRoot().getContainerForLocation(path); which does not work consistently because
         * it filters out files which should not be filtered (i.e.: if a project is not in the workspace but imported).
         * 
         * Also, it can fail to get resources in linked folders in the pythonpath.
         * 
         * @param project is optional (may be null): if given we'll search in it dependencies first.
         */
        private static IContainer getContainerForLocation(IPath location, IProject project) {
            boolean stopOnFirst = true;
            IContainer[] filesForLocation = getContainersForLocation(location, project, stopOnFirst);
            if (filesForLocation != null && filesForLocation.length > 0) {
                return filesForLocation[0];
            }
            return null;
        }

        /**
         * This method is a workaround for w.getRoot().getContainersForLocation(path); which does not work consistently because
         * it filters out files which should not be filtered (i.e.: if a project is not in the workspace but imported).
         * 
         * Also, it can fail to get resources in linked folders in the pythonpath.
         * 
         * @param project is optional (may be null): if given we'll search in it dependencies first.
         */
        private static IContainer[] getContainersForLocation(IPath location, IProject project, boolean stopOnFirst) {
            ArrayList<IContainer> lst = new ArrayList<>();
            HashSet<IProject> checked = new HashSet<>();
            IWorkspace w = ResourcesPlugin.getWorkspace();
            if (project != null) {
                checked.add(project);
                IContainer f = getContainerInProject(location, project);
                if (f != null) {
                    if (stopOnFirst) {
                        return new IContainer[] { f };
                    } else {
                        lst.add(f);
                    }
                }
                try {
                    IProject[] referencedProjects = project.getDescription().getReferencedProjects();
                    for (int i = 0; i < referencedProjects.length; i++) {
                        IProject p = referencedProjects[i];
                        checked.add(p);
                        f = getContainerInProject(location, p);
                        if (f != null) {
                            if (stopOnFirst) {
                                return new IContainer[] { f };
                            } else {
                                lst.add(f);
                            }
                        }

                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }

            IProject[] projects = w.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
            for (int i = 0; i < projects.length; i++) {
                IProject p = projects[i];
                if (checked.contains(p)) {
                    continue;
                }
                checked.add(p);
                IContainer f = getContainerInProject(location, p);
                if (f != null) {
                    if (stopOnFirst) {
                        return new IContainer[] { f };
                    } else {
                        lst.add(f);
                    }
                }
            }
            return lst.toArray(new IContainer[lst.size()]);
        }

        /**
         * Gets an IContainer inside a container given a path in the filesystem (resolves the full path of the container and
         * checks if the location given is under it).
         */
        private static IContainer getContainerInContainer(IPath location, IContainer container) {
            IPath projectLocation = container.getLocation();
            if (projectLocation != null && projectLocation.isPrefixOf(location)) {
                int segmentsToRemove = projectLocation.segmentCount();
                IPath removeFirstSegments = location.removeFirstSegments(segmentsToRemove);
                if (removeFirstSegments.segmentCount() == 0) {
                    return container; //I.e.: equal to container
                }
                IContainer file = container.getFolder(removeFirstSegments);
                if (file.exists()) {
                    return file;
                }
            }
            return null;
        }

        /**
         * Tries to get a file from a project. Considers source folders (which could be linked) or resources directly beneath
         * the project.
         * @param location this is the path location to be gotten.
         * @param project this is the project we're searching.
         * @return the file found or null if it was not found.
         */
        private static IContainer getContainerInProject(IPath location, IProject project) {
            IContainer file = getContainerInContainer(location, project);
            if (file != null) {
                return file;
            }
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature != null) {
                IPythonPathNature pythonPathNature = nature.getPythonPathNature();
                try {
                    //Paths
                    Set<IResource> projectSourcePathSet = pythonPathNature.getProjectSourcePathFolderSet();
                    for (IResource iResource : projectSourcePathSet) {
                        if (iResource instanceof IContainer) {
                            //I.e.: don't consider zip files
                            IContainer iContainer = (IContainer) iResource;
                            file = getContainerInContainer(location, iContainer);
                            if (file != null) {
                                return file;
                            }
                        }
                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
            return null;
        }

    }

}
