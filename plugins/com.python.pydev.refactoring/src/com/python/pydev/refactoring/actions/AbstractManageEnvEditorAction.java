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
import org.python.pydev.ui.dialogs.PyDialogHelpers;

public abstract class AbstractManageEnvEditorAction extends Action implements IOfflineActionWithParameters {

    protected List<String> parameters;
    protected PyEdit edit;

    public AbstractManageEnvEditorAction(PyEdit edit) {
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
            doRun(pythonNature, projectInterpreter);
        } catch (MisconfigurationException | PythonNatureWithoutProjectException e) {
            Log.log(e);
        }
    }

    protected abstract void doRun(IPythonNature pythonNature, IInterpreterInfo projectInterpreter);

}
