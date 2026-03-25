package org.python.pydev.ui.actions.project;

import org.python.pydev.ui.actions.container.PyUVAction;

public class UVSync extends PyUVAction {

    @Override
    protected String getTaskName() {
        return "Syncronising project dependencies...";
    }

    @Override
    protected String runCommand() {
        return pm.sync();
    }

}
