package org.python.pydev.shared_ui.debug;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.actions.BaseAction;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;

public class TerminateAllLaunchesAction extends BaseAction implements IUpdate {

    public TerminateAllLaunchesAction() {
        KeySequence binding = KeyBindingHelper
                .getCommandKeyBinding("org.python.pydev.debug.ui.actions.terminateAllLaunchesAction");
        String str = binding != null ? "(" + binding.format() + " with focus on editor)" : "(unbinded)";

        this.setImageDescriptor(SharedUiPlugin.getImageCache().getDescriptor(UIConstants.TERMINATE_ALL));
        this.setToolTipText("Terminate ALL." + str);

        update();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.IUpdate#update()
     */
    public void update() {
        ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
        try {
            for (ILaunch iLaunch : launches) {
                if (!iLaunch.isTerminated()) {
                    setEnabled(true);
                    return;
                }
            }
            setEnabled(false);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    @Override
    public void run(IAction action) {
        terminateAllLaunches();
    }

    public static void terminateAllLaunches() {
        Job job = new Job("Terminate all Launches") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
                for (ILaunch iLaunch : launches) {
                    try {
                        if (!iLaunch.isTerminated()) {
                            iLaunch.terminate();
                        }
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    @Override
    public void run() {
        run(this);
    }

    public void dispose() {

    }

}
