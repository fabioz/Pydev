package com.python.pydev.refactoring.actions;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.pythonpathconf.package_manager.PipPackageManager;

public class PyPipEditorAction extends AbstractManageEnvEditorAction {

    public PyPipEditorAction(PyEdit edit) {
        super(edit);
    }

    @Override
    protected void doRun(IPythonNature pythonNature, IInterpreterInfo projectInterpreter) {
        PipPackageManager manager = new PipPackageManager(projectInterpreter);

        if (parameters != null && parameters.size() > 0) {
            manager.manage(new String[] { StringUtils.join(" ", parameters) }, true);
        } else {
            manager.manage();
        }
    }
}
