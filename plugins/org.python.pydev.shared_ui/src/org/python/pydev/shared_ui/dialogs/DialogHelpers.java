package org.python.pydev.shared_ui.dialogs;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.shared_ui.EditorUtils;

public class DialogHelpers {

    public static void openWarning(String title, String message) {
        Shell shell = EditorUtils.getShell();
        MessageDialog.openWarning(shell, title, message);
    }

    public static void openCritical(String title, String message) {
        Shell shell = EditorUtils.getShell();
        MessageDialog.openError(shell, title, message);
    }

    public static boolean openQuestion(String title, String message) {
        Shell shell = EditorUtils.getShell();
        return MessageDialog.openQuestion(shell, title, message);
    }

    public static String openInputRequest(String title, String message) {
        Shell shell = EditorUtils.getShell();
        String initialValue = "";
        IInputValidator validator = new IInputValidator() {

            @Override
            public String isValid(String newText) {
                if (newText.length() == 0) {
                    return "At least 1 char must be provided.";
                }
                return null;
            }
        };
        InputDialog dialog = new InputDialog(shell, title, message, initialValue, validator);
        dialog.setBlockOnOpen(true);
        if (dialog.open() == Window.OK) {
            return dialog.getValue();
        }
        return null;
    }
}
