/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/**
 * 
 */
package org.python.pydev.ui.dialogs;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterEditor;

/**
 * @author raul
 * @author fabioz
 */
public class InterpreterInputDialog extends AbstractKeyValueDialog {

    private AbstractInterpreterEditor editor;

    /**
     * @param shell the shell.
     * @param dialogTitle the title of the dialog.
     * @param dialogMessage the message of the dialog.
     */
    public InterpreterInputDialog(Shell shell, String dialogTitle, String dialogMessage,
            AbstractInterpreterEditor editor) {
        super(shell, dialogTitle, dialogMessage);
        this.editor = editor;
    }

    protected String getInitialMessage() {
        return "Please supply a name and executable for your interpreter";
    }

    protected String getValueLabelText() {
        return "Interpreter Executable: ";
    }

    protected String getKeyLabelText() {
        return "Interpreter Name: ";
    }

    /**
     * @return a listened that should clear or set the error message after any change.
     */
    protected Listener createChangesValidator() {
        return new Listener() {
            public void handleEvent(Event event) {

                String errorMessage = null;

                String interpreterName = keyField.getText().trim();
                if (interpreterName.equals("")) {
                    errorMessage = "The interpreter name must be specified";
                }

                String executableOrJar = valueField.getText().trim();
                if (errorMessage == null && executableOrJar.equals("")) {
                    errorMessage = "The interpreter location must be specified";
                }
                if (errorMessage == null) {
                    File file = new File(executableOrJar);
                    if (!file.exists() || file.isDirectory()) {
                        errorMessage = "Invalid interpreter";
                    }
                }
                if (errorMessage == null) {
                    errorMessage = editor.getDuplicatedMessageError(interpreterName, executableOrJar);
                }
                setErrorMessage(errorMessage);
            }
        };
    }

    /**
     * Overridden because we want the value to be always a full path with links resolved.
     */
    @Override
    public Tuple<String, String> getKeyAndValueEntered() {
        Tuple<String, String> keyAndValueEntered = super.getKeyAndValueEntered();
        if (keyAndValueEntered != null) {
            keyAndValueEntered.o2 = REF.getFileAbsolutePath(finalValueValue);
        }
        return keyAndValueEntered;
    }

    protected String handleBrowseButton() {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);

        String[] filterExtensions = editor.getInterpreterFilterExtensions();
        if (filterExtensions != null) {
            dialog.setFilterExtensions(filterExtensions);
        }

        String file = dialog.open();
        return file;
    }

    protected void setValueField(String file) {
        if (keyField.getText().trim().equals("")) {
            keyField.setText(file);
        }
        super.setValueField(file);
    }

}