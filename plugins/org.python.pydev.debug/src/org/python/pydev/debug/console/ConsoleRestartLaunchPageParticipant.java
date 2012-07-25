/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.console;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.actions.RestartLaunchAction;
import org.python.pydev.debug.ui.actions.TerminateAllLaunchesAction;

/**
 * Reference: ProcessConsolePageParticipant
 */
@SuppressWarnings("restriction")
public class ConsoleRestartLaunchPageParticipant implements IConsolePageParticipant, IDebugEventSetListener {

    private RestartLaunchAction restartLaunchAction;
    private TerminateAllLaunchesAction terminateAllLaunchesAction;
    private ProcessConsole fConsole;

    public void init(IPageBookViewPage page, IConsole console) {
        if (!(console instanceof ProcessConsole)) {
            return;
        }
        ProcessConsole processConsole = (ProcessConsole) console;
        IProcess process = processConsole.getProcess();
        if (process == null) {
            return;
        }
        String attribute = process.getAttribute(Constants.PYDEV_ADD_RELAUNCH_IPROCESS_ATTR);
        if (!Constants.PYDEV_ADD_RELAUNCH_IPROCESS_ATTR_TRUE.equals(attribute)) {
            //Only provide relaunch if specified
            return;
        }
        this.fConsole = processConsole;
        DebugPlugin.getDefault().addDebugEventListener(this);

        IActionBars bars = page.getSite().getActionBars();

        IToolBarManager toolbarManager = bars.getToolBarManager();

        restartLaunchAction = new RestartLaunchAction(page, processConsole);
        terminateAllLaunchesAction = new TerminateAllLaunchesAction();

        toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, restartLaunchAction);
        toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAllLaunchesAction);

        bars.updateActionBars();

    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
     */
    public void handleDebugEvents(DebugEvent[] events) {
        for (int i = 0; i < events.length; i++) {
            DebugEvent event = events[i];
            if (event.getSource().equals(getProcess())) {
                Runnable r = new Runnable() {
                    public void run() {
                        if (restartLaunchAction != null) {
                            restartLaunchAction.update();
                        }
                        if (terminateAllLaunchesAction != null) {
                            terminateAllLaunchesAction.update();
                        }
                    }
                };

                DebugUIPlugin.getStandardDisplay().asyncExec(r);
            }
        }
    }

    protected IProcess getProcess() {
        return fConsole != null ? fConsole.getProcess() : null;
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

    public void dispose() {
        DebugPlugin.getDefault().removeDebugEventListener(this);
        if (restartLaunchAction != null) {
            restartLaunchAction.dispose();
            restartLaunchAction = null;
        }
        if (terminateAllLaunchesAction != null) {
            terminateAllLaunchesAction.dispose();
            terminateAllLaunchesAction = null;
        }
    }

    public void activated() {
    }

    public void deactivated() {
    }

}
