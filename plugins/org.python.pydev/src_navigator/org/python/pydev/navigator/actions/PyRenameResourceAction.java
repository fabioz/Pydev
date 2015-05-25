/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.ModuleRenameRefactoringRequest;
import org.python.pydev.editor.refactoring.PyRefactoringRequest;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_core.structure.OrderedMap;
import org.python.pydev.shared_ui.dialogs.DialogHelpers;

public class PyRenameResourceAction extends RenameResourceAction {

    private ISelectionProvider provider;

    private List<IResource> selected;
    private IFolder renamedFolder;
    private List<IResource> preResources;

    private Shell shell;

    public PyRenameResourceAction(Shell shell, ISelectionProvider selectionProvider) {
        super(shell);
        this.shell = shell;
        this.provider = selectionProvider;
    }

    /**
     * Return the new name to be given to the target resource.
     *
     * @return java.lang.String
     * @param resource the resource to query status on
     *
     * Fix from platform: was not checking return from dialog.open
     */
    @Override
    protected String queryNewResourceName(final IResource resource) {
        final IWorkspace workspace = IDEWorkbenchPlugin.getPluginWorkspace();
        final IPath prefix = resource.getFullPath().removeLastSegments(1);
        IInputValidator validator = new IInputValidator() {
            public String isValid(String string) {
                if (resource.getName().equals(string)) {
                    return IDEWorkbenchMessages.RenameResourceAction_nameMustBeDifferent;
                }
                IStatus status = workspace.validateName(string, resource.getType());
                if (!status.isOK()) {
                    return status.getMessage();
                }
                if (workspace.getRoot().exists(prefix.append(string))) {
                    return IDEWorkbenchMessages.RenameResourceAction_nameExists;
                }
                return null;
            }
        };

        InputDialog dialog = new InputDialog(shell, IDEWorkbenchMessages.RenameResourceAction_inputDialogTitle,
                IDEWorkbenchMessages.RenameResourceAction_inputDialogMessage, resource.getName(), validator);
        dialog.setBlockOnOpen(true);
        if (dialog.open() == Window.OK) {
            return dialog.getValue();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        selected = new ArrayList<IResource>();

        ISelection selection = provider.getSelection();
        if (!selection.isEmpty()) {
            IStructuredSelection sSelection = (IStructuredSelection) selection;
            if (sSelection.size() >= 1) {
                Iterator iterator = sSelection.iterator();
                while (iterator.hasNext()) {
                    Object element = iterator.next();
                    if (element instanceof IAdaptable) {
                        IAdaptable adaptable = (IAdaptable) element;
                        IResource resource = adaptable.getAdapter(IResource.class);
                        if (resource != null && resource.isAccessible()) {
                            selected.add(resource);
                            continue;
                        }
                    }
                    // one of the elements did not satisfy the condition
                    selected = null;
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Update the PYTHONPATH of the project containing a renamed folder by replacing the folder's
     * old path with its new one (if the folder itself is in the PYTHONPATH), and updating the
     * paths of any of its children that are in the PYTHONPATH.
     */
    private void updatePyPath() {
        if (renamedFolder == null) {
            return;
        }
        IProject project = renamedFolder.getProject();
        IPath oldPath = renamedFolder.getFullPath();
        try {
            IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
            // Quit if the renamed resource is not from a Python project.
            if (pythonPathNature == null) {
                return;
            }
            OrderedMap<String, String> projectSourcePathMap = pythonPathNature
                    .getProjectSourcePathResolvedToUnresolvedMap();
            List<IPath> sourcePaths = new LinkedListWarningOnSlowOperations<IPath>();
            List<IPath> actualPaths = new ArrayList<IPath>(); //non-resolved
            for (String pathName : projectSourcePathMap.keySet()) {
                sourcePaths.add(new Path(pathName));
                actualPaths.add(new Path(projectSourcePathMap.get(pathName)));
            }

            // Find the new name of the resource by finding the path that did not exist beforehand
            List<IResource> postResources = new ArrayList<IResource>();
            for (IResource r : renamedFolder.getParent().members()) {
                postResources.add(r);
            }
            for (IResource r : preResources) {
                postResources.remove(r);
            }
            // Quit if no resource was renamed
            if (postResources.size() == 0) {
                return;

            } else if (postResources.size() > 1) {
                Log.log("Unexpected error. There is more than one renamed file.");
                return;
            }
            boolean changedSomething = false;
            IPath newPath = postResources.get(0).getFullPath();
            int i = 0;
            while (sourcePaths.size() > 0) {
                IPath sourcePath = sourcePaths.remove(0);
                // If renamed resource is a source folder, just do this quick change:
                if (oldPath.equals(sourcePath)) {
                    actualPaths.set(i, actualPaths.get(i).removeLastSegments(1).append(newPath.lastSegment()));
                    changedSomething = true;
                }
                // If renamed resource is a prefix of a source folder, need more work.
                else if (oldPath.isPrefixOf(sourcePath)) {
                    sourcePath = newPath.append(sourcePath.removeFirstSegments(newPath.segmentCount()));
                    // Remove all trailing variable path separators that match the resolved one,
                    // and append the non-matching part of the new resolved path to the var path.
                    IPath actualPath = actualPaths.get(i);
                    int match = 0;
                    int segS = sourcePath.segmentCount();
                    int segV = actualPath.segmentCount();
                    while (match <= segS && match <= segV
                            && sourcePath.segment(segS - 1 - match).equals(actualPath.segment(segV - 1 - match))) {
                        match++;
                    }
                    actualPaths.set(i, actualPath.removeLastSegments(match + 1)
                            .append(sourcePath.removeFirstSegments(segS - match - 1)));
                    changedSomething = true;
                }
                i++;
            }
            if (!changedSomething) {
                return;
            }

            pythonPathNature.setProjectSourcePath(StringUtils.join("|", actualPaths));
            PythonNature.getPythonNature(project).rebuildPath();
        } catch (CoreException e) {
            Log.log(IStatus.ERROR, "Unexpected error setting project properties", e);
        }
    }

    @Override
    protected List<IResource> getSelectedResources() {
        return selected;
    }

    @Override
    public IStructuredSelection getStructuredSelection() {
        return new StructuredSelection(selected);
    }

    /*
     * (non-Javadoc) Method declared on IAction.
     */
    @Override
    public void run() {
        if (!isEnabled()) { //will also update the list of resources (main change from the DeleteResourceAction)
            return;
        }
        IEditorPart[] dirtyEditors = Helpers.checkValidateState();
        List<IResource> resources = getSelectedResources();

        if (resources.size() != 1) {
            DialogHelpers.openWarning("Can only rename one element.", "One element must be selected for rename.");
            return;
        }

        IResource r = resources.get(0);
        if (r instanceof IFile) {
            for (IEditorPart iEditorPart : dirtyEditors) {
                IEditorInput editorInput = iEditorPart.getEditorInput();
                Object input = editorInput.getAdapter(IResource.class);
                if (r.equals(input)) {
                    iEditorPart.doSave(null);
                }
            }
        } else if (r instanceof IFolder) {
            try {
                renamedFolder = (IFolder) r;
                preResources = new ArrayList<IResource>();
                IResource[] members = renamedFolder.getParent().members();
                for (IResource m : members) {
                    preResources.add(m);
                }
            } catch (CoreException e) {
                Log.log(IStatus.ERROR, "Unexpected error reading parent properties", e);
                renamedFolder = null;
                preResources = null;
            }
        } else {
            renamedFolder = null;
            preResources = null;
        }

        IProject project = r.getProject();
        PythonNature n = PythonNature.getPythonNature(project);
        if (n != null) {
            if (r instanceof IFile && !PythonPathHelper.isValidSourceFile((IFile) r)) {
                //If it is a file which does not end with .py, don't try to do a regular refactoring.
            } else {
                try {
                    String resolveModule = n.resolveModule(r);
                    if (resolveModule != null &&
                            // When it's an __init__, don't rename the package, only the file (regular rename operation
                            // -- the folder has to be selected to do a package rename
                            !resolveModule.endsWith(".__init__")) {
                        IFile file = null;
                        boolean foundAsInit = false;
                        if (r instanceof IContainer) {
                            file = PythonPathHelper.getFolderInit((IContainer) r);
                            foundAsInit = true;
                        } else if (r instanceof IFile) {
                            file = (IFile) r;
                        }

                        if (file != null && file.exists()) {
                            //It's a directory without an __init__.py file, just keep going...
                            RefactoringRequest request = new ModuleRenameRefactoringRequest(
                                    file.getLocation().toFile(), n, null);
                            if (!foundAsInit) {
                                // If we have found it as an __init__ when renaming a module, we won't
                                // set the related IFile (because we don't want to provide a 'simple rename'
                                // in this case -- as if he did actually select the __init__, only the simple
                                // rename would be provided in the first place).
                                request.setFileResource(file);
                            }
                            AbstractPyRefactoring.getPyRefactoring().rename(new PyRefactoringRequest(request));
                            //i.e.: if it was a module inside the pythonpath (as we resolved the name), don't go the default
                            //route and do a refactoring request to rename it)!
                            return;
                        }
                    }

                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }

        super.run();
        updatePyPath();
        renamedFolder = null;
        preResources = null;
    }
}
