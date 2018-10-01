package com.python.pydev.refactoring.actions;

import java.io.File;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.package_manager.CondaPackageManager;

public class CondaEditorAction extends AbstractManageEnvEditorAction {

    public CondaEditorAction(PyEdit edit) {
        super(edit);
    }

    @Override
    protected void doRun(IPythonNature pythonNature, IInterpreterInfo projectInterpreter) {
        File condaPrefix = projectInterpreter.getCondaPrefix();
        if (condaPrefix == null) {
            PyDialogHelpers.openCritical("Unable to manage with conda",
                    "Unable to get conda prefix for the interpreter related to the given project.");
            return;
        }
        CondaPackageManager manager = new CondaPackageManager(projectInterpreter, condaPrefix);

        if (parameters != null && parameters.size() > 0) {
            manager.manage(new String[] { StringUtils.join(" ", parameters) }, true,
                    pythonNature.getProject().getLocation().toFile());
        } else {
            manager.manage();
        }
    }
}
