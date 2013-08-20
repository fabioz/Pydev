/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions.copied;

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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.MoveFilesAndFoldersOperation;
import org.eclipse.ui.actions.ReadOnlyStateChecker;
import org.eclipse.ui.internal.navigator.resources.plugin.WorkbenchNavigatorMessages;
import org.eclipse.ui.internal.navigator.resources.plugin.WorkbenchNavigatorPlugin;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.resources.ResourceDropAdapterAssistant;
import org.eclipse.ui.part.ResourceTransfer;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.structure.OrderedMap;

/**
 * Copied becaus the original did not really adapt to resources (it tries to do if !xxx instanceof IResource in many places)
 * 
 * @author Fabio
 */
@SuppressWarnings("restriction")
public class PyResourceDropAdapterAssistant extends ResourceDropAdapterAssistant {

    /**
     * This is the main change (wherever it had a target, it tries to get the actual wrapped resource -- if it is wrapped)
     */
    private Object getActual(Object target) {
        if (target instanceof IWrappedResource) {
            IWrappedResource resource = (IWrappedResource) target;
            target = resource.getActualObject();
        }
        return target;
    }

    private static final boolean DEBUG = false;

    private static final IResource[] NO_RESOURCES = new IResource[0];

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#isSupportedType(org.eclipse.swt.dnd.TransferData)
     */
    public boolean isSupportedType(TransferData aTransferType) {
        return super.isSupportedType(aTransferType) || FileTransfer.getInstance().isSupportedType(aTransferType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#validateDrop(java.lang.Object,
     *      int, org.eclipse.swt.dnd.TransferData)
     */
    public IStatus validateDrop(Object target, int aDropOperation, TransferData transferType) {
        target = getActual(target);
        if (!(target instanceof IResource)) {
            return WorkbenchNavigatorPlugin.createStatus(IStatus.INFO, 0,
                    WorkbenchNavigatorMessages.DropAdapter_targetMustBeResource, null);
        }
        IResource resource = (IResource) target;
        if (!resource.isAccessible()) {
            return WorkbenchNavigatorPlugin.createErrorStatus(0,
                    WorkbenchNavigatorMessages.DropAdapter_canNotDropIntoClosedProject, null);
        }
        IContainer destination = getActualTarget(resource);
        if (destination.getType() == IResource.ROOT) {
            return WorkbenchNavigatorPlugin.createErrorStatus(0,
                    WorkbenchNavigatorMessages.DropAdapter_resourcesCanNotBeSiblings, null);
        }
        String message = null;
        // drag within Eclipse?
        if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
            IResource[] selectedResources = getSelectedResources();

            if (selectedResources.length == 0) {
                message = WorkbenchNavigatorMessages.DropAdapter_dropOperationErrorOther;
            } else {
                CopyFilesAndFoldersOperation operation;
                if (aDropOperation == DND.DROP_COPY) {
                    if (DEBUG) {
                        System.out.println("ResourceDropAdapterAssistant.validateDrop validating COPY."); //$NON-NLS-1$
                    }

                    operation = new CopyFilesAndFoldersOperation(getShell());
                } else {
                    if (DEBUG) {
                        System.out.println("ResourceDropAdapterAssistant.validateDrop validating MOVE."); //$NON-NLS-1$
                    }
                    operation = new MoveFilesAndFoldersOperation(getShell());
                }
                message = operation.validateDestination(destination, selectedResources);
            }
        } // file import?
        else if (FileTransfer.getInstance().isSupportedType(transferType)) {
            String[] sourceNames = (String[]) FileTransfer.getInstance().nativeToJava(transferType);
            if (sourceNames == null) {
                // source names will be null on Linux. Use empty names to do
                // destination validation.
                // Fixes bug 29778
                sourceNames = new String[0];
            }
            CopyFilesAndFoldersOperation copyOperation = new CopyFilesAndFoldersOperation(getShell());
            message = copyOperation.validateImportDestination(destination, sourceNames);
        }
        if (message != null) {
            return WorkbenchNavigatorPlugin.createErrorStatus(0, message, null);
        }
        return Status.OK_STATUS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#handleDrop(CommonDropAdapter,
     *      DropTargetEvent, Object)
     */
    public IStatus handleDrop(CommonDropAdapter aDropAdapter, DropTargetEvent aDropTargetEvent, Object aTarget) {
        //        aTarget = getActual(aTarget);
        if (DEBUG) {
            System.out.println("ResourceDropAdapterAssistant.handleDrop (begin)"); //$NON-NLS-1$
        }

        // alwaysOverwrite = false;
        if (getCurrentTarget(aDropAdapter) == null || aDropTargetEvent.data == null) {
            return Status.CANCEL_STATUS;
        }
        IStatus status = null;
        IResource[] resources = null;
        TransferData currentTransfer = aDropAdapter.getCurrentTransfer();
        if (LocalSelectionTransfer.getTransfer().isSupportedType(currentTransfer)) {
            resources = getSelectedResources();
        } else if (ResourceTransfer.getInstance().isSupportedType(currentTransfer)) {
            resources = (IResource[]) aDropTargetEvent.data;
        }

        if (FileTransfer.getInstance().isSupportedType(currentTransfer)) {
            status = performFileDrop(aDropAdapter, aDropTargetEvent.data);
        } else if (resources != null && resources.length > 0) {
            if (aDropAdapter.getCurrentOperation() == DND.DROP_COPY) {
                if (DEBUG) {
                    System.out.println("ResourceDropAdapterAssistant.handleDrop executing COPY."); //$NON-NLS-1$
                }
                status = performResourceCopy(aDropAdapter, getShell(), resources);
            } else {
                if (DEBUG) {
                    System.out.println("ResourceDropAdapterAssistant.handleDrop executing MOVE."); //$NON-NLS-1$
                }

                status = performResourceMove(aDropAdapter, resources);
            }
        }
        openError(status);
        IContainer target = getActualTarget((IResource) getCurrentTarget(aDropAdapter));
        if (target != null && target.isAccessible()) {
            try {
                target.refreshLocal(IResource.DEPTH_ONE, null);
            } catch (CoreException e) {
            }
        }
        return status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#validatePluginTransferDrop(org.eclipse.jface.viewers.IStructuredSelection,
     *      java.lang.Object)
     */
    public IStatus validatePluginTransferDrop(IStructuredSelection aDragSelection, Object aDropTarget) {
        aDropTarget = getActual(aDropTarget);
        if (!(aDropTarget instanceof IResource)) {
            return WorkbenchNavigatorPlugin.createStatus(IStatus.INFO, 0,
                    WorkbenchNavigatorMessages.DropAdapter_targetMustBeResource, null);
        }
        IResource resource = (IResource) aDropTarget;
        if (!resource.isAccessible()) {
            return WorkbenchNavigatorPlugin.createErrorStatus(0,
                    WorkbenchNavigatorMessages.DropAdapter_canNotDropIntoClosedProject, null);
        }
        IContainer destination = getActualTarget(resource);
        if (destination.getType() == IResource.ROOT) {
            return WorkbenchNavigatorPlugin.createErrorStatus(0,
                    WorkbenchNavigatorMessages.DropAdapter_resourcesCanNotBeSiblings, null);
        }

        IResource[] selectedResources = getSelectedResources(aDragSelection);

        String message = null;
        if (selectedResources.length == 0) {
            message = WorkbenchNavigatorMessages.DropAdapter_dropOperationErrorOther;
        } else {
            MoveFilesAndFoldersOperation operation;

            operation = new MoveFilesAndFoldersOperation(getShell());
            message = operation.validateDestination(destination, selectedResources);
        }
        if (message != null) {
            return WorkbenchNavigatorPlugin.createErrorStatus(0, message, null);
        }
        return Status.OK_STATUS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#handlePluginTransferDrop(org.eclipse.jface.viewers.IStructuredSelection,
     *      java.lang.Object)
     */
    public IStatus handlePluginTransferDrop(IStructuredSelection aDragSelection, Object aDropTarget) {
        aDropTarget = getActual(aDropTarget);

        IContainer target = getActualTarget((IResource) aDropTarget);
        IResource[] resources = getSelectedResources(aDragSelection);

        MoveFilesAndFoldersOperation operation = new MoveFilesAndFoldersOperation(getShell());
        operation.copyResources(resources, target);

        if (target != null && target.isAccessible()) {
            try {
                target.refreshLocal(IResource.DEPTH_ONE, null);
            } catch (CoreException e) {
            }
        }
        return Status.OK_STATUS;
    }

