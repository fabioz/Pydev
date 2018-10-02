package com.python.pydev.refactoring.actions;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.pythonpathconf.package_manager.PipenvPackageManager;
import org.python.pydev.ui.pythonpathconf.package_manager.PipenvUnconfiguredException;

public class PyPipenvEditorAction extends AbstractManageEnvEditorAction {

    public PyPipenvEditorAction(PyEdit edit) {
        super(edit);
    }

    @Override
    protected void doRun(IPythonNature pythonNature, IInterpreterInfo projectInterpreter) {
        PipenvPackageManager manager;
        try {
            manager = new PipenvPackageManager(projectInterpreter,
                    pythonNature.getRelatedInterpreterManager());
        } catch (PipenvUnconfiguredException e) {
            return; // Ignore
        }

        if (parameters != null && parameters.size() > 0) {
            manager.manage(new String[] { StringUtils.join(" ", parameters) }, true);
        } else {
            manager.manage();
        }
    }
}
