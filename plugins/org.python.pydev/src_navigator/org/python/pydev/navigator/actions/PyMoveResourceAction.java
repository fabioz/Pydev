/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.MoveResourceAction;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.structure.OrderedMap;

public class PyMoveResourceAction extends MoveResourceAction {

    private ISelectionProvider provider;

    private ArrayList<IResource> selected;

    public PyMoveResourceAction(Shell shell, ISelectionProvider selectionProvider) {
        super(shell);
        this.provider = selectionProvider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    public boolean isEnabled() {
        fillSelection();
        return selected != null && selected.size() > 0;
    }

    private boolean fillSelection() {
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
                        IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                        if (resource != null) {
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
     * Update the PYTHONPATH of the project that originally contained the moved resource,
     * and that of the project it was moved to.
     */
    private void updatePyPath(IContainer destination) {
        try {
            // Get the PYTHONPATH of the destination project. It may be modified to include the pasted resources.
            IProject destProject = destination.getProject();
            IPythonPathNature destPythonPathNature = PythonNature.getPythonPathNature(destProject);
            SortedSet<String> destActualPathSet = new TreeSet<String>(
                    destPythonPathNature.getProjectSourcePathSet(false)); //non-resolved
            int numOldPaths = destActualPathSet.size();

            // Now find which of the copied resources are source folders, whose paths are in their projects' PYTHONPATH.
            // NOTE: presently, copied resources must come from the same parent/project. The multiple project checking
            // used here is kept in case a potential new feature changes that restriction.
            Map<IProject, OrderedMap<String, String>> projectSourcePathMaps = new HashMap<IProject, OrderedMap<String, String>>();
            Map<IProject, List<IFolder>> remFoldersOfProjMap = new HashMap<IProject, List<IFolder>>();
            boolean innerMove = false;
            for (IResource resource : selected) {
                if (!(resource instanceof IFolder)) {
                    continue;
                }
                IProject project = resource.getProject();
                OrderedMap<String, String> sourceMap = projectSourcePathMaps.get(project);
                if (sourceMap == null) {
                    IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
                    // Ignore resources that come from a non-Python project.
                    if (pythonPathNature == null) {
                        continue;
                    }
                    sourceMap = pythonPathNature
                            .getProjectSourcePathResolvedToUnresolvedMap();
                    projectSourcePathMaps.put(project, sourceMap);
                }
                List<IFolder> remFoldersOfProj = remFoldersOfProjMap.get(project);
                if (remFoldersOfProj == null) {
                    remFoldersOfProj = new LinkedList<IFolder>();
                    remFoldersOfProjMap.put(project, remFoldersOfProj);
                }
                remFoldersOfProj.add((IFolder) resource);
                IPath resourcePath = resource.getFullPath();

                // If the resource or its children are in its original project's PYTHONPATH, add to the destination project's PYTHONPATH.
                // By default, make the path project relative.
                if (destPythonPathNature != null) {
                    for (String pathName : sourceMap.keySet()) {
                        IPath sourcePath = Path.fromOSString(pathName);
                        if (resourcePath.isPrefixOf(sourcePath)) {
                            String destActualPath = PyStructureConfigHelpers.convertToProjectRelativePath(
                                    destProject.getFullPath().toOSString(),
                                    destination.getFullPath().append(
                                            sourcePath.removeFirstSegments(resourcePath.segmentCount() - 1)).toOSString());
                            if (destActualPathSet.add(destActualPath)) {
                                // Do this in case a resource was moved within its own project.
                                if (destProject.equals(project)) {
                                    sourceMap.put(resourcePath.toOSString(), destActualPath);
                                    innerMove = true;
                                }
                            }
                        }
                    }
                }
            }

            // If the destination project's PYTHONPATH was updated, rebuild it.
            // NOTE: skip this if a resource was moved in its own project, in which case the PYTHONPATH will be rebuilt later.
            if (destActualPathSet.size() != numOldPaths && !innerMove) {
                destPythonPathNature.setProjectSourcePath(StringUtils.join("|", destActualPathSet));
                PythonNature.getPythonNature(destProject).rebuildPath();
            }

            // Update the PYTHONPATHs of the project(s) resources were moved out of.
            for (IProject project : remFoldersOfProjMap.keySet()) {
                boolean removedSomething = false;
                OrderedMap<String, String> projectSourcePathMap = projectSourcePathMaps.get(project);
                List<IPath> sourcePaths = new LinkedList<IPath>();
                for (String pathName : projectSourcePathMap.keySet()) {
                    sourcePaths.add(Path.fromOSString(pathName));
                }
                // Check if deleted folders are/contain source folders.
                for (IFolder remFolder : remFoldersOfProjMap.get(project)) {
                    IPath remPath = remFolder.getFullPath();
                    for (int i = 0; i < sourcePaths.size(); i++) {
                        if (remPath.isPrefixOf(sourcePaths.get(i))) {
                            projectSourcePathMap.remove(sourcePaths.remove(i--).toOSString());
                            removedSomething = true;
                        }
                    }
                }
                // Now update each project's PYTHONPATH, if source folders have been removed.
                if (removedSomething) {
                    PythonNature.getPythonPathNature(project).setProjectSourcePath(
                            StringUtils.join("|", projectSourcePathMap.values()));
                    PythonNature.getPythonNature(project).rebuildPath();
                }
            }

        } catch (Exception e) {
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

    private IPath pyQueryDestinationResource() {
        // start traversal at root resource, should probably start at a
        // better location in the tree
        String title;
        if (selected.size() == 1) {
            title = "Choose destination for ''" + selected.get(0).getName() + "'':";
        } else {
            title = "Choose destination for " + new Integer(selected.size()) + " selected resources:";
        }
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(shellProvider.getShell(),
                selected.get(0).getParent(), true, title);
        dialog.setTitle("Move Resources");
        dialog.setValidator(this);
        dialog.showClosedProjects(false);
        dialog.open();
        Object[] result = dialog.getResult();
        if (result != null && result.length == 1) {
            return (IPath) result[0];
        }
        return null;
    }

    /*
     * (non-Javadoc) Method declared on IAction.
     */
    public void run() {
        if (!fillSelection()) { //will also update the list of resources (main change from the DeleteResourceAction)
            return;
        }
        Helpers.checkValidateState();
        try {
            operation = createOperation();
            operation.setModelProviderIds(getModelProviderIds());
            IPath destination = pyQueryDestinationResource();
            if (destination == null) {
                return;
            }
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IContainer container = (IContainer) root.findMember(destination);
            if (container == null) {
                return;
            }
            runOperation(getResources(selected), container);
        } finally {
            operation = null;
        }
    }

    @Override
    protected void runOperation(IResource[] resources, IContainer destination) {
        super.runOperation(resources, destination);
        if (destinations.size() > 0) {
            updatePyPath(destination);
        }
    }

}