    /**
     * Returns the actual target of the drop, given the resource under the
     * mouse. If the mouse target is a file, then the drop actually occurs in
     * its parent. If the drop location is before or after the mouse target and
     * feedback is enabled, the target is also the parent.
     */
    private IContainer getActualTarget(IResource mouseTarget) {

        /* if cursor is on a file, return the parent */
        if (mouseTarget.getType() == IResource.FILE) {
            return mouseTarget.getParent();
        }
        /* otherwise the mouseTarget is the real target */
        return (IContainer) mouseTarget;
    }

    /**
     * Returns the resource selection from the LocalSelectionTransfer.
     * 
     * @return the resource selection from the LocalSelectionTransfer
     */
    private IResource[] getSelectedResources() {

        ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
        if (selection instanceof IStructuredSelection) {
            return getSelectedResources((IStructuredSelection) selection);
        }
        return NO_RESOURCES;
    }

    /**
     * Returns the resource selection from the LocalSelectionTransfer.
     * 
     * @return the resource selection from the LocalSelectionTransfer
     */
    @SuppressWarnings("unchecked")
    private IResource[] getSelectedResources(IStructuredSelection selection) {
        ArrayList selectedResources = new ArrayList();

        for (Iterator i = selection.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof IResource) {
                selectedResources.add(o);
            } else if (o instanceof IAdaptable) {
                IAdaptable a = (IAdaptable) o;
                IResource r = (IResource) a.getAdapter(IResource.class);
                if (r != null) {
                    selectedResources.add(r);
                }
            }
        }
        return (IResource[]) selectedResources.toArray(new IResource[selectedResources.size()]);
    }

    /**
     * Update the PYTHONPATH of projects that have had source folders pasted into them by
     * adding those folders' paths to it.
     */
    private void updatePyPath(IResource[] copiedResources, IContainer destination, boolean moved) {
        try {
            // Get the PYTHONPATH of the destination project. It may be modified to include the pasted resources.
            IProject destProject = destination.getProject();
            IPythonPathNature destPythonPathNature = PythonNature.getPythonPathNature(destProject);
            // If pasting in a non-Python project, we can quit now.
            if (destPythonPathNature == null && !moved) {
                return;
            }
            SortedSet<String> destActualPathSet = new TreeSet<String>(
                    destPythonPathNature.getProjectSourcePathSet(false)); //non-resolved
            int numOldPaths = destActualPathSet.size();

            // Now find which of the copied resources are source folders, whose paths are in their projects' PYTHONPATH.
            // NOTE: presently, copied resources must come from the same parent/project. The multiple project checking
            // used here is kept in case a potential new feature changes that restriction.
            Map<IProject, OrderedMap<String, String>> projectSourcePathMaps = new HashMap<IProject, OrderedMap<String, String>>();
            Map<IProject, List<IFolder>> remFoldersOfProjMap = !moved ? null : new HashMap<IProject, List<IFolder>>();
            boolean innerMove = false;
            for (IResource resource : copiedResources) {
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
                if (moved) {
                    List<IFolder> remFoldersOfProj = remFoldersOfProjMap.get(project);
                    if (remFoldersOfProj == null) {
                        remFoldersOfProj = new LinkedList<IFolder>();
                        remFoldersOfProjMap.put(project, remFoldersOfProj);
                    }
                    remFoldersOfProj.add((IFolder) resource);
                }
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
                                if (moved && destProject.equals(project)) {
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

            // If resources were moved, update their project's/s' PYTHONPATHs.
            if (moved) {
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
            }

        } catch (Exception e) {
            Log.log(IStatus.ERROR, "Unexpected error setting project properties", e);
        }
    }

    /**
     * Performs a resource copy
     */
    private IStatus performResourceCopy(CommonDropAdapter dropAdapter, Shell shell, IResource[] sources) {
        MultiStatus problems = new MultiStatus(PlatformUI.PLUGIN_ID, 1,
                WorkbenchNavigatorMessages.DropAdapter_problemsMoving, null);
        mergeStatus(
                problems,
                validateTarget(getCurrentTarget(dropAdapter), dropAdapter.getCurrentTransfer(),
                        dropAdapter.getCurrentOperation()));

        IContainer target = getActualTarget((IResource) getCurrentTarget(dropAdapter));
        CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(shell);
        IResource[] copiedResources = operation.copyResources(sources, target);
        if (copiedResources.length > 0) {
            updatePyPath(copiedResources, target, false);
        }

        return problems;
    }

    /**
     * Performs a resource move
     */
    private IStatus performResourceMove(CommonDropAdapter dropAdapter, IResource[] sources) {
        MultiStatus problems = new MultiStatus(PlatformUI.PLUGIN_ID, 1,
                WorkbenchNavigatorMessages.DropAdapter_problemsMoving, null);
        mergeStatus(
                problems,
                validateTarget(getCurrentTarget(dropAdapter), dropAdapter.getCurrentTransfer(),
                        dropAdapter.getCurrentOperation()));

        IContainer target = getActualTarget((IResource) getCurrentTarget(dropAdapter));
        ReadOnlyStateChecker checker = new ReadOnlyStateChecker(getShell(),
                WorkbenchNavigatorMessages.MoveResourceAction_title,
                WorkbenchNavigatorMessages.MoveResourceAction_checkMoveMessage);
        sources = checker.checkReadOnlyResources(sources);
        MoveFilesAndFoldersOperation operation = new MoveFilesAndFoldersOperation(getShell());
        IResource[] copiedResources = operation.copyResources(sources, target);
        if (copiedResources.length > 0) {
            updatePyPath(copiedResources, target, true);
        }

        return problems;
    }

    private Object getCurrentTarget(CommonDropAdapter dropAdapter) {
        return getActual(dropAdapter.getCurrentTarget());
    }

    /**
     * Performs a drop using the FileTransfer transfer type.
     */
    private IStatus performFileDrop(CommonDropAdapter anAdapter, Object data) {
        data = getActual(data);
        MultiStatus problems = new MultiStatus(PlatformUI.PLUGIN_ID, 0,
                WorkbenchNavigatorMessages.DropAdapter_problemImporting, null);
        mergeStatus(
                problems,
                validateTarget(getCurrentTarget(anAdapter), anAdapter.getCurrentTransfer(),
                        anAdapter.getCurrentOperation()));

        final IContainer target = getActualTarget((IResource) getCurrentTarget(anAdapter));
        final String[] names = (String[]) data;
        // Run the import operation asynchronously.
        // Otherwise the drag source (e.g., Windows Explorer) will be blocked
        // while the operation executes. Fixes bug 16478.
        Display.getCurrent().asyncExec(new Runnable() {
            public void run() {
                getShell().forceActive();
                CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(getShell());
                operation.copyFiles(names, target);
            }
        });
        return problems;
    }

    /**
     * Ensures that the drop target meets certain criteria
     */
    private IStatus validateTarget(Object target, TransferData transferType, int dropOperation) {
        target = getActual(target);
        if (!(target instanceof IResource)) {
            return WorkbenchNavigatorPlugin
                    .createInfoStatus(WorkbenchNavigatorMessages.DropAdapter_targetMustBeResource);
        }
        IResource resource = (IResource) target;
        if (!resource.isAccessible()) {
            return WorkbenchNavigatorPlugin
                    .createErrorStatus(WorkbenchNavigatorMessages.DropAdapter_canNotDropIntoClosedProject);
        }
        IContainer destination = getActualTarget(resource);
        if (destination.getType() == IResource.ROOT) {
            return WorkbenchNavigatorPlugin
                    .createErrorStatus(WorkbenchNavigatorMessages.DropAdapter_resourcesCanNotBeSiblings);
        }
        String message = null;
        // drag within Eclipse?
        if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
            IResource[] selectedResources = getSelectedResources();

            if (selectedResources.length == 0) {
                message = WorkbenchNavigatorMessages.DropAdapter_dropOperationErrorOther;
            } else {
                CopyFilesAndFoldersOperation operation;
                if (dropOperation == DND.DROP_COPY) {
                    operation = new CopyFilesAndFoldersOperation(getShell());
                } else {
                    operation = new MoveFilesAndFoldersOperation(getShell());
                }
                message = operation.validateDestination(destination, selectedResources);
            }
        } // file import?
        else if (FileTransfer.getInstance().isSupportedType(transferType)) {
            String[] sourceNames = (String[]) FileTransfer.getInstance().nativeToJava(transferType);
            if (sourceNames == null) {
                // source names will be null on Linux. Use empty names to do
                // destination validation.
                // Fixes bug 29778
                sourceNames = new String[0];
            }
            CopyFilesAndFoldersOperation copyOperation = new CopyFilesAndFoldersOperation(getShell());
            message = copyOperation.validateImportDestination(destination, sourceNames);
        }
        if (message != null) {
            return WorkbenchNavigatorPlugin.createErrorStatus(message);
        }
        return Status.OK_STATUS;
    }

    /**
     * Adds the given status to the list of problems. Discards OK statuses. If
     * the status is a multi-status, only its children are added.
     */
    private void mergeStatus(MultiStatus status, IStatus toMerge) {
        if (!toMerge.isOK()) {
            status.merge(toMerge);
        }
    }

    /**
     * Opens an error dialog if necessary. Takes care of complex rules necessary
     * for making the error dialog look nice.
     */
    private void openError(IStatus status) {
        if (status == null) {
            return;
        }

        String genericTitle = WorkbenchNavigatorMessages.DropAdapter_title;
        int codes = IStatus.ERROR | IStatus.WARNING;

        // simple case: one error, not a multistatus
        if (!status.isMultiStatus()) {
            ErrorDialog.openError(getShell(), genericTitle, null, status, codes);
            return;
        }

        // one error, single child of multistatus
        IStatus[] children = status.getChildren();
        if (children.length == 1) {
            ErrorDialog.openError(getShell(), status.getMessage(), null, children[0], codes);
            return;
        }
        // several problems
        ErrorDialog.openError(getShell(), genericTitle, null, status, codes);
    }

}
