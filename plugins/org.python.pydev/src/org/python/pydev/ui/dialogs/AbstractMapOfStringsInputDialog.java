/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import java.util.Map;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractMapOfStringsInputDialog extends AbstractKeyValueDialog {

    private Map<String, String> map;

    public AbstractMapOfStringsInputDialog(Shell shell, String dialogTitle, String dialogMessage,
            Map<String, String> map) {
        super(shell, dialogTitle, dialogMessage);
        this.map = map;
    }

    @Override
    protected String getInitialMessage() {
        return "Please, supply the name and value for the variable";
    }

    @Override
    protected String getValueLabelText() {
        return "Value: ";
    }

    @Override
    protected String getKeyLabelText() {
        return "Name: ";
    }

    protected abstract boolean isExistingKeyEdit();

    /**
     * @return a listened that should clear or set the error message after any change.
     */
    @Override
    protected Listener createChangesValidator() {
        return new Listener() {
            @Override
            public void handleEvent(Event event) {

                String errorMessage = null;

                String key = keyField.getText().trim();
                if (key.equals("")) {
                    errorMessage = "The variable name must be specified";
                }

                String value = valueField.getText().trim();
                if (errorMessage == null && value.equals("")) {
                    errorMessage = "The value must be specified";
                }
                if (errorMessage == null && !isExistingKeyEdit()) {
                    if (map.containsKey(key)) {
                        errorMessage = "The key: " + key + " is already specified.";
                    }
                }
                setErrorMessage(errorMessage);
            }
        };
    }

}
