package org.python.pydev.debug.ui;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.ui.actions.PyBreakpointRulerAction;
import org.python.pydev.debug.ui.launching.JythonLaunchShortcut;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.ICallback;

public class DebuggerTestUtils {

    private PyEdit debugEditor;

    /**
     * Maximum number of loops (used with the timeout) 
     */
    private static final int MAX_LOOPS = 300;

    /**
     * Maximum time for each loop in millis
     */
    private static final int STEP_TIMEOUT = 100;

    /**
     * Number of steps in the tests that will have busy loops until some condition is hit.
     */
    private static final int TOTAL_STEPS = 3;

    /**
     * Total time in millis that the test has for finishing 
     */
    public static final int TOTAL_TIME_FOR_TESTS = MAX_LOOPS * STEP_TIMEOUT * (TOTAL_STEPS + 1);

    /**
     * Used for having wait()
     */
    private Object lock = new Object();

    /**
     * An exception that occurred that was thrown and didn't let the tests finish
     */
    public Throwable failException = null;

    public DebuggerTestUtils(PyEdit debugEditor2) {
        this.debugEditor = debugEditor2;
    }

    /**
     * @return an action that can be run to create a breakpoint in the given line
     */
    public PyBreakpointRulerAction createAddBreakPointAction(final int line) {
        PyBreakpointRulerAction ret = new PyBreakpointRulerAction(debugEditor, new IVerticalRulerInfo() {
            public int getLineOfLastMouseButtonActivity() {
                return line;
            }

            public Control getControl() {
                throw new RuntimeException("Not Implemented");
            }

            public int getWidth() {
                throw new RuntimeException("Not Implemented");
            }

            public int toDocumentLineNumber(int y_coordinate) {
                throw new RuntimeException("Not Implemented");
            }
        });
        ret.update();
        return ret;
    }

    /**
     * Creates a run in debug mode for the debug editor
     */
    public void launchEditorInDebug() {
        final IWorkbench workBench = PydevPlugin.getDefault().getWorkbench();
        Display display = workBench.getDisplay();

        // Make sure to run the UI thread.
        display.syncExec(new Runnable() {
            public void run() {
                JythonLaunchShortcut launchShortcut = new JythonLaunchShortcut();
                launchShortcut.launch(debugEditor, "debug");
            }
        });
    }

    /**
     * Waits until some thread is suspended.
     */
    protected IThread waitForSuspendedThread(final PyDebugTarget target) throws Throwable {
        final IThread[] ret = new IThread[1];

        waitForCondition(new ICallback() {

            public Object call(Object args) throws Exception {
                IThread[] threads = target.getThreads();
                for (IThread thread : threads) {
                    if (thread.isSuspended()) {
                        ret[0] = thread;
                        return true;
                    }
                }
                return false;
            }
        }, "waitForSuspendedThread");

        return ret[0];
    }

    /**
     * Waits until a launch becomes available 
     * @return the launch that was found
     */
    public ILaunch waitForLaunchAvailable() throws Throwable {
        final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        waitForCondition(new ICallback() {

            public Object call(Object args) throws Exception {
                ILaunch[] launches = launchManager.getLaunches();
                return launches.length > 0;
            }
        }, "waitForLaunchAvailable");
        return launchManager.getLaunches()[0];
    }

    /**
     * Waits until a debug target is available in the passed launch
     * @return the debug target found
     */
    public IDebugTarget waitForDebugTargetAvailable(final ILaunch launch) throws Throwable {
        waitForCondition(new ICallback() {

            public Object call(Object args) throws Exception {
                return launch.getDebugTarget() != null;
            }
        }, "waitForDebugTargetAvailable");

        return launch.getDebugTarget();
    }

    /**
     * Keeps on a busy loop with a timeout until the given callback returns true (otherwise, an
     * exception is thrown when the total time is elapsed).
     */
    public void waitForCondition(ICallback callback, String errorMessage) throws Throwable {
        if (failException != null) {
            throw failException;
        }

        int loops = MAX_LOOPS;
        for (int i = 0; i < loops; i++) {
            if ((Boolean) callback.call(new Object[] {})) {
                return;
            }
            synchronized (lock) {
                try {
                    Thread.yield();
                    lock.wait(STEP_TIMEOUT);
                } catch (InterruptedException e) {
                }
            }
        }
        throw new AssertionError("Unable to get to condition after " + (loops * STEP_TIMEOUT) / 1000
                + " seconds.\nMessage: "
                + errorMessage);
    }

    /**
     * This method can be used to switch to a given perspective
     * @param perspectiveId the id of the perspective that should be activated.
     * @return the exception raised or null.
     */
    public void switchToPerspective(final String perspectiveId) {
        final IWorkbench workBench = PydevPlugin.getDefault().getWorkbench();
        Display display = workBench.getDisplay();

        // Make sure to run the UI thread.
        display.syncExec(new Runnable() {

            public void run() {
                IWorkbenchWindow window = workBench.getActiveWorkbenchWindow();
                try {
                    workBench.showPerspective(perspectiveId, window);
                } catch (WorkbenchException e) {
                    failException = e;
                }
            }
        });
    }

}
