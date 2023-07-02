package com.python.pydev.refactoring.refactorer.search.copied;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ReadOnlyStateChecker;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Implements drag behaviour when items are dragged out of the resource
 * navigator. Copied from deprecated org.eclipse.ui.views.navigator.NavigatorDragAdapter
 */
public class NavigatorDragAdapter extends DragSourceAdapter {
    private static final String CHECK_MOVE_TITLE = "Check Move";

    private static final String CHECK_DELETE_MESSAGE = "{0} is read-only. Do you still wish to delete it?";

    ISelectionProvider selectionProvider;

    private TransferData lastDataType;

    /**
     * Constructs a new drag adapter.
     *
     * @param provider The selection provider
     */
    public NavigatorDragAdapter(ISelectionProvider provider) {
        selectionProvider = provider;
    }

    /**
     * This implementation of
     * {@link DragSourceListener#dragFinished(DragSourceEvent)} responds to a drag
     * that has moved resources outside the Navigator by deleting the corresponding
     * source resource.
     */
    @Override
    public void dragFinished(DragSourceEvent event) {
        LocalSelectionTransfer.getTransfer().setSelection(null);

        if (event.doit == false) {
            return;
        }

        final int typeMask = IResource.FOLDER | IResource.FILE;
        if (event.detail == DND.DROP_MOVE) {
            // never delete resources when dragging outside Eclipse.
            // workaround for bug 30543.
            if (lastDataType != null && FileTransfer.getInstance().isSupportedType(lastDataType)) {
                return;
            }

            IResource[] resources = getSelectedResources(typeMask);
            DragSource dragSource = (DragSource) event.widget;
            Control control = dragSource.getControl();
            Shell shell = control.getShell();
            ReadOnlyStateChecker checker;

            if (resources == null || resources.length == 0) {
                return;
            }

            checker = new ReadOnlyStateChecker(shell, CHECK_MOVE_TITLE, CHECK_DELETE_MESSAGE);
            resources = checker.checkReadOnlyResources(resources);
            // delete the old elements
            for (IResource resource : resources) {
                try {
                    resource.delete(IResource.KEEP_HISTORY | IResource.FORCE, null);
                } catch (CoreException e) {
                    StatusManager.getManager().handle(e, "com.python.pydev.refactoring");
                }
            }
        } else if (event.detail == DND.DROP_TARGET_MOVE) {
            IResource[] resources = getSelectedResources(typeMask);

            // file moved for us by OS, no need to delete the resources, just
            // update the view
            if (resources == null) {
                return;
            }
            for (IResource resource : resources) {
                try {
                    resource.refreshLocal(IResource.DEPTH_INFINITE, null);
                } catch (CoreException e) {
                    StatusManager.getManager().handle(e, "com.python.pydev.refactoring");
                }
            }
        }
    }

    /**
     * This implementation of
     * {@link DragSourceListener#dragSetData(DragSourceEvent)} sets the drag event
     * data based on the current selection in the Navigator.
     */
    @Override
    public void dragSetData(DragSourceEvent event) {
        final int typeMask = IResource.FILE | IResource.FOLDER;
        IResource[] resources = getSelectedResources(typeMask);

        if (resources == null || resources.length == 0) {
            return;
        }

        lastDataType = event.dataType;
        // use local selection transfer if possible
        if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
            event.data = LocalSelectionTransfer.getTransfer().getSelection();
            return;
        }
        // use resource transfer if possible
        if (ResourceTransfer.getInstance().isSupportedType(event.dataType)) {
            event.data = resources;
            return;
        }
        // resort to a file transfer
        if (!FileTransfer.getInstance().isSupportedType(event.dataType)) {
            return;
        }

        // Get the path of each file and set as the drag data
        final int length = resources.length;
        int actualLength = 0;
        String[] fileNames = new String[length];
        for (int i = 0; i < length; i++) {
            IPath location = resources[i].getLocation();
            // location may be null. See bug 29491.
            if (location != null) {
                fileNames[actualLength++] = location.toOSString();
            }
        }
        if (actualLength == 0) {
            return;
        }
        // was one or more of the locations null?
        if (actualLength < length) {
            String[] tempFileNames = fileNames;
            fileNames = new String[actualLength];
            System.arraycopy(tempFileNames, 0, fileNames, 0, actualLength);
        }
        event.data = fileNames;
    }

    /**
     * This implementation of {@link DragSourceListener#dragStart(DragSourceEvent)}
     * allows the drag to start if the current Navigator selection contains
     * resources that can be dragged.
     */
    @Override
    public void dragStart(DragSourceEvent event) {
        lastDataType = null;
        // Workaround for 1GEUS9V
        DragSource dragSource = (DragSource) event.widget;
        Control control = dragSource.getControl();
        if (control != control.getDisplay().getFocusControl()) {
            event.doit = false;
            return;
        }

        IStructuredSelection selection = (IStructuredSelection) selectionProvider.getSelection();
        for (Object next : selection) {
            if (!(next instanceof IFile || next instanceof IFolder)) {
                event.doit = false;
                return;
            }
        }
        if (selection.isEmpty()) {
            event.doit = false;
            return;
        }
        LocalSelectionTransfer.getTransfer().setSelection(selection);
        event.doit = true;
    }

    private IResource[] getSelectedResources(int resourceTypes) {
        List<IResource> resources = new ArrayList<>();

        ISelection selection = selectionProvider.getSelection();
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            return null;
        }
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;

        // loop through list and look for matching items
        for (Object obj : structuredSelection) {
            if (obj instanceof IResource) {
                IResource res = (IResource) obj;
                if ((res.getType() & resourceTypes) == res.getType()) {
                    resources.add(res);
                }
            }
        }
        IResource[] result = new IResource[resources.size()];
        resources.toArray(result);
        return result;
    }
}
