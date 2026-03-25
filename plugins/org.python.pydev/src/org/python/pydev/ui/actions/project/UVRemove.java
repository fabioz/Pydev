/**
 * 
 */
package org.python.pydev.ui.actions.project;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.ui.actions.container.PyUVAction;

/**
 * 
 */
public class UVRemove extends PyUVAction {

    @Override
    protected String getTaskName() {
        return "Installing python package...";
    }

    @Override
    protected String runCommand() {
        InputDialog dialog = new InputDialog(EditorUtils.getShell(), "App to remove",
                "Name of the python App to remove", null, null);
        int open = dialog.open();
        if (open != Window.OK) {
            return "";
        }
        String appName = dialog.getValue();
        return pm.remove(appName);
    }

}
