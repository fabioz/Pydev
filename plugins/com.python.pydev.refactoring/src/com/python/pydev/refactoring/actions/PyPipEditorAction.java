package com.python.pydev.refactoring.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.IOfflineActionWithParameters;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.package_manager.PipPackageManager;

public class PyPipEditorAction extends Action implements IOfflineActionWithParameters {

    private List<String> parameters;
    private PyEdit edit;

    public PyPipEditorAction(PyEdit edit) {
        this.edit = edit;
    }

    @Override
    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void run() {
        try {
            final IPythonNature pythonNature = edit.getPythonNature();
            if (pythonNature == null) {
                PyDialogHelpers.openCritical("Unable to execute pip",
                        "The related editor does not have an associated python nature.");
                return;
            }
            IInterpreterInfo projectInterpreter = pythonNature.getProjectInterpreter();
            if (projectInterpreter == null) {
                PyDialogHelpers.openCritical("Unable to execute pip",
                        "The related editor does not have an associated interpreter.");
                return;
            }
            PipPackageManager manager = new PipPackageManager(projectInterpreter);

            if (parameters != null && parameters.size() > 0) {
                manager.manage(new String[] { StringUtils.join(" ", parameters) }, true);
            } else {
                manager.manage();
            }
        } catch (MisconfigurationException | PythonNatureWithoutProjectException e) {
            Log.log(e);
        }
    }
}
