/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.ui.actions.PyBreakpointRulerAction;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;

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
     * Only true when the test finishes without exceptions.
     */
    private boolean finished = false;

    private String currentStep = "<unspecified>";

    private DebuggerTestUtils debuggerTestUtils;

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

        debuggerTestUtils = new DebuggerTestUtils(debugEditor);
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
        goToManual(DebuggerTestUtils.TOTAL_TIME_FOR_TESTS,
                new org.python.pydev.shared_core.callbacks.ICallback<Boolean, Object>() {

                    public Boolean call(Object arg) {
                        return finished || debuggerTestUtils.failException != null;
                    }
                });

        //Make it fail if we encountered some problem
        if (debuggerTestUtils.failException != null) {
            debuggerTestUtils.failException.printStackTrace();
            fail("Current Step: " + currentStep + "\n" + debuggerTestUtils.failException.getMessage());
        }
        if (!finished) {
            if (debuggerTestUtils.failException == null) {
                fail("Current Step: " + currentStep + "\nThe test didn't finish in the available time: "
                        + DebuggerTestUtils.TOTAL_TIME_FOR_TESTS / 1000 + " secs.");
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
                debuggerTestUtils.launchEditorInDebug();

                //switch to debug perspective, because otherwise, when we hit a breakpoint it'll ask if we want to show it.
                debuggerTestUtils.switchToPerspective("org.eclipse.debug.ui.DebugPerspective");
                PyBreakpointRulerAction createAddBreakPointAction = debuggerTestUtils.createAddBreakPointAction(
                        1);
                createAddBreakPointAction.run();

                currentStep = "waitForLaunchAvailable";
                ILaunch launch = debuggerTestUtils.waitForLaunchAvailable();
                PyDebugTarget target = (PyDebugTarget) debuggerTestUtils.waitForDebugTargetAvailable(launch);

                currentStep = "waitForSuspendedThread";
                IThread suspendedThread = debuggerTestUtils.waitForSuspendedThread(target);
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
                debuggerTestUtils.failException = e;
            }
        }
    };

}
