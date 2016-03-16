/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;


/**
 * Abstract class for actions that'll act upon the selected containers.
 * 
 * @author Fabio
 */
public abstract class PyContainerAction implements IObjectActionDelegate {

    /**
     * Subclasses can override to determine if the container should be refreshed before the action is executed or not.
     */
    protected boolean getRefreshBeforeExecute() {
        return true;
    }

    /**
     * List with the containers the user selected 
     */
    protected List<IContainer> selectedContainers;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        //empty
    }

    /**
     * When the selection changes, we've to keep the selected containers...
     */
    @Override
    @SuppressWarnings("unchecked")
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            selectedContainers = null;
            return;
        }

        IStructuredSelection selections = (IStructuredSelection) selection;
        ArrayList<IContainer> containers = new ArrayList<IContainer>();

        for (Iterator<Object> it = selections.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof IContainer) {
                containers.add((IContainer) o);

            } else if (o instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) o;
                IContainer container = (IContainer) adaptable.getAdapter(IContainer.class);
                if (container != null) {
                    containers.add(container);
                }
            }
        }

        this.selectedContainers = containers;
    }

    /**
     * Act on the selection to do the needed action (will confirm and make a refresh before executing)
     */
    @Override
    public void run(IAction action) {
        //should not happen
        if (selectedContainers == null) {
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
                    for (Iterator<IContainer> iter = selectedContainers.iterator(); iter.hasNext();) {
                        IContainer next = iter.next();
                        if (getRefreshBeforeExecute()) {
                            //as files are generated externally, if we don't refresh, it's very likely that we won't delete a bunch of files.
                            try {
                                next.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                            } catch (Exception e) {
                                Log.log(e);
                            }
                        }
                        nChanged[0] += doActionOnContainer(next, monitor);
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
     * Executes the action on the container passed
     * 
     * @param next the container where the action should be executed
     * @param monitor The monitor that should be used to report the progress.
     * @return the number of resources affected in the action
     */
    protected abstract int doActionOnContainer(IContainer next, IProgressMonitor monitor);

}
