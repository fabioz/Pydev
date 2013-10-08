package org.python.pydev.shared_ui.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;

public class AsynchronousProgressMonitorWrapper extends ProgressMonitorWrapper {

    private long lastChange;

    public AsynchronousProgressMonitorWrapper(IProgressMonitor monitor) {
        super(monitor);
    }

    @Override
    public void setTaskName(String name) {
        long curr = System.currentTimeMillis();
        if (curr - lastChange > AsynchronousProgressMonitorDialog.UPDATE_INTERVAL_MS) {
            this.lastChange = curr;
            super.setTaskName(name);
        }
    }
}
