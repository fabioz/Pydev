/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 21, 2004
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.RunToLineCommand;
import org.python.pydev.debug.model.remote.SetNextCommand;
import org.python.pydev.debug.model.remote.StepCommand;
import org.python.pydev.debug.model.remote.ThreadRunCommand;
import org.python.pydev.debug.model.remote.ThreadSuspendCommand;

/**
 * Represents python threads.
 * Stack global variables are associated with threads.
 */
public class PyThread extends PlatformObject implements IThread {

    private AbstractDebugTarget target;
    private String name;
    private String id;

    /**
     * true if this is a debugger thread, that can't be killed/suspended
     */
    private final boolean isPydevThread;

    /**
     * A custom frame is one that's added programmatically (such as a tasklet).
     */
    public final boolean isCustomFrame;

    private boolean isSuspended = false;
    private boolean isStepping = false;
    private IStackFrame[] stack;

    public PyThread(AbstractDebugTarget target, String name, String id) {
        this.target = target;
        this.name = name;
        this.id = id;
        isPydevThread = id.equals("-1"); // use a special id for pydev threads
        isCustomFrame = id.startsWith("__frame__:");
    }

    /**
     * If a thread is entering a suspended state, pass in the stack
     */
    public void setSuspended(boolean state, IStackFrame[] stack) {
        isSuspended = state;
        if (stack != null) {
            // Only save the stack when it's paused (otherwise, it should be null, but we
            // don't want to reset it because we want to reuse the stack later on so that
            // the expanded state in the tree is properly kept).
            this.stack = stack;
        }
    }

    @Override
    public String getName() throws DebugException {
        return name + " - " + getId();
    }

    public String getId() {
        return id;
    }

    public boolean isPydevThread() {
        return isPydevThread;
    }

    @Override
    public int getPriority() throws DebugException {
        return 0;
    }

    @Override
    public String getModelIdentifier() {
        return target.getModelIdentifier();
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return target;
    }

    @Override
    public ILaunch getLaunch() {
        return target.getLaunch();
    }

    @Override
    public boolean canTerminate() {
        return !isPydevThread && !isTerminated();
    }

    @Override
    public boolean isTerminated() {
        return target.isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        target.terminate();
    }

    @Override
    public boolean canResume() {
        return !isPydevThread && isSuspended && !isTerminated() && !isCustomFrame;
    }

    @Override
    public boolean canSuspend() {
        return !isPydevThread && !isSuspended && !isTerminated() && !isCustomFrame;
    }

    @Override
    public boolean isSuspended() {
        return isSuspended;
    }

    @Override
    public void resume() throws DebugException {
        if (!isPydevThread) {
            isStepping = false;
            target.postCommand(new ThreadRunCommand(target, id));
        }
    }

    @Override
    public void suspend() throws DebugException {
        if (!isPydevThread) {
            target.postCommand(new ThreadSuspendCommand(target, id));
        }
    }

    @Override
    public boolean canStepInto() {
        return canResume();
    }

    @Override
    public boolean canStepOver() {
        return canResume();
    }

    @Override
    public boolean canStepReturn() {
        return canResume();
    }

    @Override
    public boolean isStepping() {
        return isStepping;
    }

    @Override
    public void stepInto() throws DebugException {
        if (!isPydevThread) {
            isStepping = true;
            target.postCommand(new StepCommand(target, AbstractDebuggerCommand.CMD_STEP_INTO, id));
        }
    }

    @Override
    public void stepOver() throws DebugException {
        if (!isPydevThread) {
            isStepping = true;
            target.postCommand(new StepCommand(target, AbstractDebuggerCommand.CMD_STEP_OVER, id));
        }
    }

    @Override
    public void stepReturn() throws DebugException {
        if (!isPydevThread) {
            isStepping = true;
            target.postCommand(new StepCommand(target, AbstractDebuggerCommand.CMD_STEP_RETURN, id));
        }
    }

    public void runToLine(int line, String funcName) {
        isStepping = true;
        target.postCommand(new RunToLineCommand(target, AbstractDebuggerCommand.CMD_RUN_TO_LINE, id, line, funcName));
    }

    public void setNextStatement(int line, String funcName) {
        isStepping = true;
        target.postCommand(new SetNextCommand(target, AbstractDebuggerCommand.CMD_SET_NEXT_STATEMENT, id, line,
                funcName));
    }

    @Override
    public IStackFrame[] getStackFrames() throws DebugException {
        if (isSuspended && stack != null) {
            return stack;
        }
        return new IStackFrame[0];
    }

    @Override
    public boolean hasStackFrames() throws DebugException {
        return (stack != null && stack.length > 0);
    }

    @Override
    public IStackFrame getTopStackFrame() {
        return (stack == null || stack.length == 0) ? null : stack[0];
    }

    public PyStackFrame findStackFrameByID(String id) {
        if (stack != null) {

            for (int i = 0; i < stack.length; i++) {

                if (id.equals(((PyStackFrame) stack[i]).getId())) {
                    return (PyStackFrame) stack[i];
                }
            }
        }
        return null;
    }

    @Override
    public IBreakpoint[] getBreakpoints() {
        // should return breakpoint that caused this thread to suspend
        // not implementing this seems to cause no harm
        PyBreakpoint[] breaks = new PyBreakpoint[0];
        return breaks;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        AdapterDebug.print(this, adapter);

        if (adapter.equals(ILaunch.class) || adapter.equals(IResource.class)) {
            return target.getAdapter(adapter);

        } else if (adapter.equals(ITaskListResourceAdapter.class)) {
            return null;

        } else if (adapter.equals(IDebugTarget.class)) {
            return (T) target;

        } else if (adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class)) {
            return (T) this.target.getRunToLineTarget();

        } else if (adapter.equals(IPropertySource.class) || adapter.equals(ITaskListResourceAdapter.class)
                || adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class)
                || adapter.equals(org.eclipse.ui.IContributorResourceAdapter.class)
                || adapter.equals(org.eclipse.ui.model.IWorkbenchAdapter.class)
                || adapter.equals(org.eclipse.ui.IActionFilter.class)) {
            return super.getAdapter(adapter);
        }
        //Platform.getAdapterManager().getAdapter(this, adapter);
        AdapterDebug.printDontKnow(this, adapter);
        // ongoing, I do not fully understand all the interfaces they'd like me to support
        return super.getAdapter(adapter);
    }

    @Override
    public String toString() {
        return "PyThread: " + this.id;
    }

}
