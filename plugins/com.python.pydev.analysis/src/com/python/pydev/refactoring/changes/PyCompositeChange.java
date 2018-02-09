package com.python.pydev.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.python.pydev.core.concurrency.RunnableAsJobsPoolThread;

public final class PyCompositeChange extends CompositeChange {
    private boolean makeUndo;

    public PyCompositeChange(String name, boolean makeUndo) {
        super(name);
        this.makeUndo = makeUndo;
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
        RunnableAsJobsPoolThread.getSingleton().pushStopThreads();
        Change ret;
        try {
            ret = super.perform(pm);
        } finally {
            RunnableAsJobsPoolThread.getSingleton().popStopThreads();
        }
        if (makeUndo) {
            return ret;
        }
        return null;
    }
}