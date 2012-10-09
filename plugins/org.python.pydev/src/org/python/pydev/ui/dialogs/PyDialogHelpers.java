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
import org.python.pydev.ui.interpreters.AbstractInterpreterManager;

/**
 * @author fabioz
 *
 */
public class PyDialogHelpers {

    public static void openWarning(String title, String message) {
        Shell shell = PyAction.getShell();
        MessageDialog.openWarning(shell, title, message);
    }

    public static void openCritical(String title, String message) {
        Shell shell = PyAction.getShell();
        MessageDialog.openError(shell, title, message);
    }

    public static boolean openQuestion(String title, String message) {
        Shell shell = PyAction.getShell();
        return MessageDialog.openQuestion(shell, title, message);
    }

    public static int openWarningWithIgnoreToggle(String title, String message, String key) {
        Shell shell = PyAction.getShell();
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String val = store.getString(key);
        if (val.trim().length() == 0) {
            val = MessageDialogWithToggle.PROMPT; //Initial value if not specified
        }

        if (!val.equals(MessageDialogWithToggle.ALWAYS)) {
            MessageDialogWithToggle.openWarning(shell, title, message, "Don't show this message again", false, store,
                    key);
        }
        return MessageDialog.OK;
    }

    /**
     * @return the index chosen or -1 if it was canceled.
     */
    public static int openCriticalWithChoices(String title, String message, String[] choices) {
        Shell shell = PyAction.getShell();
        MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.ERROR, choices, 0);
        return dialog.open();
    }

    public final static int INTERPRETER_AUTO_CONFIG = 0;
    public final static int INTERPRETER_MANUAL_CONFIG = 1;
    public final static int INTERPRETER_DONT_ASK_CONFIG = 2;
    public final static int INTERPRETER_CANCEL_CONFIG = -1;
    private static final String DONT_ASK_AGAIN_PREFERENCE_VALUE = "DONT_ASK";

    public static int openQuestionConfigureInterpreter(AbstractInterpreterManager m) {
        String title = m.getInterpreterUIName() + " not configured";
        String message = "It seems that the " + m.getInterpreterUIName()
                + " interpreter is not currently configured.\n\nHow do you want to proceed?";
        String key = "INTERPRETER_CONFIGURATION_" + m.getInterpreterType();

        Shell shell = PyAction.getShell();
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String val = store.getString(key);

        if (!val.equals(DONT_ASK_AGAIN_PREFERENCE_VALUE)) {
            MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, new String[] {
                    "Auto config", "Manual config", "Don't ask again" }, 0);
            int open = dialog.open();
            switch (open) {
                case 0:
                    //auto config
                    return INTERPRETER_AUTO_CONFIG;

                case 1:
                    //manual config
                    return INTERPRETER_MANUAL_CONFIG;

                case 2:
                    //don't ask again
                    store.putValue(key, DONT_ASK_AGAIN_PREFERENCE_VALUE);
                    return INTERPRETER_DONT_ASK_CONFIG;
            }
        }
        return INTERPRETER_CANCEL_CONFIG;
    }

    /**
     * @param abstractInterpreterManager
     */
    public static boolean getAskAgainInterpreter(AbstractInterpreterManager m) {
        String key = "INTERPRETER_CONFIGURATION_" + m.getInterpreterType();
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String val = store.getString(key);
        return !val.equals(DONT_ASK_AGAIN_PREFERENCE_VALUE);
    }
}
