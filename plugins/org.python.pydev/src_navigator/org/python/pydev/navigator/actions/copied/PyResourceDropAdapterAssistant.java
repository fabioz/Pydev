/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions.copied;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
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
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.ModuleRenameRefactoringRequest;
import org.python.pydev.editor.refactoring.MultiModuleMoveRefactoringRequest;
import org.python.pydev.editor.refactoring.TargetNotInPythonpathException;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

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
    @Override
    public boolean isSupportedType(TransferData aTransferType) {
        return super.isSupportedType(aTransferType) || FileTransfer.getInstance().isSupportedType(aTransferType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#validateDrop(java.lang.Object,
     *      int, org.eclipse.swt.dnd.TransferData)
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
            PythonPathHelper.updatePyPath(copiedResources, target, PythonPathHelper.OPERATION_COPY);
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

        boolean targetInSourceFolder = false;
        PythonNature nature;
        try {
            nature = PythonNature.getPythonNature(target);
            Set<String> projectSourcePathSet = nature.getPythonPathNature().getProjectSourcePathSet(true);
            for (String string : projectSourcePathSet) {
                if (new Path(string).isPrefixOf(target.getFullPath())) {
                    targetInSourceFolder = true;
                    break;
                }
            }
        } catch (CoreException e1) {
            Log.log(e1);
        }

        if (targetInSourceFolder) {
            try {
                int resolved = 0;
                List<ModuleRenameRefactoringRequest> requests = new ArrayList<>();
                for (IResource s : sources) {
                    if (!PythonPathHelper.isValidSourceFile(s.getName())) {
                        //For now this is a limitation: compiled modules cannot be moved updating references :(
                        continue;
                    }
                    nature = PythonNature.getPythonNature(s);
                    try {
                        String resolveModule = nature.resolveModule(s);
                        if (resolveModule != null) {
                            File file = s.getLocation().toFile();
                            boolean isDir = file.isDirectory();
                            File initFile = null;
                            if (isDir) {
                                initFile = PythonPathHelper.getFolderInit(file);
                            }
                            if (isDir && initFile == null) {
                                //It's a directory without an __init__.py inside the pythonpath: can't move along with the others...
                                break;
                            } else {
                                if (isDir) {
                                    //If it's a directory, use the __init__.py instead.
                                    file = initFile;
                                }
                            }

                            resolved += 1;
                            requests.add(new ModuleRenameRefactoringRequest(file, nature, target));
                        }
                    } catch (MisconfigurationException e) {
                        Log.log(e);
                    }
                }
                if (resolved != 0) {
                    if (resolved != sources.length) {
                        problems.add(PydevPlugin
                                .makeStatus(
                                        IStatus.ERROR,
                                        "Unable to do refactor action because some of the resources moved are in the PYTHONPATH and some are not.",
                                        null));
                        return problems;
                    } else {
                        //Make a refactoring operation
                        AbstractPyRefactoring.getPyRefactoring().rename(
                                new MultiModuleMoveRefactoringRequest(requests, target));

                        return problems;
                    }
                }
            } catch (TargetNotInPythonpathException e) {
                //Keep on going through the regular path.

            } catch (Exception e) {
                Log.log(e);
                //Ok, log it but do the regular operation.
            }
        }

        MoveFilesAndFoldersOperation operation = new MoveFilesAndFoldersOperation(getShell());
        IResource[] copiedResources = operation.copyResources(sources, target);
        if (copiedResources.length > 0) {
            PythonPathHelper.updatePyPath(copiedResources, target, PythonPathHelper.OPERATION_MOVE);
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
