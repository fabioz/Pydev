package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.python.pydev.plugin.PydevPlugin;

public class TerminateAllLaunches implements IEditorActionDelegate {

    public void run(IAction action) {
        Job job = new Job("Terminate all Launches") {
            
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
                for (ILaunch iLaunch : launches) {
                    try {
                        iLaunch.terminate();
                    } catch (DebugException e) {
                        PydevPlugin.log(e);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    public void selectionChanged(IAction action, ISelection selection) {

    }

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {

    }

}
