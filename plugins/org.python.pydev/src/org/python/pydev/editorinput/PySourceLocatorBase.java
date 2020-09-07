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
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.ast.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.ast.location.FindWorkspaceFiles;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPyStackFrame;
import org.python.pydev.core.editor.OpenEditors;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.editor_input.EditorInputUtils;

/**
 * Helpers to get an editor input for some path. Prioritizes getting an input from
 * an editor which is currently opened.
 */
public class PySourceLocatorBase {

    /**
     * Helper class.
     */
    private abstract static class FindFromExistingEditors {
        private final Object matchName;

        protected FindFromExistingEditors(Object matchName) {
            this.matchName = matchName;
        }

        public IEditorInput findFromOpenedPyEdits() {
            Object ret = OpenEditors.iterOpenEditorsUntilFirstReturn(new ICallback<Object, IPyEdit>() {

                @Override
                public Object call(IPyEdit pyEdit) {
                    IEditorInput editorInput = (IEditorInput) pyEdit.getEditorInput();
                    if (editorInput instanceof IPathEditorInput) {
                        IPathEditorInput pathEditorInput = (IPathEditorInput) editorInput;
                        IPath localPath = pathEditorInput.getPath();
                        if (localPath != null) {
                            if (matchesPath(matchName, editorInput, localPath)) {
                                return editorInput;
                            }
                        }
                    } else {
                        File editorFile = pyEdit.getEditorFile();
                        if (editorFile != null) {
                            if (matchesFile(matchName, editorInput, editorFile)) {
                                return editorInput;
                            }
                        }
                    }
                    return null;
                }
            });
            return (IEditorInput) ret;
        }

        protected abstract boolean matchesFile(final Object match, IEditorInput editorInput, File editorFile);

        protected abstract boolean matchesPath(final Object match, IEditorInput editorInput, IPath localPath);

    }

    private static class FindFromExistingEditorsName extends PySourceLocatorBase.FindFromExistingEditors {
        protected FindFromExistingEditorsName(String matchName) {
            super(matchName);
        }

        @Override
        protected boolean matchesFile(final Object match, IEditorInput editorInput,
                File editorFile) {
            String matchName = (String) match;
            if (editorFile.getName().equals(matchName)) {
                return true;
            }
            return false;
        }

        @Override
        protected boolean matchesPath(final Object match, IEditorInput editorInput, IPath localPath) {
            String matchName = (String) match;
            String considerName = localPath.segment(localPath.segmentCount() - 1);
            if (matchName.equals(considerName)) {
                return true;
            }
            return false;
        }

    }

    private static class FindFromExistingEditorsFile extends PySourceLocatorBase.FindFromExistingEditors {
        protected FindFromExistingEditorsFile(File matchFile) {
            super(matchFile);
        }

        @Override
        protected boolean matchesFile(final Object match, IEditorInput editorInput,
                File editorFile) {
            File matchFile = (File) match;
            if (editorFile.equals(matchFile)) {
                return true;
            }
            return false;
        }

        @Override
        protected boolean matchesPath(final Object match, IEditorInput editorInput, IPath localPath) {
            File matchFile = (File) match;
            // Note: check by the file because it takes into account casing rules on Windows.
            return matchFile.equals(localPath.toFile());
        }

    }

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
    public IEditorInput createEditorInput(final IPath initialPath, boolean askIfDoesNotExist,
            IPyStackFrame pyStackFrame,
            IProject project) {
        int onSourceNotFound = PySourceLocatorPrefs.getOnSourceNotFound();
        boolean isFileLoadedFromDebugger = pyStackFrame != null ? pyStackFrame.isFileLoadedFromDebugger(initialPath)
                : false;

        if (isFileLoadedFromDebugger) {
            if (onSourceNotFound == PySourceLocatorPrefs.ASK_FOR_FILE_GET_FROM_SERVER
                    || onSourceNotFound == PySourceLocatorPrefs.GET_FROM_SERVER) {
                return (IEditorInput) pyStackFrame.getEditorInputFromLoadedSource(initialPath);
            }
        }

        File systemFile = initialPath.toFile();
        IEditorInput input = getEditorInputFromExistingEditors(systemFile);
        if (input != null) {
            // The same filename for different frames can have a different source, so, we
            // need to double check its contents (if it doesn't match we need to create
            // a new file).
            return input;
        }

        IPath path = initialPath;
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

        IFile fileForLocation = FindWorkspaceFiles.getFileForLocation(path, project);
        if (fileForLocation != null && fileForLocation.exists()) {
            //if a project was specified, make sure the file found comes from that project
            return new FileEditorInput(fileForLocation);
        }

        //getFileForLocation() will search all projects starting with the one we pass and references,
        //so, if not found there, it is probably an external file
        if (systemFile.exists()) {
            // Note: no need to check if we have an exact match for the file in the local filesystem.
            return EditorInputFactory.create(systemFile, true);
        }

        //here we can do one more thing: if the file matches some opened editor, let's use it...
        //(this is done because when debugging, we don't want to be asked over and over
        //for the same file)
        input = getEditorInputFromExistingEditors(systemFile.getName());
        if (input != null) {
            return input;
        }

        if (askIfDoesNotExist
                && (onSourceNotFound == PySourceLocatorPrefs.ASK_FOR_FILE
                        || onSourceNotFound == PySourceLocatorPrefs.ASK_FOR_FILE_GET_FROM_SERVER)) {

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
                File file = EditorInputUtils.getFile(pydevFileEditorInput);
                if (file != null) {
                    PySourceLocatorPrefs.addPathTranslation(path,
                            Path.fromOSString(FileUtils.getFileAbsolutePath(file)));
                    return input;
                }
            }

            PySourceLocatorPrefs.setIgnorePathTranslation(path);
        }

        if (onSourceNotFound == PySourceLocatorPrefs.ASK_FOR_FILE_GET_FROM_SERVER
                || onSourceNotFound == PySourceLocatorPrefs.GET_FROM_SERVER) {
            if (pyStackFrame != null) {
                return (IEditorInput) pyStackFrame.getEditorInputFromLoadedSource(initialPath);
            }
        }
        return null;
    }

    /**
     * @param matchFile the file to match in the editor
     * @return an editor input from an existing editor available
     */
    private IEditorInput getEditorInputFromExistingEditors(final File matchFile) {
        return new FindFromExistingEditorsFile(matchFile).findFromOpenedPyEdits();
    }

    /**
     * @param matchName the name to match in the editor
     * @return an editor input from an existing editor available
     */
    private IEditorInput getEditorInputFromExistingEditors(final String matchName) {
        return new FindFromExistingEditorsName(matchName).findFromOpenedPyEdits();
    }

    /**
     * This is the last resort... pointing to some filesystem file to get the editor for some path.
     */
    protected IEditorInput selectFilesystemFileForPath(final IPath path) {
        final List<String> l = new ArrayList<String>();
        Runnable r = new Runnable() {

            @Override
            public void run() {
                Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                FileDialog dialog = new FileDialog(shell);
                dialog.setText(path + " - select correspondent filesystem file.");
                String[] wildcardValidSourceFiles = FileTypesPreferences.getWildcardValidSourceFiles();
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
            return EditorInputFactory.create(new File(fileAbsolutePath), true);
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
                if (resource instanceof IProject) {
                    if (!((IProject) resource).isOpen()) {
                        continue;
                    }
                }
                try {
                    getLikelyFiles(path, ret, ((IContainer) resource).members());
                } catch (CoreException e) {
                    Log.log(e);
                }
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
            @Override
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
