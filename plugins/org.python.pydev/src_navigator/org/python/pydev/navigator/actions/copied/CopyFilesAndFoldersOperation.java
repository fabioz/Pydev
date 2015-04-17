/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions.copied;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.resources.mapping.ResourceChangeValidator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.StatusUtil;
import org.eclipse.ui.internal.ide.dialogs.IDEResourceInfoUtils;
import org.eclipse.ui.wizards.datatransfer.FileStoreStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 * Perform the copy of file and folder resources from the clipboard when paste
 * action is invoked.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
@SuppressWarnings("restriction")
public class CopyFilesAndFoldersOperation {

    /**
     * Status containing the errors detected when running the operation or
     * <code>null</code> if no errors detected.
     */
    private MultiStatus errorStatus;

    /**
     * The parent shell used to show any dialogs.
     */
    private Shell messageShell;

    /**
     * Whether or not the copy has been canceled by the user.
     */
    private boolean canceled = false;

    /**
     * Overwrite all flag.
     */
    private boolean alwaysOverwrite = false;

    private String[] modelProviderIds;

    /**
     * Returns a new name for a copy of the resource at the given path in the
     * given workspace. This name is determined automatically.
     *
     * @param originalName
     *            the full path of the resource
     * @param workspace
     *            the workspace
     * @return the new full path for the copy
     */
    static IPath getAutoNewNameFor(IPath originalName, IWorkspace workspace) {
        int counter = 1;
        String resourceName = originalName.lastSegment();
        IPath leadupSegment = originalName.removeLastSegments(1);

        while (true) {
            String nameSegment;

            if (counter > 1) {
                nameSegment = NLS.bind("Copy ({0}) of {1}", new Integer(
                        counter), resourceName);
            } else {
                nameSegment = NLS.bind("Copy of {0}", resourceName);
            }

            IPath pathToTry = leadupSegment.append(nameSegment);

            if (!workspace.getRoot().exists(pathToTry)) {
                return pathToTry;
            }

            counter++;
        }
    }

    /**
     * Creates a new operation initialized with a shell.
     *
     * @param shell
     *            parent shell for error dialogs
     */
    public CopyFilesAndFoldersOperation(Shell shell) {
        messageShell = shell;
    }

    /**
     * Returns whether this operation is able to perform on-the-fly
     * auto-renaming of resources with name collisions.
     *
     * @return <code>true</code> if auto-rename is supported, and
     *         <code>false</code> otherwise
     */
    protected boolean canPerformAutoRename() {
        return true;
    }

