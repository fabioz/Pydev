/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.InterpreterGeneralPreferences;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.dialogs.DialogHelpers;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.ui.interpreters.AbstractInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterConfigHelpers;

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

    public static boolean openQuestionWithIgnoreToggle(String title, String message, String key) {
        Shell shell = EditorUtils.getShell();
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String val = store.getString(key);
        if (val.trim().length() == 0) {
            val = MessageDialogWithToggle.PROMPT; //Initial value if not specified
        }

        if (!val.equals(MessageDialogWithToggle.ALWAYS)) {
            MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(shell, title, message,
                    "Don't show this message again", false, store,
                    key);
            if (dialog.getReturnCode() != IDialogConstants.YES_ID) {
                return false;
            }
        }
        return true;
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
        String key = InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER + m.getInterpreterType();
        boolean val = store.getBoolean(key);

        if (val) {
            String title = m.getInterpreterUIName() + " not configured";
            String message = "It seems that the " + m.getInterpreterUIName()
                    + " interpreter is not currently configured.\n\nHow do you want to proceed?";
            Shell shell = EditorUtils.getShell();

            String[] dialogButtonLabels = Arrays.copyOf(InterpreterConfigHelpers.CONFIG_NAMES,
                    InterpreterConfigHelpers.NUM_CONFIG_TYPES + 1);
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
        return store.getBoolean(InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER + m.getInterpreterType());
    }

    public static void openException(String title, UnableToFindExecutableException e) {
        ErrorDialog.openError(UIUtils.getActiveShell(), title, e.getMessage(),
                new Status(IStatus.ERROR,
                        PydevPlugin.getPluginID(), e.getMessage(), e));
    }

    public static void showString(String string) {
        RunInUiThread.async(() -> {
            Display disp = Display.getCurrent();
            Shell shell = disp.getActiveShell();
            if (shell == null) {
                shell = new Shell(disp);
            }
            ShowTextDialog showTextDialog = new ShowTextDialog(shell, string);
            showTextDialog.open();
        });
    }

    private static final class ShowTextDialog extends Dialog {

        private String message;

        public ShowTextDialog(Shell shell, String message) {
            super(shell);
            this.message = message;
            setShellStyle(
                    SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX | getDefaultOrientation());
            setBlockOnOpen(true);
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Point getInitialSize() {
            return new Point(800, 600);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite = (Composite) super.createDialogArea(parent);

            GridLayout layout = (GridLayout) composite.getLayout();
            layout.numColumns = 1;
            createText(composite, message, 1);

            return composite;
        }

        private Text createText(Composite composite, String labelMsg, int colSpan) {
            Text text = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            gridData.horizontalSpan = colSpan;
            text.setLayoutData(gridData);
            text.setText(labelMsg);
            return text;
        }
    }
}
