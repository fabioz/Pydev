package org.python.pydev.shared_ui.debug;

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
        String attribute = process.getAttribute(RelaunchConstants.PYDEV_ADD_RELAUNCH_IPROCESS_ATTR);
        if (!RelaunchConstants.PYDEV_ADD_RELAUNCH_IPROCESS_ATTR_TRUE.equals(attribute)) {
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

    @Override
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

    @Override
    public void activated() {
    }

    @Override
    public void deactivated() {
    }

}
