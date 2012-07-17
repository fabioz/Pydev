/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import java.io.ByteArrayInputStream;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.ui.actions.PyBreakpointRulerAction;
import org.python.pydev.debug.ui.launching.JythonLaunchShortcut;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.ICallback;

public class DebuggerTestWorkbench extends AbstractWorkbenchTestCase {

    /**
     * File used to debug
     */
    private IFile debugFile;

    /**
     * The editor that'll be created on debug
     */
    private PyEdit debugEditor;

    /**
     * Maximum number of loops (used with the timeout) 
     */
    private final int MAX_LOOPS = 300;

    /**
     * Maximum time for each loop in millis
     */
    private final int STEP_TIMEOUT = 100;

    /**
     * Number of steps in the tests that will have busy loops until some condition is hit.
     */
    private final int TOTAL_STEPS = 3;

    /**
     * Total time in millis that the test has for finishing 
     */
    private final int TOTAL_TIME_FOR_TESTS = MAX_LOOPS * STEP_TIMEOUT * (TOTAL_STEPS + 1);

    /**
     * Used for having wait()
     */
    private Object lock = new Object();

    /**
     * An exception that occurred that was thrown and didn't let the tests finish
     */
    private Throwable failException = null;

    /**
     * Only true when the test finishes without exceptions.
     */
    private boolean finished = false;

    private String currentStep = "<unspecified>";

    /**
     * Creates the debug file and editor.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        debugFile = initFile.getParent().getFile(new Path("debug_file.py"));
        String mod1Contents = "from pack1.pack2 import mod1\nprint mod1\nprint 'now'\n";
        debugFile.create(new ByteArrayInputStream(mod1Contents.getBytes()), true, null);
        debugFile.refreshLocal(IResource.DEPTH_ZERO, null);

        debugEditor = (PyEdit) PyOpenEditor.doOpenEditor(debugFile);
    }

    /**
     * Removes the debug file and closes the debug editor
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (debugFile != null) {
            debugFile.delete(true, null);
        }
        if (debugEditor != null) {
            debugEditor.close(false);
        }
    }

    /**
     * In this test, a thread is started and then we wait on a busy loop until the thread finishes with the tests.
     */
    public void testDebugger() throws Exception {

        //start the thread that'll do the test
        threadTest.start();

        //wait on a busy loop until the test is finished or an exception is thrown.
        goToManual(TOTAL_TIME_FOR_TESTS, new org.python.pydev.core.callbacks.ICallback<Boolean, Object>() {

            public Boolean call(Object arg) {
                return finished || failException != null;
            }
        });

        //Make it fail if we encountered some problem
        if (failException != null) {
            failException.printStackTrace();
            fail("Current Step: " + currentStep + "\n" + failException.getMessage());
        }
        if (!finished) {
            if (failException == null) {
                fail("Current Step: " + currentStep + "\nThe test didn't finish in the available time: "
                        + TOTAL_TIME_FOR_TESTS / 1000 + " secs.");
            }
        }
    }

    /**
     * This is the thread that'll make the test.
     */
    Thread threadTest = new Thread() {
        @Override
        public void run() {
            try {
                currentStep = "launchEditorInDebug";
                //make a launch for debugging 
                launchEditorInDebug();

                //switch to debug perspective, because otherwise, when we hit a breakpoint it'll ask if we want to show it.
                switchToPerspective("org.eclipse.debug.ui.DebugPerspective");
                PyBreakpointRulerAction createAddBreakPointAction = createAddBreakPointAction(1);
                createAddBreakPointAction.run();

                currentStep = "waitForLaunchAvailable";
                ILaunch launch = waitForLaunchAvailable();
                PyDebugTarget target = (PyDebugTarget) waitForDebugTargetAvailable(launch);

                currentStep = "waitForSuspendedThread";
                IThread suspendedThread = waitForSuspendedThread(target);
                assertTrue(suspendedThread.getName().startsWith("MainThread"));
                IStackFrame topStackFrame = suspendedThread.getTopStackFrame();
                assertTrue("Was not expecting: " + topStackFrame.getName(),
                        topStackFrame.getName().indexOf("debug_file.py:2") != 0);
                IVariable[] variables = topStackFrame.getVariables();

                HashSet<String> varNames = new HashSet<String>();
                for (IVariable variable : variables) {
                    PyVariable var = (PyVariable) variable;
                    varNames.add(var.getName());
                }
                HashSet<String> expected = new HashSet<String>();
                expected.add("Globals");
                expected.add("__doc__");
                expected.add("__file__");
                expected.add("__name__");
                expected.add("mod1");
                assertEquals(expected, varNames);

                assertTrue(target.canTerminate());
                target.terminate();

                finished = true;
            } catch (Throwable e) {
                failException = e;
            }
        }

    };

    /**
     * Creates a run in debug mode for the debug editor
     */
    private void launchEditorInDebug() {
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
     * @return an action that can be run to create a breakpoint in the given line
     */
    private PyBreakpointRulerAction createAddBreakPointAction(final int line) {
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
     * This method can be used to switch to a given perspective
     * @param perspectiveId the id of the perspective that should be activated.
     */
    protected void switchToPerspective(final String perspectiveId) {
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
    private ILaunch waitForLaunchAvailable() throws Throwable {
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
    private IDebugTarget waitForDebugTargetAvailable(final ILaunch launch) throws Throwable {
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
    private void waitForCondition(ICallback callback, String errorMessage) throws Throwable {
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
        fail("Unable to get to condition after " + (loops * STEP_TIMEOUT) / 1000 + " seconds.\nMessage: "
                + errorMessage);
    }

}
