/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author fabioz
 *
 */
public class PyDialogHelpers {

    public static void openWarning(String title, String message) {
        Shell shell = PyAction.getShell();
        MessageDialog.openWarning(
                shell, title, message);
    }
    
    public static void openCritical(String title, String message) {
        Shell shell = PyAction.getShell();
        MessageDialog.openError(
                shell, title, message);
    }
    
    public static int openWarningWithIgnoreToggle(
            String title, String message, String key) {
        Shell shell = PyAction.getShell();
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String val = store.getString(key);
        if (val.trim().length() == 0) {
            val = MessageDialogWithToggle.PROMPT; //Initial value if not specified
        }
        
        if (!val.equals(MessageDialogWithToggle.ALWAYS)) {
            MessageDialogWithToggle.openWarning(
                    shell, title, message, "Don't show this message again",
                    false, store, key);
        }
        return MessageDialog.OK;
    }
}
