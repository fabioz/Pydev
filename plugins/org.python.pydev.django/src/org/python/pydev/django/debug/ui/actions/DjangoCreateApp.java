/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.python.pydev.shared_ui.EditorUtils;

public class DjangoCreateApp extends DjangoAction {

    @Override
    public void run(IAction action) {
        IInputValidator validator = new IInputValidator() {

            @Override
            public String isValid(String newText) {
                if (newText.trim().length() == 0) {
                    return "Name cannot be empty";
                }
                return null;
            }
        };
        InputDialog d = new InputDialog(EditorUtils.getShell(), "App name", "Name of the django app to be created", "",
                validator);

        int retCode = d.open();
        if (retCode == InputDialog.OK) {
            createApp(d.getValue().trim());
        }
    }

    private void createApp(String name) {
        try {
            launchDjangoCommand("startapp " + name, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
