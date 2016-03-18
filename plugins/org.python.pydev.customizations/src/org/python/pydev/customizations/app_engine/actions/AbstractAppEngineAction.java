/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Just checks if the selection has size == 1 and if it has, mark that as the source folder
 * (doesn't actually check if it is a source folder)
 */
public abstract class AbstractAppEngineAction implements IObjectActionDelegate {

    protected Object sourceFolder;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        sourceFolder = null;
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }

        IStructuredSelection selections = (IStructuredSelection) selection;
        if (selections.size() != 1) {
            return;
        }

        this.sourceFolder = selections.getFirstElement();
    }
}
