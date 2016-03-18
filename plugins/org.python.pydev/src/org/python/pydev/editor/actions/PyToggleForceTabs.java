/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.editor.PyEdit;

public class PyToggleForceTabs extends PyAction {

    @Override
    public void run(IAction action) {
        if (targetEditor instanceof PyEdit) {
            PyEdit pyEdit = (PyEdit) targetEditor;
            IIndentPrefs indentPrefs = pyEdit.getIndentPrefs();
            indentPrefs.setForceTabs(!indentPrefs.getForceTabs());
            updateActionState(indentPrefs);
        }
    }

    private void updateActionState(IIndentPrefs indentPrefs) {
        //This doesn't work! (setChecked and setImageDescriptor don't seem to update the action in the pop up menu).
        //		setChecked(forceTabs);
        //		setImageDescriptor(desc);

        PyEdit pyEdit = getPyEdit();
        pyEdit.updateForceTabsMessage();
    }

}
