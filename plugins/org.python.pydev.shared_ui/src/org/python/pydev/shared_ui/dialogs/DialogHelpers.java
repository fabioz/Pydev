/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
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

    public static void openInfo(String title, String message) {
        Shell shell = EditorUtils.getShell();
        MessageDialog.openInformation(shell, title, message);
    }

    public static boolean openQuestion(String title, String message) {
        Shell shell = EditorUtils.getShell();
        return MessageDialog.openQuestion(shell, title, message);
    }

    public static String openInputRequest(String title, String message) {
        return openInputRequest(title, message, null);
    }

    public static String openInputRequest(String title, String message, Shell shell) {
        IInputValidator validator = new IInputValidator() {

            @Override
            public String isValid(String newText) {
                if (newText.length() == 0) {
                    return "At least 1 char must be provided.";
                }
                return null;
            }
        };
        return openInputRequest(title, message, shell, validator);
    }

    public static String openInputRequest(String title, String message, Shell shell, IInputValidator validator) {
        if (shell == null) {
            shell = EditorUtils.getShell();
        }
        String initialValue = "";
        InputDialog dialog = new InputDialog(shell, title, message, initialValue, validator);
        dialog.setBlockOnOpen(true);
        if (dialog.open() == Window.OK) {
            return dialog.getValue();
        }
        return null;
    }

    // Return could be null if user cancelled.
    public static Integer openAskInt(String title, String message, int initial) {
        Shell shell = EditorUtils.getShell();
        String initialValue = "" + initial;
        IInputValidator validator = new IInputValidator() {

            @Override
            public String isValid(String newText) {
                if (newText.length() == 0) {
                    return "At least 1 char must be provided.";
                }
                try {
                    Integer.parseInt(newText);
                } catch (Exception e) {
                    return "A number is required.";
                }
                return null;
            }
        };
        InputDialog dialog = new InputDialog(shell, title, message, initialValue, validator);
        dialog.setBlockOnOpen(true);
        if (dialog.open() == Window.OK) {
            return Integer.parseInt(dialog.getValue());
        }
        return null;
    }
}
