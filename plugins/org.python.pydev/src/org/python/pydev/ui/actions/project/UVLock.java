package org.python.pydev.ui.actions.project;

import org.python.pydev.ui.actions.container.PyUVAction;

public class UVLock extends PyUVAction {

    @Override
    protected String getTaskName() {
        return "Locking project dependencies...";
    }

    @Override
    protected String runCommand() {
        return pm.lock();
    }

}
