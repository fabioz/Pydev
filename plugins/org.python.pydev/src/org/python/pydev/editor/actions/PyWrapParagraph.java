/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.util.Collection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.editor.ActionInfo;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_ui.EditorUtils;

public class PyWrapParagraph extends PyAction {

    /**
     * Makes the wrap paragraph (registered from the scripting engine).
     */
    @Override
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }

            PyEdit pyEdit = getPyEdit();
            Collection<ActionInfo> offlineActionDescriptions = pyEdit.getOfflineActionDescriptions();
            for (ActionInfo actionInfo : offlineActionDescriptions) {
                if ("wrap paragraph".equals(actionInfo.description.trim().toLowerCase())) {
                    actionInfo.action.run();
                    return;
                }
            }
            MessageDialog.openError(EditorUtils.getShell(), "Error", "Wrap paragraph is still not available.");
        } catch (Exception e) {
            beep(e);
        }
    }
}
