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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.MoveResourceAction;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;

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
    @Override
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
            title = "Choose destination for " + selected.size() + " selected resources:";
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
    @Override
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
            PythonPathHelper.updatePyPath(resources, destination,
                    PythonPathHelper.OPERATION_MOVE);
        }
    }

}
