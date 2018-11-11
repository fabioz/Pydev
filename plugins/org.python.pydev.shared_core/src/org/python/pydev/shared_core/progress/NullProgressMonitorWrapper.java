package org.python.pydev.shared_core.progress;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Helper class to monitor the cancel state of another monitor.
 */
public class NullProgressMonitorWrapper extends NullProgressMonitor {

    private IProgressMonitor wrap;

    public NullProgressMonitorWrapper(IProgressMonitor monitor) {
        this.wrap = monitor;
    }

    @Override
    public boolean isCanceled() {
        return super.isCanceled() || this.wrap.isCanceled();
    }

}