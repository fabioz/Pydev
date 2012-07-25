/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

public class RelaunchLastAction implements IEditorActionDelegate {

    public void run(IAction action) {
        RestartLaunchAction.relaunchLast();
    }

    public void selectionChanged(IAction action, ISelection selection) {

    }

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {

    }

}
