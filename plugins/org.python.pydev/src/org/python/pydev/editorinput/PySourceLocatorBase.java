/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editorinput;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.URIUtil;
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
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.io.FileUtils;
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

    /**
     * @param file the file we want to get in the workspace
     * @return a workspace file that matches the given file.
     */
    public IFile getWorkspaceFile(File file) {
        IFile[] files = getWorkspaceFiles(file);
        return selectWorkspaceFile(files);
    }

    /**
     * @param file the file we want to get in the workspace
     * @return a workspace file that matches the given file.
     */
    public IFile[] getWorkspaceFiles(File file) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IFile[] files = workspace.getRoot().findFilesForLocationURI(file.toURI());
        files = filterNonExistentFiles(files);
        if (files == null || files.length == 0) {
            return null;
        }

        return files;
    }

    public IContainer getWorkspaceContainer(File file) {
        IContainer[] containers = getWorkspaceContainers(file);
        if (containers == null || containers.length < 1) {
            return null;
        }
        return containers[0];
    }

    public IContainer[] getWorkspaceContainers(File file) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IContainer[] containers = workspace.getRoot().findContainersForLocationURI(file.toURI());
        containers = filterNonExistentContainers(containers);
        if (containers == null || containers.length == 0) {
            return null;
        }

        return containers;
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

        IWorkspace w = ResourcesPlugin.getWorkspace();

        //let's start with the 'easy' way
        IFile fileForLocation = w.getRoot().getFileForLocation(path);
        if (fileForLocation != null && fileForLocation.exists()
                && (project == null || project == fileForLocation.getProject())) {
            //if a project was specified, make sure the file found comes from that project
            return new FileEditorInput(fileForLocation);
        }

        IFile files[] = w.getRoot().findFilesForLocationURI(URIUtil.toURI(path));
        if (files == null || files.length == 0 || !files[0].exists()) {
            //it is probably an external file
            File systemFile = path.toFile();
            if (systemFile.exists()) {
                edInput = createEditorInput(systemFile);
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
                    List<IFile> likelyFiles = getLikelyFiles(path, w);
                    IFile iFile = selectWorkspaceFile(likelyFiles.toArray(new IFile[0]));
                    if (iFile != null) {
                        PySourceLocatorPrefs.addPathTranslation(path, iFile.getLocation());
                        return new FileEditorInput(iFile);
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
        } else { //file exists
            IFile workspaceFile = null;
            if (project != null) { //check for file in current project, and select it
                IProject[] refProjects;
                try {
                    refProjects = project.getDescription().getReferencedProjects();
                } catch (CoreException e) {
                    Log.log("Error accessing referenced projects.", e);
                    refProjects = new IProject[0];
                }
                int i = -1;
                do {
                    IProject searchProject = (i == -1 ? project : refProjects[i]);
                    for (IFile file : files) {
                        if (file.getProject().equals(searchProject)) {
                            workspaceFile = file;
                            i = refProjects.length; //to break out of parent loop
                            break;
                        }
                    }
                } while (++i < refProjects.length);
            }
            if (workspaceFile == null) { //if project doesn't contain the file, let user select
                workspaceFile = selectWorkspaceFile(files);
            }
            if (workspaceFile != null) {
                edInput = new FileEditorInput(workspaceFile);
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
     * Creates some editor input for the passed file
     * @param file the file for which an editor input should be created
     * @return the editor input that'll open the passed file.
     */
    private IEditorInput createEditorInput(File file) {
        IFile[] workspaceFile = getWorkspaceFiles(file);
        if (workspaceFile != null && workspaceFile.length > 0) {
            IFile file2 = selectWorkspaceFile(workspaceFile);
            if (file2 != null) {
                return new FileEditorInput(file2);
            } else {
                return new FileEditorInput(workspaceFile[0]);
            }
        }
        return PydevFileEditorInput.create(file, true);
    }

    /**
     * @param files the files that should be filtered
     * @return a new array of IFile with only the files that actually exist.
     */
    private IFile[] filterNonExistentFiles(IFile[] files) {
        if (files == null)
            return null;

        int length = files.length;
        ArrayList<IFile> existentFiles = new ArrayList<IFile>(length);
        for (int i = 0; i < length; i++) {
            if (files[i].exists())
                existentFiles.add(files[i]);
        }
        return (IFile[]) existentFiles.toArray(new IFile[existentFiles.size()]);
    }

    /**
     * @param containers the containers that should be filtered
     * @return a new array of IContainer with only the containers that actually exist.
     */
    public IContainer[] filterNonExistentContainers(IContainer[] containers) {
        if (containers == null)
            return null;

        int length = containers.length;
        ArrayList<IContainer> existentFiles = new ArrayList<IContainer>(length);
        for (int i = 0; i < length; i++) {
            if (containers[i].exists())
                existentFiles.add(containers[i]);
        }
        return (IContainer[]) existentFiles.toArray(new IContainer[existentFiles.size()]);
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
}
