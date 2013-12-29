/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Action used to delete the .pyc and $py.class files (generated from the python or jython interpreter).
 *  
 * @author Fabio
 */
public class PyDeletePycAndClassFiles extends PyContainerAction {

    /**
     * Deletes the files... recursively pass the folders and delete the files (and sum them so that we know how many
     * files were deleted).
     * 
     * @param container the folder from where we want to remove the files
     * @return the number of files deleted
     */
    @Override
    protected int doActionOnContainer(IContainer container, IProgressMonitor monitor) {
        int deleted = 0;
        try {
            IResource[] members = container.members();

            for (IResource c : members) {
                if (monitor.isCanceled()) {
                    break;
                }

                monitor.worked(1);
                if (c instanceof IContainer) {
                    deleted += this.doActionOnContainer((IContainer) c, monitor);

                } else if (c instanceof IFile) {
                    String name = c.getName();
                    if (name != null) {
                        if (name.endsWith(".pyc") || name.endsWith(".pyo") || name.endsWith("$py.class")) {
                            c.delete(true, monitor);
                            deleted += 1;
                        }
                    }
                }
            }
        } catch (CoreException e) {
            Log.log(e);
        }

        return deleted;
    }

    @Override
    protected void afterRun(int deleted) {
        MessageDialog.openInformation(null, "Files deleted", StringUtils.format("Deleted %s files.", deleted));
    }

    @Override
    protected boolean confirmRun() {
        return MessageDialog
                .openConfirm(
                        null,
                        "Confirm deletion",
                        "Are you sure that you want to recursively delete the *.pyc and *$py.class files from the selected folder(s)?\n"
                                + "\n" + "This action cannot be undone.");
    }

}
