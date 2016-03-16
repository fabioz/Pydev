/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.log.Log;


/**
 * Action used to delete the error markers
 *  
 * @author Fabio
 */
public class PyDeleteErrors extends PyContainerAction {

    /**
     * Deletes the error markers... recursively pass the folders and delete the files (and sum them so that we know how many
     * files were affected).
     * 
     * @param container the folder from where we want to remove the markers
     * @return the number of markers deleted
     */
    @Override
    protected int doActionOnContainer(IContainer container, IProgressMonitor monitor) {
        try {
            container.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch (CoreException e) {
            Log.log(e);
        }

        if (monitor.isCanceled()) {
            return -1;
        }
        try {
            container.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            Log.log(e);
        }

        return -1;
    }

    @Override
    protected void afterRun(int deleted) {
    }

    @Override
    protected boolean confirmRun() {
        return MessageDialog.openConfirm(null, "Confirm deletion",
                "Are you sure that you want to recursively remove all the markers from the selected folder(s)?");
    }

}
