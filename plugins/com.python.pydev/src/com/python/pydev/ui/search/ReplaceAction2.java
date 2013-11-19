/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.search;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.text.FileSearchPage;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.util.ExceptionHandler;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;

/* package */class ReplaceAction2 extends Action {

    private IWorkbenchSite fSite;
    private IFile[] fElements;
    private FileSearchPage fPage;

    private static class ItemIterator implements Iterator {
        private Item[] fArray;
        private int fNextPosition;

        ItemIterator(Item[] array) {
            fArray = array;
            fNextPosition = 0;
        }

        public boolean hasNext() {
            return fNextPosition < fArray.length;
        }

        public Object next() {
            if (!hasNext())
                throw new NoSuchElementException();
            return fArray[fNextPosition++].getData();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public ReplaceAction2(FileSearchPage page, IFile[] elements) {
        Assert.isNotNull(page);
        fSite = page.getSite();
        if (elements != null)
            fElements = elements;
        else
            fElements = new IFile[0];
        fPage = page;

        setText(SearchMessages.ReplaceAction_label_all);
        setEnabled(!(fElements.length == 0));
    }

    public ReplaceAction2(FileSearchPage page) {
        Assert.isNotNull(page);
        fSite = page.getSite();
        fPage = page;

        Item[] items = null;
        StructuredViewer viewer = fPage.getViewer();
        if (viewer instanceof TreeViewer) {
            items = ((TreeViewer) viewer).getTree().getItems();
        } else if (viewer instanceof TableViewer) {
            items = ((TableViewer) viewer).getTable().getItems();
        }
        fElements = collectFiles(new ItemIterator(items));

        setText(SearchMessages.ReplaceAction_label_all);
        setEnabled(!(fElements.length == 0));
    }

    public ReplaceAction2(FileSearchPage page, IStructuredSelection selection) {
        fSite = page.getSite();
        fPage = page;
        setText(SearchMessages.ReplaceAction_label_selected);
        fElements = collectFiles(selection.iterator());
        setEnabled(!(fElements.length == 0));
    }

    private IFile[] collectFiles(Iterator resources) {
        final Set<IResource> files = new HashSet<IResource>();
        final AbstractTextSearchResult result = fPage.getInput();
        if (result == null)
            return new IFile[0];
        while (resources.hasNext()) {
            IResource resource = (IResource) resources.next();
            try {
                resource.accept(new IResourceProxyVisitor() {
                    public boolean visit(IResourceProxy proxy) throws CoreException {
                        if (proxy.getType() == IResource.FILE) {
                            IResource file = proxy.requestResource();
                            if (result.getMatchCount(file) > 0) {
                                files.add(file);
                            }
                            return false;
                        }
                        return true;
                    }
                }, IResource.NONE);
            } catch (CoreException e) {
                // TODO Don't know yet how to handle this. This is called when we open the context
                // menu. A bad time to show a dialog.
                SearchPlugin.getDefault().getLog().log(e.getStatus());
            }
        }
        return (IFile[]) files.toArray(new IFile[files.size()]);
    }

    public void run() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        ISchedulingRule rule = workspace.getRuleFactory().modifyRule(workspace.getRoot());
        try {
            Job.getJobManager().beginRule(rule, null);
            if (validateResources((FileSearchQuery) fPage.getInput().getQuery())) {
                ReplaceDialog2 dialog = new ReplaceDialog2(fSite.getShell(), fElements, fPage);
                dialog.open();
            }
        } catch (OperationCanceledException e) {
        } finally {
            Job.getJobManager().endRule(rule);
        }
    }

    private boolean validateResources(final FileSearchQuery operation) {
        IFile[] readOnlyFiles = getReadOnlyFiles();
        IStatus status = ResourcesPlugin.getWorkspace().validateEdit(readOnlyFiles, fSite.getShell());
        if (!status.isOK()) {
            if (status.getSeverity() != IStatus.CANCEL) {
                ErrorDialog.openError(fSite.getShell(), SearchMessages.ReplaceAction2_error_validate_title,
                        SearchMessages.ReplaceAction2_error_validate_message, status);
            }
            return false;
        }

        final List<IFile> outOfDateEntries = new ArrayList<IFile>();
        for (int j = 0; j < fElements.length; j++) {
            IFile entry = fElements[j];
            Match[] markers = fPage.getDisplayedMatches(entry);
            for (int i = 0; i < markers.length; i++) {
                if (isOutOfDate((FileMatch) markers[i])) {
                    outOfDateEntries.add(entry);
                    break;
                }
            }
        }

        final List<IFile> outOfSyncEntries = new ArrayList<IFile>();
        for (int i = 0; i < fElements.length; i++) {
            IFile entry = fElements[i];
            if (isOutOfSync(entry)) {
                outOfSyncEntries.add(entry);
            }
        }

        if (outOfDateEntries.size() > 0 || outOfSyncEntries.size() > 0) {
            if (askForResearch(outOfDateEntries, outOfSyncEntries)) {
                ProgressMonitorDialog pmd = new AsynchronousProgressMonitorDialog(fSite.getShell());
                try {
                    pmd.run(true, true, new WorkspaceModifyOperation(null) {
                        protected void execute(IProgressMonitor monitor) throws CoreException {
                            research(monitor, outOfDateEntries, operation);
                        }
                    });
                    return true;
                } catch (InvocationTargetException e) {
                    ExceptionHandler.handle(e, fSite.getShell(), SearchMessages.ReplaceAction_label,
                            SearchMessages.ReplaceAction_research_error);
                } catch (InterruptedException e) {
                    // canceled
                }
            }
            return false;
        }
        return true;
    }

    private IFile[] getReadOnlyFiles() {
        Set<IFile> readOnly = new HashSet<IFile>();
        for (int i = 0; i < fElements.length; i++) {
            if (fElements[i].isReadOnly())
                readOnly.add(fElements[i]);
        }
        IFile[] readOnlyArray = new IFile[readOnly.size()];
        return (IFile[]) readOnly.toArray(readOnlyArray);
    }

    private void research(IProgressMonitor monitor, List<IFile> outOfDateEntries, FileSearchQuery operation)
            throws CoreException {
        String message = SearchMessages.ReplaceAction2_statusMessage;
        MultiStatus multiStatus = new MultiStatus(NewSearchUI.PLUGIN_ID, IStatus.OK, message, null);
        for (Iterator elements = outOfDateEntries.iterator(); elements.hasNext();) {
            IFile entry = (IFile) elements.next();
            IStatus status = research(operation, monitor, entry);
            if (status != null && !status.isOK()) {
                multiStatus.add(status);
            }
        }
        if (!multiStatus.isOK()) {
            throw new CoreException(multiStatus);
        }
    }

    private boolean askForResearch(List<IFile> outOfDateEntries, List<IFile> outOfSyncEntries) {
        SearchAgainConfirmationDialog dialog = new SearchAgainConfirmationDialog(fSite.getShell(),
                (ILabelProvider) fPage.getViewer().getLabelProvider(), outOfSyncEntries, outOfDateEntries);
        return dialog.open() == IDialogConstants.OK_ID;
    }

    private boolean isOutOfDate(FileMatch match) {

        if (match.getCreationTimeStamp() != match.getFile().getModificationStamp())
            return true;
        ITextFileBufferManager bm = FileBuffers.getTextFileBufferManager();
        ITextFileBuffer fb = bm.getTextFileBuffer(match.getFile().getFullPath());
        if (fb != null && fb.isDirty())
            return true;
        return false;
    }

    private boolean isOutOfSync(IFile entry) {
        return !entry.isSynchronized(IResource.DEPTH_ZERO);
    }

    private IStatus research(FileSearchQuery operation, final IProgressMonitor monitor, IFile entry) {
        Match[] matches = fPage.getDisplayedMatches(entry);
        IStatus status = operation.searchInFile(getResult(), monitor, entry);

        // always remove old matches
        for (int i = 0; i < matches.length; i++) {
            getResult().removeMatch(matches[i]);
        }
        return status;
    }

    private AbstractTextSearchResult getResult() {
        return fPage.getInput();
    }

}
