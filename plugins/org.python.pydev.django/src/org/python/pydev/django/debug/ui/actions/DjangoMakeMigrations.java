package org.python.pydev.django.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.python.pydev.shared_ui.EditorUtils;

public class DjangoMakeMigrations extends DjangoAction {

    @Override
    public void run(IAction action) {
        IInputValidator validator = new IInputValidator() {

            public String isValid(String newText) {
                if (newText.trim().length() == 0) {
                    return "Name cannot be empty";
                }
                return null;
            }
        };
        InputDialog d = new InputDialog(EditorUtils.getShell(), "App name",
                "Name of the django app to makemigrations on", "",
                validator);

        int retCode = d.open();
        if (retCode == InputDialog.OK) {
            createApp(d.getValue().trim());
        }
    }

    private void createApp(String name) {
        try {
            launchDjangoCommand("makemigrations " + name, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}