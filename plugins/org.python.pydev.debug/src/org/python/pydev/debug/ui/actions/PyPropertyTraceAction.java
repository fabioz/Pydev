/******************************************************************************
* Copyright (C) 2011-2013  Hussain Bohra and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Hussain Bohra <hussain.bohra@tavant.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>       - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.python.pydev.debug.model.PyPropertyTraceManager;
import org.python.pydev.debug.ui.PyPropertyTraceDialog;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.shared_ui.EditorUtils;

public class PyPropertyTraceAction extends PyAction implements IWorkbenchWindowActionDelegate {

    @Override
    public void run(IAction arg0) {
        PyPropertyTraceDialog dialog = new PyPropertyTraceDialog(EditorUtils.getShell());
        dialog.setTitle("Enable/Disable Step Into properties");
        if (dialog.open() == PyPropertyTraceDialog.OK) {
            PyPropertyTraceManager.getInstance().setPyPropertyTraceState(dialog.isDisableStepIntoProperties(),
                    dialog.isDisableStepIntoGetter(), dialog.isDisableStepIntoSetter(),
                    dialog.isDisableStepIntoDeleter());
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void init(IWorkbenchWindow arg0) {
    }
}