    /**
     * Returns the message for querying deep copy/move of a linked resource.
     *
     * @param source
     *            resource the query is made for
     * @return the deep query message
     */
    protected String getDeepCheckQuestion(IResource source) {
        return NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_deepCopyQuestion, source.getFullPath()
                .makeRelative());
    }

    /**
     * Checks whether the infos exist.
     *
     * @param stores
     *            the file infos to test
     * @return Multi status with one error message for each missing file.
     */
    IStatus checkExist(IFileStore[] stores) {
        MultiStatus multiStatus = new MultiStatus(PlatformUI.PLUGIN_ID, IStatus.OK, getProblemsMessage(), null);

        for (int i = 0; i < stores.length; i++) {
            if (stores[i].fetchInfo().exists() == false) {
                String message = NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_resourceDeleted,
                        stores[i].getName());
                IStatus status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, message, null);
                multiStatus.add(status);
            }
        }
        return multiStatus;
    }

    /**
     * Checks whether the resources with the given names exist.
     *
     * @param resources
     *            IResources to checl
     * @return Multi status with one error message for each missing file.
     */
    IStatus checkExist(IResource[] resources) {
        MultiStatus multiStatus = new MultiStatus(PlatformUI.PLUGIN_ID, IStatus.OK, getProblemsMessage(), null);

        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            if (resource != null) {
                URI location = resource.getLocationURI();
                String message = null;
                if (location != null) {
                    IFileInfo info = IDEResourceInfoUtils.getFileInfo(location);
                    if (info == null || info.exists() == false) {
                        if (resource.isLinked()) {
                            message = NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_missingLinkTarget,
                                    resource.getName());
                        } else {
                            message = NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_resourceDeleted,
                                    resource.getName());
                        }
                    }
                }
                if (message != null) {
                    IStatus status = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, message, null);
                    multiStatus.add(status);
                }
            }
        }
        return multiStatus;
    }

    /**
     * Check if the user wishes to overwrite the supplied resource or all
     * resources.
     *
     * @param source
     *            the source resource
     * @param destination
     *            the resource to be overwritten
     * @return one of IDialogConstants.YES_ID, IDialogConstants.YES_TO_ALL_ID,
     *         IDialogConstants.NO_ID, IDialogConstants.CANCEL_ID indicating
     *         whether the current resource or all resources can be overwritten,
     *         or if the operation should be canceled.
     */
    private int checkOverwrite(final IResource source, final IResource destination) {
        final int[] result = new int[1];

        // Dialogs need to be created and opened in the UI thread
        Runnable query = new Runnable() {
            public void run() {
                String message;
                int resultId[] = { IDialogConstants.YES_ID, IDialogConstants.YES_TO_ALL_ID, IDialogConstants.NO_ID,
                        IDialogConstants.CANCEL_ID };
                String labels[] = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL,
                        IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };

                if (destination.getType() == IResource.FOLDER) {
                    if (homogenousResources(source, destination)) {
                        message = NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_overwriteMergeQuestion,
                                destination.getFullPath().makeRelative());
                    } else {
                        if (destination.isLinked()) {
                            message = NLS.bind(
                                    IDEWorkbenchMessages.CopyFilesAndFoldersOperation_overwriteNoMergeLinkQuestion,
                                    destination.getFullPath().makeRelative());
                        } else {
                            message = NLS.bind(
                                    IDEWorkbenchMessages.CopyFilesAndFoldersOperation_overwriteNoMergeNoLinkQuestion,
                                    destination.getFullPath().makeRelative());
                        }
                        resultId = new int[] { IDialogConstants.YES_ID, IDialogConstants.NO_ID,
                                IDialogConstants.CANCEL_ID };
                        labels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
                                IDialogConstants.CANCEL_LABEL };
                    }
                } else {
                    String[] bindings = new String[] { IDEResourceInfoUtils.getLocationText(destination),
                            IDEResourceInfoUtils.getDateStringValue(destination),
                            IDEResourceInfoUtils.getLocationText(source),
                            IDEResourceInfoUtils.getDateStringValue(source) };
                    message = NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_overwriteWithDetailsQuestion,
                            bindings);
                }
                MessageDialog dialog = new MessageDialog(messageShell,
                        IDEWorkbenchMessages.CopyFilesAndFoldersOperation_resourceExists, null, message,
                        MessageDialog.QUESTION, labels, 0);
                dialog.open();
                if (dialog.getReturnCode() == SWT.DEFAULT) {
                    // A window close returns SWT.DEFAULT, which has to be
                    // mapped to a cancel
                    result[0] = IDialogConstants.CANCEL_ID;
                } else {
                    result[0] = resultId[dialog.getReturnCode()];
                }
            }
        };
        messageShell.getDisplay().syncExec(query);
        return result[0];
    }

    /**
     * Recursively collects existing files in the specified destination path.
     *
     * @param destinationPath
     *            destination path to check for existing files
     * @param copyResources
     *            resources that may exist in the destination
     * @param existing
     *            holds the collected existing files
     */
    private void collectExistingReadonlyFiles(IPath destinationPath, IResource[] copyResources,
            ArrayList<IFile> existing) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        for (int i = 0; i < copyResources.length; i++) {
            IResource source = copyResources[i];
            IPath newDestinationPath = destinationPath.append(source.getName());
            IResource newDestination = workspaceRoot.findMember(newDestinationPath);
            IFolder folder;

            if (newDestination == null) {
                continue;
            }
            folder = getFolder(newDestination);
            if (folder != null) {
                IFolder sourceFolder = getFolder(source);

                if (sourceFolder != null) {
                    try {
                        collectExistingReadonlyFiles(newDestinationPath, sourceFolder.members(), existing);
                    } catch (CoreException exception) {
                        recordError(exception);
                    }
                }
            } else {
                IFile file = getFile(newDestination);

                if (file != null) {
                    if (file.isReadOnly()) {
                        existing.add(file);
                    }
                    if (getValidateConflictSource()) {
                        IFile sourceFile = getFile(source);
                        if (sourceFile != null) {
                            existing.add(sourceFile);
                        }
                    }
                }
            }
        }
    }

    /**
     * Copies the resources to the given destination. This method is called
     * recursively to merge folders during folder copy.
     *
     * @param resources
     *            the resources to copy
     * @param destination
     *            destination to which resources will be copied
     * @param subMonitor
     *            a progress monitor for showing progress and for cancelation
     */
    protected void copy(IResource[] resources, IPath destination, IProgressMonitor subMonitor) throws CoreException {

        subMonitor.beginTask(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_CopyResourcesTask, resources.length);

        for (int i = 0; i < resources.length; i++) {
            IResource source = resources[i];
            IPath destinationPath = destination.append(source.getName());
            IWorkspace workspace = source.getWorkspace();
            IWorkspaceRoot workspaceRoot = workspace.getRoot();
            IResource existing = workspaceRoot.findMember(destinationPath);
            if (source.getType() == IResource.FOLDER && existing != null) {
                // the resource is a folder and it exists in the destination,
                // copy the
                // children of the folder.
                if (homogenousResources(source, existing)) {
                    IResource[] children = ((IContainer) source).members();
                    copy(children, destinationPath, new SubProgressMonitor(subMonitor, 1));
                } else {
                    // delete the destination folder, copying a linked folder
                    // over an unlinked one or vice versa. Fixes bug 28772.
                    delete(existing, new SubProgressMonitor(subMonitor, 0));
                    source.copy(destinationPath, IResource.SHALLOW, new SubProgressMonitor(subMonitor, 1));
                }
            } else {
                if (existing != null) {
                    if (homogenousResources(source, existing)) {
                        copyExisting(source, existing, new SubProgressMonitor(subMonitor, 1));
                    } else {
                        // Copying a linked resource over unlinked or vice
                        // versa.
                        // Can't use setContents here. Fixes bug 28772.
                        delete(existing, new SubProgressMonitor(subMonitor, 0));
                        source.copy(destinationPath, IResource.SHALLOW, new SubProgressMonitor(subMonitor, 1));
                    }
                } else {
                    source.copy(destinationPath, IResource.SHALLOW, new SubProgressMonitor(subMonitor, 1));

                }

                if (subMonitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
            }
        }
    }

    /**
     * Sets the content of the existing file to the source file content.
     *
     * @param source
     *            source file to copy
     * @param existing
     *            existing file to set the source content in
     * @param subMonitor
     *            a progress monitor for showing progress and for cancelation
     * @throws CoreException
     *             setContents failed
     */
    private void copyExisting(IResource source, IResource existing, IProgressMonitor subMonitor) throws CoreException {
        IFile existingFile = getFile(existing);

        if (existingFile != null) {
            IFile sourceFile = getFile(source);

            if (sourceFile != null) {
                existingFile.setContents(sourceFile.getContents(), IResource.KEEP_HISTORY, new SubProgressMonitor(
                        subMonitor, 0));
            }
        }
    }

    /**
     * Copies the given resources to the destination. The current Thread is
     * halted while the resources are copied using a WorkspaceModifyOperation.
     * This method should be called from the UIThread.
     *
     * @param resources
     *            the resources to copy
     * @param destination
     *            destination to which resources will be copied
     * @return IResource[] the resulting {@link IResource}[]
     * @see WorkspaceModifyOperation
     * @see Display#getThread()
     * @see Thread#currentThread()
     */
    public IResource[] copyResources(final IResource[] resources, IContainer destination) {
        return copyResources(resources, destination, true, null);
    }

    /**
     * Copies the given resources to the destination in the current Thread
     * without forking a new Thread or blocking using a
     * WorkspaceModifyOperation. It recommended that this method only be called
     * from a {@link WorkspaceJob} to avoid possible deadlock.
     *
     * @param resources
     *            the resources to copy
     * @param destination
     *            destination to which resources will be copied
     * @param monitor
     *            the monitor that information will be sent to.
     * @return IResource[] the resulting {@link IResource}[]
     * @see WorkspaceModifyOperation
     * @see WorkspaceJob
     * @since 3.2
     */
    public IResource[] copyResourcesInCurrentThread(final IResource[] resources, IContainer destination,
            IProgressMonitor monitor) {
        return copyResources(resources, destination, false, monitor);
    }

    /**
     * Copies the given resources to the destination.
     *
     * @param resources
     *            the resources to copy
     * @param destination
     *            destination to which resources will be copied
     * @return IResource[] the resulting {@link IResource}[]
     */
    private IResource[] copyResources(final IResource[] resources, IContainer destination, boolean fork,
            IProgressMonitor monitor) {
        final IPath destinationPath = destination.getFullPath();
        final IResource[][] copiedResources = new IResource[1][0];

        // test resources for existence separate from validate API.
        // Validate is performance critical and resource exists
        // check is potentially slow. Fixes bugs 16129/28602.
        IStatus resourceStatus = checkExist(resources);
        if (resourceStatus.getSeverity() != IStatus.OK) {
            displayError(resourceStatus);
            return copiedResources[0];
        }
        String errorMsg = validateDestination(destination, resources);
        if (errorMsg != null) {
            displayError(errorMsg);
            return copiedResources[0];
        }

        if (!validateOperation(resources, destinationPath)) {
            return copiedResources[0];
        }

        if (fork) {
            WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
                @Override
                public void execute(IProgressMonitor monitor) {
                    copyResources(resources, destinationPath, copiedResources, monitor);
                }
            };

            try {
                PlatformUI.getWorkbench().getProgressService().run(true, true, op);
            } catch (InterruptedException e) {
                return copiedResources[0];
            } catch (InvocationTargetException e) {
                display(e);
            }
        } else {
            copyResources(resources, destinationPath, copiedResources, monitor);
        }

        // If errors occurred, open an Error dialog
        if (errorStatus != null) {
            displayError(errorStatus);
            errorStatus = null;
        }
        return copiedResources[0];
    }

    /**
     * Validates the copy or move operation.
     *
     * @param resources
     *            the resources being copied or moved
     * @param destinationPath
     *            the destination of the copy or move
     * @return whether the operation should proceed
     * @since 3.2
     */
    private boolean validateOperation(IResource[] resources, IPath destinationPath) {
        IResourceChangeDescriptionFactory factory = ResourceChangeValidator.getValidator().createDeltaFactory();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            if (isMove()) {
                factory.move(resource, destinationPath.append(resource.getName()));
            } else {
                factory.copy(resource, destinationPath.append(resource.getName()));
            }
        }
        String title;
        String message;
        if (isMove()) {
            title = IDEWorkbenchMessages.CopyFilesAndFoldersOperation_confirmMove;
            message = IDEWorkbenchMessages.CopyFilesAndFoldersOperation_warningMove;
        } else {
            title = IDEWorkbenchMessages.CopyFilesAndFoldersOperation_confirmCopy;
            message = IDEWorkbenchMessages.CopyFilesAndFoldersOperation_warningCopy;
        }
        return IDE
                .promptToConfirm(messageShell, title, message, factory.getDelta(), modelProviderIds,
                        true /* syncExec */);
    }

    /**
     * Return whether the operation is a move or a copy
     *
     * @return whether the operation is a move or a copy
     * @since 3.2
     */
    protected boolean isMove() {
        return false;
    }

    private void display(InvocationTargetException e) {
        // CoreExceptions are collected above, but unexpected runtime
        // exceptions and errors may still occur.
        IDEWorkbenchPlugin.getDefault().getLog()
                .log(StatusUtil.newStatus(IStatus.ERROR, MessageFormat.format("Exception in {0}.performCopy(): {1}", //$NON-NLS-1$
                        new Object[] { getClass().getName(), e.getTargetException() }), null));
        displayError(NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_internalError, e.getTargetException()
                .getMessage()));
    }

    /**
     * Copies the given URIS and folders to the destination. The current Thread
     * is halted while the resources are copied using a
     * WorkspaceModifyOperation. This method should be called from the UI
     * Thread.
     *
     * @param uris
     *            the URIs to copy
     * @param destination
     *            destination to which files will be copied
     * @see WorkspaceModifyOperation
     * @see Display#getThread()
     * @see Thread#currentThread()
     * @since 3.2
     */
    public void copyFiles(URI[] uris, IContainer destination) {
        IFileStore[] stores = buildFileStores(uris);
        if (stores == null) {
            return;
        }

        copyFileStores(destination, stores, true, null);
    }

    /**
     * Copies the given files and folders to the destination without forking a
     * new Thread or blocking using a WorkspaceModifyOperation. It is
     * recommended that this method only be called from a {@link WorkspaceJob}
     * to avoid possible deadlock.
     *
     * @param uris
     *            the URIs to copy
     * @param destination
     *            destination to which URIS will be copied
     * @param monitor
     *            the monitor that information will be sent to.
     * @see WorkspaceModifyOperation
     * @see WorkspaceJob
     * @since 3.2
     */
    public void copyFilesInCurrentThread(URI[] uris, IContainer destination, IProgressMonitor monitor) {
        IFileStore[] stores = buildFileStores(uris);
        if (stores == null) {
            return;
        }

        copyFileStores(destination, stores, false, monitor);
    }

    /**
     * Build the collection of fileStores that map to fileNames. If any of them
     * cannot be found then match then return <code>null</code>.
     *
     * @param uris
     * @return IFileStore[]
     */
    private IFileStore[] buildFileStores(URI[] uris) {
        IFileStore[] stores = new IFileStore[uris.length];
        for (int i = 0; i < uris.length; i++) {
            IFileStore store;
            try {
                store = EFS.getStore(uris[i]);
            } catch (CoreException e) {
                IDEWorkbenchPlugin.log(e.getMessage(), e);
                reportFileInfoNotFound(uris[i].toString());
                return null;
            }
            if (store == null) {
                reportFileInfoNotFound(uris[i].toString());
                return null;
            }
            stores[i] = store;
        }
        return stores;

    }

    /**
     * Copies the given files and folders to the destination. The current Thread
     * is halted while the resources are copied using a
     * WorkspaceModifyOperation. This method should be called from the UI
     * Thread.
     *
     * @param fileNames
     *            names of the files to copy
     * @param destination
     *            destination to which files will be copied
     * @see WorkspaceModifyOperation
     * @see Display#getThread()
     * @see Thread#currentThread()
     * @since 3.2
     */
    public void copyFiles(final String[] fileNames, IContainer destination) {
        IFileStore[] stores = buildFileStores(fileNames);
        if (stores == null) {
            return;
        }

        copyFileStores(destination, stores, true, null);
    }

    /**
     * Copies the given files and folders to the destination without forking a
     * new Thread or blocking using a WorkspaceModifyOperation. It is
     * recommended that this method only be called from a {@link WorkspaceJob}
     * to avoid possible deadlock.
     *
     * @param fileNames
     *            names of the files to copy
     * @param destination
     *            destination to which files will be copied
     * @param monitor
     *            the monitor that information will be sent to.
     * @see WorkspaceModifyOperation
     * @see WorkspaceJob
     * @since 3.2
     */
    public void copyFilesInCurrentThread(final String[] fileNames, IContainer destination, IProgressMonitor monitor) {
        IFileStore[] stores = buildFileStores(fileNames);
        if (stores == null) {
            return;
        }

        copyFileStores(destination, stores, false, monitor);
    }

    /**
     * Build the collection of fileStores that map to fileNames. If any of them
     * cannot be found then match then return null.
     *
     * @param fileNames
     * @return IFileStore[]
     */
    private IFileStore[] buildFileStores(final String[] fileNames) {
        IFileStore[] stores = new IFileStore[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            IFileStore store = IDEResourceInfoUtils.getFileStore(fileNames[i]);
            if (store == null) {
                reportFileInfoNotFound(fileNames[i]);
                return null;
            }
            stores[i] = store;
        }
        return stores;
    }

    /**
     * Report that a file info could not be found.
     *
     * @param fileName
     */
    private void reportFileInfoNotFound(final String fileName) {

        messageShell.getDisplay().syncExec(new Runnable() {
            public void run() {
                ErrorDialog.openError(messageShell, getProblemsTitle(),
                        NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_infoNotFound, fileName), null);
            }
        });
    }

    /**
     * Copies the given files and folders to the destination.
     *
     * @param stores
     *            the file stores to copy
     * @param destination
     *            destination to which files will be copied
     */
    private void copyFileStores(IContainer destination, final IFileStore[] stores, boolean fork,
            IProgressMonitor monitor) {
        // test files for existence separate from validate API
        // because an external file may not exist until the copy actually
        // takes place (e.g., WinZip contents).
        IStatus fileStatus = checkExist(stores);
        if (fileStatus.getSeverity() != IStatus.OK) {
            displayError(fileStatus);
            return;
        }
        String errorMsg = validateImportDestinationInternal(destination, stores);
        if (errorMsg != null) {
            displayError(errorMsg);
            return;
        }
        final IPath destinationPath = destination.getFullPath();

        if (fork) {
            WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
                @Override
                public void execute(IProgressMonitor monitor) {
                    copyFileStores(stores, destinationPath, monitor);
                }
            };
            try {
                PlatformUI.getWorkbench().getProgressService().run(true, true, op);
            } catch (InterruptedException e) {
                return;
            } catch (InvocationTargetException exception) {
                display(exception);
            }
        } else {
            copyFileStores(stores, destinationPath, monitor);
        }

        // If errors occurred, open an Error dialog
        if (errorStatus != null) {
            displayError(errorStatus);
            errorStatus = null;
        }
    }

    /**
     * Display the supplied status in an error dialog.
     *
     * @param status
     *            The status to display
     */
    private void displayError(final IStatus status) {
        messageShell.getDisplay().syncExec(new Runnable() {
            public void run() {
                ErrorDialog.openError(messageShell, getProblemsTitle(), null, status);
            }
        });
    }

    /**
     * Creates a file or folder handle for the source resource as if it were to
     * be created in the destination container.
     *
     * @param destination
     *            destination container
     * @param source
     *            source resource
     * @return IResource file or folder handle, depending on the source type.
     */
    IResource createLinkedResourceHandle(IContainer destination, IResource source) {
        IWorkspace workspace = destination.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        IPath linkPath = destination.getFullPath().append(source.getName());
        IResource linkHandle;

        if (source.getType() == IResource.FOLDER) {
            linkHandle = workspaceRoot.getFolder(linkPath);
        } else {
            linkHandle = workspaceRoot.getFile(linkPath);
        }
        return linkHandle;
    }

    /**
     * Removes the given resource from the workspace.
     *
     * @param resource
     *            resource to remove from the workspace
     * @param monitor
     *            a progress monitor for showing progress and for cancelation
     * @return true the resource was deleted successfully false the resource was
     *         not deleted because a CoreException occurred
     */
    boolean delete(IResource resource, IProgressMonitor monitor) {
        boolean force = false; // don't force deletion of out-of-sync resources

        if (resource.getType() == IResource.PROJECT) {
            // if it's a project, ask whether content should be deleted too
            IProject project = (IProject) resource;
            try {
                project.delete(true, force, monitor);
            } catch (CoreException e) {
                recordError(e); // log error
                return false;
            }
        } else {
            // if it's not a project, just delete it
            int flags = IResource.KEEP_HISTORY;
            if (force) {
                flags = flags | IResource.FORCE;
            }
            try {
                resource.delete(flags, monitor);
            } catch (CoreException e) {
                recordError(e); // log error
                return false;
            }
        }
        return true;
    }

    /**
     * Opens an error dialog to display the given message.
     *
     * @param message
     *            the error message to show
     */
    private void displayError(final String message) {
        messageShell.getDisplay().syncExec(new Runnable() {
            public void run() {
                MessageDialog.openError(messageShell, getProblemsTitle(), message);
            }
        });
    }

    /**
     * Returns the resource either casted to or adapted to an IFile.
     *
     * @param resource
     *            resource to cast/adapt
     * @return the resource either casted to or adapted to an IFile.
     *         <code>null</code> if the resource does not adapt to IFile
     */
    protected IFile getFile(IResource resource) {
        if (resource instanceof IFile) {
            return (IFile) resource;
        }
        return (IFile) ((IAdaptable) resource).getAdapter(IFile.class);
    }

    /**
     * Returns java.io.File objects for the given file names.
     *
     * @param fileNames
     *            files to return File object for.
     * @return java.io.File objects for the given file names.
     * @deprecated This method is not longer in use anywhere in this class and
     *             is only provided for backwards compatability with subclasses
     *             of the receiver.
     */
    @Deprecated
    protected File[] getFiles(String[] fileNames) {
        File[] files = new File[fileNames.length];

        for (int i = 0; i < fileNames.length; i++) {
            files[i] = new File(fileNames[i]);
        }
        return files;
    }

    /**
     * Returns the resource either casted to or adapted to an IFolder.
     *
     * @param resource
     *            resource to cast/adapt
     * @return the resource either casted to or adapted to an IFolder.
     *         <code>null</code> if the resource does not adapt to IFolder
     */
    protected IFolder getFolder(IResource resource) {
        if (resource instanceof IFolder) {
            return (IFolder) resource;
        }
        return (IFolder) ((IAdaptable) resource).getAdapter(IFolder.class);
    }

    /**
     * Returns a new name for a copy of the resource at the given path in the
     * given workspace.
     *
     * @param originalName
     *            the full path of the resource
     * @param workspace
     *            the workspace
     * @return the new full path for the copy, or <code>null</code> if the
     *         resource should not be copied
     */
    private IPath getNewNameFor(final IPath originalName, final IWorkspace workspace) {
        final IResource resource = workspace.getRoot().findMember(originalName);
        final IPath prefix = resource.getFullPath().removeLastSegments(1);
        final String returnValue[] = { "" }; //$NON-NLS-1$

        messageShell.getDisplay().syncExec(new Runnable() {
            public void run() {
                IInputValidator validator = new IInputValidator() {
                    public String isValid(String string) {
                        if (resource.getName().equals(string)) {
                            return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_nameMustBeDifferent;
                        }
                        IStatus status = workspace.validateName(string, resource.getType());
                        if (!status.isOK()) {
                            return status.getMessage();
                        }
                        if (workspace.getRoot().exists(prefix.append(string))) {
                            return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_nameExists;
                        }
                        return null;
                    }
                };

                InputDialog dialog = new InputDialog(messageShell,
                        IDEWorkbenchMessages.CopyFilesAndFoldersOperation_inputDialogTitle, NLS.bind(
                                IDEWorkbenchMessages.CopyFilesAndFoldersOperation_inputDialogMessage,
                                resource.getName()),
                        getAutoNewNameFor(originalName, workspace).lastSegment()
                                .toString(),
                        validator);
                dialog.setBlockOnOpen(true);
                dialog.open();
                if (dialog.getReturnCode() == Window.CANCEL) {
                    returnValue[0] = null;
                } else {
                    returnValue[0] = dialog.getValue();
                }
            }
        });
        if (returnValue[0] == null) {
            throw new OperationCanceledException();
        }
        return prefix.append(returnValue[0]);
    }

    /**
     * Returns the task title for this operation's progress dialog.
     *
     * @return the task title
     */
    protected String getOperationTitle() {
        return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_operationTitle;
    }

    /**
     * Returns the message for this operation's problems dialog.
     *
     * @return the problems message
     */
    protected String getProblemsMessage() {
        return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_problemMessage;
    }

    /**
     * Returns the title for this operation's problems dialog.
     *
     * @return the problems dialog title
     */
    protected String getProblemsTitle() {
        return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_copyFailedTitle;
    }

    /**
     * Returns whether the source file in a destination collision will be
     * validateEdited together with the collision itself. Returns false. Should
     * return true if the source file is to be deleted after the operation.
     *
     * @return boolean <code>true</code> if the source file in a destination
     *         collision should be validateEdited. <code>false</code> if only
     *         the destination should be validated.
     */
    protected boolean getValidateConflictSource() {
        return false;
    }

    /**
     * Returns whether the given resources are either both linked or both
     * unlinked.
     *
     * @param source
     *            source resource
     * @param destination
     *            destination resource
     * @return boolean <code>true</code> if both resources are either linked
     *         or unlinked. <code>false</code> otherwise.
     */
    protected boolean homogenousResources(IResource source, IResource destination) {
        boolean isSourceLinked = source.isLinked();
        boolean isDestinationLinked = destination.isLinked();

        return (isSourceLinked && isDestinationLinked || isSourceLinked == false && isDestinationLinked == false);
    }

    /**
     * Returns whether the given resource is accessible. Files and folders are
     * always considered accessible and a project is accessible if it is open.
     *
     * @param resource
     *            the resource
     * @return <code>true</code> if the resource is accessible, and
     *         <code>false</code> if it is not
     */
    private boolean isAccessible(IResource resource) {
        switch (resource.getType()) {
            case IResource.FILE:
                return true;
            case IResource.FOLDER:
                return true;
            case IResource.PROJECT:
                return ((IProject) resource).isOpen();
            default:
                return false;
        }
    }

    /**
     * Returns whether any of the given source resources are being recopied to
     * their current container.
     *
     * @param sourceResources
     *            the source resources
     * @param destination
     *            the destination container
     * @return <code>true</code> if at least one of the given source
     *         resource's parent container is the same as the destination
     */
    boolean isDestinationSameAsSource(IResource[] sourceResources, IContainer destination) {
        IPath destinationLocation = destination.getLocation();

        for (int i = 0; i < sourceResources.length; i++) {
            IResource sourceResource = sourceResources[i];
            if (sourceResource.getParent().equals(destination)) {
                return true;
            } else if (destinationLocation != null) {
                // do thorough check to catch linked resources. Fixes bug 29913.
                IPath sourceLocation = sourceResource.getLocation();
                IPath destinationResource = destinationLocation.append(sourceResource.getName());
                if (sourceLocation != null && sourceLocation.isPrefixOf(destinationResource)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Copies the given resources to the destination container with the given
     * name.
     * <p>
     * Note: the destination container may need to be created prior to copying
     * the resources.
     * </p>
     *
     * @param resources
     *            the resources to copy
     * @param destination
     *            the path of the destination container
     * @param monitor
     *            a progress monitor for showing progress and for cancelation
     * @return <code>true</code> if the copy operation completed without
     *         errors
     */
    private boolean performCopy(IResource[] resources, IPath destination, IProgressMonitor monitor) {
        try {
            ContainerGenerator generator = new ContainerGenerator(destination);
            generator.generateContainer(new SubProgressMonitor(monitor, 10));
            IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 75);
            copy(resources, destination, subMonitor);
        } catch (CoreException e) {
            recordError(e); // log error
            return false;
        } finally {
            monitor.done();
        }
        return true;
    }

    /**
     * Individually copies the given resources to the specified destination
     * container checking for name collisions. If a collision is detected, it is
     * saved with a new name.
     * <p>
     * Note: the destination container may need to be created prior to copying
     * the resources.
     * </p>
     *
     * @param resources
     *            the resources to copy
     * @param destination
     *            the path of the destination container
     * @return <code>true</code> if the copy operation completed without
     *         errors.
     */
    private boolean performCopyWithAutoRename(IResource[] resources, IPath destination, IProgressMonitor monitor) {
        IWorkspace workspace = resources[0].getWorkspace();

        try {
            ContainerGenerator generator = new ContainerGenerator(destination);
            generator.generateContainer(new SubProgressMonitor(monitor, 10));

            IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 75);
            subMonitor.beginTask(getOperationTitle(), resources.length);

            for (int i = 0; i < resources.length; i++) {
                IResource source = resources[i];
                IPath destinationPath = destination.append(source.getName());

                if (workspace.getRoot().exists(destinationPath)) {
                    destinationPath = getNewNameFor(destinationPath, workspace);
                }
                if (destinationPath != null) {
                    try {
                        source.copy(destinationPath, IResource.SHALLOW, new SubProgressMonitor(subMonitor, 0));
                    } catch (CoreException e) {
                        recordError(e); // log error
                        return false;
                    }
                }
                subMonitor.worked(1);
                if (subMonitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
            }
        } catch (CoreException e) {
            recordError(e); // log error
            return false;
        } finally {
            monitor.done();
        }

        return true;
    }

    /**
     * Performs an import of the given stores into the provided container.
     * Returns a status indicating if the import was successful.
     *
     * @param stores
     *            stores that are to be imported
     * @param target
     *            container to which the import will be done
     * @param monitor
     *            a progress monitor for showing progress and for cancelation
     */
    private void performFileImport(IFileStore[] stores, IContainer target, IProgressMonitor monitor) {
        IOverwriteQuery query = new IOverwriteQuery() {
            public String queryOverwrite(String pathString) {
                if (alwaysOverwrite) {
                    return ALL;
                }

                final String returnCode[] = { CANCEL };
                final String msg = NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_overwriteQuestion,
                        pathString);
                final String[] options = { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL,
                        IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
                messageShell.getDisplay().syncExec(new Runnable() {
                    public void run() {
                        MessageDialog dialog = new MessageDialog(messageShell,
                                IDEWorkbenchMessages.CopyFilesAndFoldersOperation_question, null, msg,
                                MessageDialog.QUESTION, options, 0);
                        dialog.open();
                        int returnVal = dialog.getReturnCode();
                        String[] returnCodes = { YES, ALL, NO, CANCEL };
                        returnCode[0] = returnVal == -1 ? CANCEL : returnCodes[returnVal];
                    }
                });
                if (returnCode[0] == ALL) {
                    alwaysOverwrite = true;
                } else if (returnCode[0] == CANCEL) {
                    canceled = true;
                }
                return returnCode[0];
            }
        };

        ImportOperation op = new ImportOperation(target.getFullPath(), stores[0].getParent(),
                FileStoreStructureProvider.INSTANCE, query, Arrays.asList(stores));
        op.setContext(messageShell);
        op.setCreateContainerStructure(false);
        try {
            op.run(monitor);
        } catch (InterruptedException e) {
            return;
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof CoreException) {
                displayError(((CoreException) e.getTargetException()).getStatus());
            } else {
                display(e);
            }
            return;
        }
        // Special case since ImportOperation doesn't throw a CoreException on
        // failure.
        IStatus status = op.getStatus();
        if (!status.isOK()) {
            if (errorStatus == null) {
                errorStatus = new MultiStatus(PlatformUI.PLUGIN_ID, IStatus.ERROR, getProblemsMessage(), null);
            }
            errorStatus.merge(status);
        }
    }

    /**
     * Records the core exception to be displayed to the user once the action is
     * finished.
     *
     * @param error
     *            a <code>CoreException</code>
     */
    private void recordError(CoreException error) {
        if (errorStatus == null) {
            errorStatus = new MultiStatus(PlatformUI.PLUGIN_ID, IStatus.ERROR, getProblemsMessage(), error);
        }

        errorStatus.merge(error.getStatus());
    }

    /**
     * Checks whether the destination is valid for copying the source resources.
     * <p>
     * Note this method is for internal use only. It is not API.
     * </p>
     *
     * @param destination
     *            the destination container
     * @param sourceResources
     *            the source resources
     * @return an error message, or <code>null</code> if the path is valid
     */
    public String validateDestination(IContainer destination, IResource[] sourceResources) {
        if (!isAccessible(destination)) {
            return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_destinationAccessError;
        }
        IContainer firstParent = null;
        URI destinationLocation = destination.getLocationURI();
        for (int i = 0; i < sourceResources.length; i++) {
            IResource sourceResource = sourceResources[i];
            if (firstParent == null) {
                firstParent = sourceResource.getParent();
            } else if (firstParent.equals(sourceResource.getParent()) == false) {
                // Resources must have common parent. Fixes bug 33398.
                return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_parentNotEqual;
            }

            URI sourceLocation = sourceResource.getLocationURI();
            if (sourceLocation == null) {
                if (sourceResource.isLinked()) {
                    // Don't allow copying linked resources with undefined path
                    // variables. See bug 28754.
                    return NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_missingPathVariable,
                            sourceResource.getName());
                }
                return NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_resourceDeleted,
                        sourceResource.getName());

            }
            if (sourceLocation.equals(destinationLocation)) {
                return NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_sameSourceAndDest,
                        sourceResource.getName());
            }
            // is the source a parent of the destination?
            if (new Path(sourceLocation.toString()).isPrefixOf(new Path(destinationLocation.toString()))) {
                return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_destinationDescendentError;
            }

            String linkedResourceMessage = validateLinkedResource(destination, sourceResource);
            if (linkedResourceMessage != null) {
                return linkedResourceMessage;
            }
        }
        return null;
    }

    /**
     * Validates that the given source resources can be copied to the
     * destination as decided by the VCM provider.
     *
     * @param destination
     *            copy destination
     * @param sourceResources
     *            source resources
     * @return <code>true</code> all files passed validation or there were no
     *         files to validate. <code>false</code> one or more files did not
     *         pass validation.
     */
    private boolean validateEdit(IContainer destination, IResource[] sourceResources) {
        ArrayList<IFile> copyFiles = new ArrayList<IFile>();

        collectExistingReadonlyFiles(destination.getFullPath(), sourceResources, copyFiles);
        if (copyFiles.size() > 0) {
            IFile[] files = copyFiles.toArray(new IFile[copyFiles.size()]);
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IStatus status = workspace.validateEdit(files, messageShell);

            canceled = status.isOK() == false;
            return status.isOK();
        }
        return true;
    }

    /**
     * Checks whether the destination is valid for copying the source files.
     * <p>
     * Note this method is for internal use only. It is not API.
     * </p>
     *
     * @param destination
     *            the destination container
     * @param sourceNames
     *            the source file names
     * @return an error message, or <code>null</code> if the path is valid
     */
    public String validateImportDestination(IContainer destination, String[] sourceNames) {

        IFileStore[] stores = new IFileStore[sourceNames.length];
        for (int i = 0; i < sourceNames.length; i++) {
            IFileStore store = IDEResourceInfoUtils.getFileStore(sourceNames[i]);
            if (store == null) {
                return NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_infoNotFound, sourceNames[i]);
            }
            stores[i] = store;
        }
        return validateImportDestinationInternal(destination, stores);

    }

    /**
     * Checks whether the destination is valid for copying the source file
     * stores.
     * <p>
     * Note this method is for internal use only. It is not API.
     * </p>
     * <p>
     * TODO Bug 117804. This method has been renamed to avoid a bug in the
     * Eclipse compiler with regards to visibility and type resolution when
     * linking.
     * </p>
     *
     * @param destination
     *            the destination container
     * @param sourceStores
     *            the source IFileStore
     * @return an error message, or <code>null</code> if the path is valid
     */
    private String validateImportDestinationInternal(IContainer destination, IFileStore[] sourceStores) {
        if (!isAccessible(destination)) {
            return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_destinationAccessError;
        }

        IFileStore destinationStore;
        try {
            destinationStore = EFS.getStore(destination.getLocationURI());
        } catch (CoreException exception) {
            IDEWorkbenchPlugin.log(exception.getLocalizedMessage(), exception);
            return NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_internalError,
                    exception.getLocalizedMessage());
        }
        for (int i = 0; i < sourceStores.length; i++) {
            IFileStore sourceStore = sourceStores[i];
            IFileStore sourceParentStore = sourceStore.getParent();

            if (sourceStore != null) {
                if (destinationStore.equals(sourceStore)
                        || (sourceParentStore != null && destinationStore.equals(sourceParentStore))) {
                    return NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_importSameSourceAndDest,
                            sourceStore.getName());
                }
                // work around bug 16202. replacement for
                // sourcePath.isPrefixOf(destinationPath)
                IFileStore destinationParent = destinationStore.getParent();
                if (sourceStore.isParentOf(destinationParent)) {
                    return IDEWorkbenchMessages.CopyFilesAndFoldersOperation_destinationDescendentError;
                }

            }
        }
        return null;
    }

    /**
     * Check if the destination is valid for the given source resource.
     *
     * @param destination
     *            destination container of the operation
     * @param source
     *            source resource
     * @return String error message or null if the destination is valid
     */
    private String validateLinkedResource(IContainer destination, IResource source) {
        if (source.isLinked() == false) {
            return null;
        }
        IWorkspace workspace = destination.getWorkspace();
        IResource linkHandle = createLinkedResourceHandle(destination, source);
        IStatus locationStatus = workspace.validateLinkLocation(linkHandle, source.getRawLocation());

        if (locationStatus.getSeverity() == IStatus.ERROR) {
            return locationStatus.getMessage();
        }
        IPath sourceLocation = source.getLocation();
        if (source.getProject().equals(destination.getProject()) == false && source.getType() == IResource.FOLDER
                && sourceLocation != null) {
            // prevent merging linked folders that point to the same
            // file system folder
            try {
                IResource[] members = destination.members();
                for (int j = 0; j < members.length; j++) {
                    if (sourceLocation.equals(members[j].getLocation())
                            && source.getName().equals(members[j].getName())) {
                        return NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_sameSourceAndDest,
                                source.getName());
                    }
                }
            } catch (CoreException exception) {
                displayError(NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_internalError,
                        exception.getMessage()));
            }
        }
        return null;
    }

    /**
     * Returns whether moving all of the given source resources to the given
     * destination container could be done without causing name collisions.
     *
     * @param destination
     *            the destination container
     * @param sourceResources
     *            the list of resources
     * @return <code>true</code> if there would be no name collisions, and
     *         <code>false</code> if there would
     */
    private IResource[] validateNoNameCollisions(IContainer destination, IResource[] sourceResources) {
        List<IResource> copyItems = new ArrayList<IResource>();
        IWorkspaceRoot workspaceRoot = destination.getWorkspace().getRoot();
        int overwrite = IDialogConstants.NO_ID;

        // Check to see if we would be overwriting a parent folder.
        // Cancel entire copy operation if we do.
        for (int i = 0; i < sourceResources.length; i++) {
            final IResource sourceResource = sourceResources[i];
            final IPath destinationPath = destination.getFullPath().append(sourceResource.getName());
            final IPath sourcePath = sourceResource.getFullPath();

            IResource newResource = workspaceRoot.findMember(destinationPath);
            if (newResource != null && destinationPath.isPrefixOf(sourcePath)) {
                displayError(NLS.bind(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_overwriteProblem,
                        destinationPath, sourcePath));

                canceled = true;
                return null;
            }
        }
        // Check for overwrite conflicts
        for (int i = 0; i < sourceResources.length; i++) {
            final IResource source = sourceResources[i];
            final IPath destinationPath = destination.getFullPath().append(source.getName());

            IResource newResource = workspaceRoot.findMember(destinationPath);
            if (newResource != null) {
                if (overwrite != IDialogConstants.YES_TO_ALL_ID
                        || (newResource.getType() == IResource.FOLDER
                        && homogenousResources(source, destination) == false)) {
                    overwrite = checkOverwrite(source, newResource);
                }
                if (overwrite == IDialogConstants.YES_ID || overwrite == IDialogConstants.YES_TO_ALL_ID) {
                    copyItems.add(source);
                } else if (overwrite == IDialogConstants.CANCEL_ID) {
                    canceled = true;
                    return null;
                }
            } else {
                copyItems.add(source);
            }
        }
        return copyItems.toArray(new IResource[copyItems.size()]);
    }

    private void copyResources(final IResource[] resources, final IPath destinationPath,
            final IResource[][] copiedResources, IProgressMonitor monitor) {
        IResource[] copyResources = resources;

        // Fix for bug 31116. Do not provide a task name when
        // creating the task.
        monitor.beginTask("", 100); //$NON-NLS-1$
        monitor.setTaskName(getOperationTitle());
        monitor.worked(10); // show some initial progress

        // Checks only required if this is an exisiting container path.
        boolean copyWithAutoRename = false;
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (root.exists(destinationPath)) {
            IContainer container = (IContainer) root.findMember(destinationPath);
            // If we're copying to the source container then perform
            // auto-renames on all resources to avoid name collisions.
            if (isDestinationSameAsSource(copyResources, container) && canPerformAutoRename()) {
                copyWithAutoRename = true;
            } else {
                // If no auto-renaming will be happening, check for
                // potential name collisions at the target resource
                copyResources = validateNoNameCollisions(container, copyResources);
                if (copyResources == null) {
                    if (canceled) {
                        return;
                    }
                    displayError(IDEWorkbenchMessages.CopyFilesAndFoldersOperation_nameCollision);
                    return;
                }
                if (validateEdit(container, copyResources) == false) {
                    return;
                }
            }
        }

        errorStatus = null;
        if (copyResources.length > 0) {
            if (copyWithAutoRename) {
                performCopyWithAutoRename(copyResources, destinationPath, monitor);
            } else {
                performCopy(copyResources, destinationPath, monitor);
            }
        }
        copiedResources[0] = copyResources;
    }

    private void copyFileStores(final IFileStore[] stores, final IPath destinationPath, IProgressMonitor monitor) {
        // Checks only required if this is an exisiting container path.
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        if (root.exists(destinationPath)) {
            IContainer container = (IContainer) root.findMember(destinationPath);

            performFileImport(stores, container, monitor);
        }
    }

    /**
     * Returns the model provider ids that are known to the client that
     * instantiated this operation.
     *
     * @return the model provider ids that are known to the client that
     *         instantiated this operation.
     * @since 3.2
     */
    public String[] getModelProviderIds() {
        return modelProviderIds;
    }

    /**
     * Sets the model provider ids that are known to the client that
     * instantiated this operation. Any potential side effects reported by these
     * models during validation will be ignored.
     *
     * @param modelProviderIds
     *            the model providers known to the client who is using this
     *            operation.
     * @since 3.2
     */
    public void setModelProviderIds(String[] modelProviderIds) {
        this.modelProviderIds = modelProviderIds;
    }
}
