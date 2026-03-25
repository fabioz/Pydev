package org.python.pydev.ui.actions.project;

import org.python.pydev.ui.actions.container.PyPoetryAction;

public class PoetrySync extends PyPoetryAction {

    @Override
    protected String getTaskName() {
        return "Syncronising project dependencies...";
    }

    @Override
    protected String runCommand() {
        return pm.sync();
    }

}
