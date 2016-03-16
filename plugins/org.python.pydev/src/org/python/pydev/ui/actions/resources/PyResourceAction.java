/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.resources;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;


/**
 * Abstract class for actions that'll act upon the selected resources.
 * 
 * @author Fabio
 */
public abstract class PyResourceAction {

    /**
     * Subclasses can override to determine if the resource should be refreshed before the action is executed or not.
     */
    protected boolean getRefreshBeforeExecute() {
        return true;
    }

    /**
     * List with the resources the user selected 
     */
    protected List<IResource> selectedResources;

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        //empty
    }

    /**
     * When the selection changes, we've to keep the selected resources...
     */
    @SuppressWarnings("unchecked")
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            selectedResources = null;
            return;
        }

        IStructuredSelection selections = (IStructuredSelection) selection;
        ArrayList<IResource> resources = new ArrayList<IResource>();

        for (Iterator<Object> it = selections.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof IResource) {
                resources.add((IResource) o);

            } else if (o instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) o;
                IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                if (resource != null) {
                    resources.add(resource);
                }
            }
        }

        this.selectedResources = resources;
    }

    /**
     * Act on the selection to do the needed action (will confirm and make a refresh before executing)
     */
    public void run(IAction action) {
        //should not happen
        if (selectedResources == null) {
            return;
        }

        if (!confirmRun()) {
            return;
        }

        beforeRun();

        final Integer[] nChanged = new Integer[] { 0 };
        ProgressMonitorDialog monitorDialog = new AsynchronousProgressMonitorDialog(EditorUtils.getShell());
        try {
            IRunnableWithProgress operation = new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    for (Iterator<IResource> iter = selectedResources.iterator(); iter.hasNext();) {
                        IResource next = iter.next();
                        if (getRefreshBeforeExecute()) {
                            //as files are generated externally, if we don't refresh, it's very likely that we won't delete a bunch of files.
                            try {
                                next.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                            } catch (Exception e) {
                                Log.log(e);
                            }
                        }
                        nChanged[0] += doActionOnResource(next, monitor);
                    }
                }
            };
            boolean fork = !needsUIThread();
            monitorDialog.run(fork, true, operation);
        } catch (Throwable e) {
            Log.log(e);
        }

        afterRun(nChanged[0]);
    }

    /**
     * Called before actually running the action.
     */
    protected void beforeRun() {
        //do nothing by default.
    }

    /**
     * @return true if the action should be run and false otherwise
     */
    protected abstract boolean confirmRun();

    /**
     * If it needs UI access, 
     * @return true if UI access is needed (and false -- which is the default -- otherwise).
     * 
     * @note If it needs the UI access, it needs to call Display.readAndDispatch() to assure that 
     * the interface remains responsive.
     */
    protected boolean needsUIThread() {
        return false;
    }

    /**
     * Hook for clients to implement after the run is done (useful to show message)
     * 
     * @param resourcesAffected the number of resources that've been affected.
     */
    protected abstract void afterRun(int resourcesAffected);

    /**
     * Executes the action on the resource passed
     * 
     * @param next the resource where the action should be executed
     * @param monitor The monitor that should be used to report the progress.
     * @return the number of resources affected in the action
     */
    protected abstract int doActionOnResource(IResource next, IProgressMonitor monitor);

}
