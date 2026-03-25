package org.python.pydev.ui.actions.project;

import org.python.pydev.ui.actions.container.PyPoetryAction;

public class PoetryLock extends PyPoetryAction {

    @Override
    protected String getTaskName() {
        return "Locking project dependencies...";
    }

    @Override
    protected String runCommand() {
        return pm.lock();
    }

}
