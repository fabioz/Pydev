/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.dialogs.DialogHelpers;
import org.python.pydev.ui.interpreters.AbstractInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterConfigHelpers;
import org.python.pydev.ui.pythonpathconf.InterpreterGeneralPreferencesPage;

/**
 * @author fabioz
 *
 */
public class PyDialogHelpers {

    public static void openWarning(String title, String message) {
        DialogHelpers.openWarning(title, message);
    }

    public static void openCritical(String title, String message) {
        DialogHelpers.openCritical(title, message);
    }

    public static boolean openQuestion(String title, String message) {
        return DialogHelpers.openQuestion(title, message);
    }

    public static Integer openAskInt(String title, String message, int initial) {
        return DialogHelpers.openAskInt(title, message, initial);
    }

    public static int openWarningWithIgnoreToggle(String title, String message, String key) {
        Shell shell = EditorUtils.getShell();
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
        Shell shell = EditorUtils.getShell();
        MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.ERROR, choices, 0);
        return dialog.open();
    }

    public final static int INTERPRETER_CANCEL_CONFIG = -1;

    private static MessageDialog dialog = null;
    private static int enableAskInterpreter = 0;

    /**
     * Use this to disable/try to enable displaying a "configure interpreter" dialog when an interpreter
     * cannot be found. Disabling it is useful for when it shouldn't be displayed on top of exisitng dialogs.
     * @param enable Set to <code>false</code> to disable the dialogs from appearing, or <code>true</code>
     * to try to re-enable them. The dialogs will only be enabled once all "disable" calls have been negated
     * by an equal number of "enable" calls.
     */
    public static void enableAskInterpreterStep(boolean enable) {
        enableAskInterpreter = Math.min(enableAskInterpreter + (enable ? 1 : -1), 0);
        if (enableAskInterpreter < 0 && dialog != null) {
            dialog.close();
            dialog = null;
        }
    }

    public static int openQuestionConfigureInterpreter(AbstractInterpreterManager m) {
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String key = InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER + m.getInterpreterType();
        boolean val = store.getBoolean(key);

        if (val) {
            String title = m.getInterpreterUIName() + " not configured";
            String message = "It seems that the " + m.getInterpreterUIName()
                    + " interpreter is not currently configured.\n\nHow do you want to proceed?";
            Shell shell = EditorUtils.getShell();

            String[] dialogButtonLabels = new String[InterpreterConfigHelpers.NUM_CONFIG_TYPES + 1];
            for (int i = 0; i < InterpreterConfigHelpers.CONFIG_NAMES.length; i++) {
                dialogButtonLabels[i] = InterpreterConfigHelpers.CONFIG_NAMES[i];
            }
            dialogButtonLabels[dialogButtonLabels.length - 1] = "Don't ask again";

            dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION,
                    dialogButtonLabels, 0);
            int open = dialog.open();

            //If dialog is null now, it was forcibly closed by a "disable" call of enableAskInterpreterStep.
            if (dialog != null) {
                dialog = null;
                // "Don't ask again" button is the final button in the list
                if (open == dialogButtonLabels.length - 1) {
                    store.setValue(key, false);
                    return INTERPRETER_CANCEL_CONFIG;
                }
                return open;
            }
        }
        return INTERPRETER_CANCEL_CONFIG;
    }

    /**
     * @param abstractInterpreterManager
     */
    public static boolean getAskAgainInterpreter(AbstractInterpreterManager m) {
        if (enableAskInterpreter < 0) {
            return false;
        }
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        return store.getBoolean(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER + m.getInterpreterType());
    }
}
