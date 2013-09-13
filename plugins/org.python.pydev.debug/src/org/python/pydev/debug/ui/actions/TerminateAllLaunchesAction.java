/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;


public class TerminateAllLaunchesAction extends PyAction implements IUpdate {

    public TerminateAllLaunchesAction() {
        KeySequence binding = KeyBindingHelper
                .getCommandKeyBinding("org.python.pydev.debug.ui.actions.terminateAllLaunchesAction");
        String str = binding != null ? "(" + binding.format() + " when on Pydev editor)" : "(unbinded)";

        this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.TERMINATE_ALL));
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

    public void run(IAction action) {
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

    public void run() {
        run(this);
    }

    public void dispose() {

    }

}
