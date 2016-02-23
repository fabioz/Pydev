/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.dialogs.TreeSelectionDialog;
import org.python.pydev.ui.dialogs.SelectExistingOrCreateNewDialog;

/**
 * Command to execute a custom (not predefined) django action.
 */
public class DjangoCustomCommand extends DjangoAction {

    private static final String SHELL_MEMENTO_ID = "org.python.pydev.django.debug.ui.actions.DjangoCustomCommand.shell";
    private static final String DJANGO_CUSTOM_COMMANDS_PREFERENCE_KEY = "DJANGO_CUSTOM_COMMANDS";

    @Override
    public void run(IAction action) {
        try {

            String command = chooseCommand();
            if (command != null) {
                launchDjangoCommand(command, true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens a dialog so that the user can enter a command to be executed.
     * 
     * @return the command to be executed or null if no command was selected for execution.
     */
    private String chooseCommand() {
        final IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();

        TreeSelectionDialog dialog = new SelectExistingOrCreateNewDialog(EditorUtils.getShell(), preferenceStore,
                DJANGO_CUSTOM_COMMANDS_PREFERENCE_KEY, SHELL_MEMENTO_ID);

        dialog.setTitle("Select the command to run or enter a new command");
        dialog.setMessage("Select the command to run or enter a new command");
        dialog.setInitialFilter("");

        int open = dialog.open();
        if (open != Window.OK) {
            return null;
        }
        Object[] result = dialog.getResult();
        if (result != null && result.length == 1) {
            return result[0].toString();
        }
        return null;
    }
}
