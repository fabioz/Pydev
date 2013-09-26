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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;

/**
 * Overriden org.eclipse.ui.actions.DeleteResourceAction
 * 
 * with the following changes:
 * - isEnabled overriden to compute the changes accordingly
 * - in the run we update the selection correctly (by calling isEnabled), because this was not synched correctly in the 
 * eclipse version (because there could be a delay there).
 * 
 * @author Fabio
 */
public class PyDeleteResourceAction extends DeleteResourceAction {

    private ISelectionProvider provider;

    private List<IResource> selected;
    private List<IFolder> remFolders;

    public PyDeleteResourceAction(Shell shell, ISelectionProvider selectionProvider) {
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

    /**
     * Update the PYTHONPATH of projects that have had source folders removed from them by
     * removing the folders' paths from it.
     */
    private void updatePyPath() {
        // Quit if no source folder was deleted.
        if (remFolders.size() == 0 || remFolders.get(0).exists()) {
            return;
        }
        PythonPathHelper.updatePyPath(remFolders.toArray(new IResource[0]), null,
                PythonPathHelper.OPERATION_DELETE);
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
        if (!fillSelection()) { //will also update the list of resources (main change from the DeleteResourceAction)
            return;
        }
        Helpers.checkValidateState();
        remFolders = new ArrayList<IFolder>();
        for (IResource folder : selected) {
            if (folder instanceof IFolder) {
                remFolders.add((IFolder) folder);
            }
        }
        super.run();
        updatePyPath();
        remFolders.clear();
    }
}