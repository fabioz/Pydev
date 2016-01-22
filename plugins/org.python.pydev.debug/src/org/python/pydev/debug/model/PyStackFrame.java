/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 22, 2004
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.python.pydev.core.IPyStackFrame;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.debug.model.remote.GetFileContentsCommand;
import org.python.pydev.debug.model.remote.GetFrameCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.editorinput.PySourceLocatorPrefs;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Represents a stack entry.
 *
 * Needs to integrate with the source locator
 */
public class PyStackFrame extends PlatformObject implements IStackFrame, IVariableLocator, IPyStackFrame {

    private String name;
    private PyThread thread;
    private String id;
    private IPath path;
    private int line;
    private volatile IVariable[] variables;
    private IVariableLocator localsLocator;
    private IVariableLocator globalsLocator;
    private IVariableLocator frameLocator;
    private IVariableLocator expressionLocator;
    private AbstractDebugTarget target;
    private volatile boolean onAskGetNewVars = true;

    public PyStackFrame(PyThread in_thread, String in_id, String name, IPath file, int line, AbstractDebugTarget target) {
        this.id = in_id;
        this.name = name;
        this.path = file;
        this.line = line;
        this.thread = in_thread;

        localsLocator = new IVariableLocator() {
            public String getPyDBLocation() {
                return thread.getId() + "\t" + id + "\tLOCAL";
            }

            public String getThreadId() {
                return thread.getId();
            }
        };
        frameLocator = new IVariableLocator() {
            public String getPyDBLocation() {
                return thread.getId() + "\t" + id + "\tFRAME";
            }

            public String getThreadId() {
                return thread.getId();
            }
        };
        globalsLocator = new IVariableLocator() {
            public String getPyDBLocation() {
                return thread.getId() + "\t" + id + "\tGLOBAL";
            }

            public String getThreadId() {
                return thread.getId();
            }
        };
        expressionLocator = new IVariableLocator() {
            public String getPyDBLocation() {
                return thread.getId() + "\t" + id + "\tEXPRESSION";
            }

            public String getThreadId() {
                return thread.getId();
            }
        };
        this.target = target;
    }

    public AbstractDebugTarget getTarget() {
        return target;
    }

    public String getId() {
        return id;
    }

    public String getThreadId() {
        return this.thread.getId();
    }

    public IVariableLocator getLocalsLocator() {
        return localsLocator;
    }

    public IVariableLocator getFrameLocator() {
        return frameLocator;
    }

    public IVariableLocator getGlobalLocator() {
        return globalsLocator;
    }

    public IVariableLocator getExpressionLocator() {
        return expressionLocator;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(IPath path) {
        this.path = path;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public IPath getPath() {
        return path;
    }

    public IThread getThread() {
        return thread;
    }

    public void setVariables(IVariable[] locals) {
        this.variables = locals;
    }

    private final static IVariable[] EMPTY_VARIABLES = new IVariable[0];
    private final static Object lock = new Object();

    /**
     * This interface changed in 3.2... we returned an empty collection before, and used the
     * DeferredWorkbenchAdapter to get the actual children, but now we have to use the
     * DeferredWorkbenchAdapter from here, as it is not called in that other interface
     * anymore.
     *
     * @see org.eclipse.debug.core.model.IStackFrame#getVariables()
     */
    public IVariable[] getVariables() throws DebugException {
        if (onAskGetNewVars) {
            synchronized (lock) {
                //double check idiom for accessing onAskGetNewVars.
                if (onAskGetNewVars) {
                    IVariable[] oldVars = this.variables;
                    if (oldVars == null) {
                        //Temporary in case some other thread asks for it while we're still calculating.
                        this.variables = EMPTY_VARIABLES;
                    }
                    onAskGetNewVars = false;

                    DeferredWorkbenchAdapter adapter = new DeferredWorkbenchAdapter(this);
                    IVariable[] vars = (IVariable[]) adapter.getChildren(this);

                    if (oldVars != null) {
                        this.target.getModificationChecker().verifyVariablesModified(vars, oldVars);

                    } else {
                        this.target.getModificationChecker().verifyModified(this, vars);
                    }

                    this.variables = vars;
                }

            }
        }
        return this.variables;
    }

    /**
     * @return the internal variables array directly (may be null).
     */
    public IVariable[] getInternalVariables() {
        return this.variables;
    }

    public void forceGetNewVariables() {
        this.onAskGetNewVars = true;
        AbstractDebugTarget target = getTarget();
        if (target != null) {
            // I.e.: if we do a new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.CONTENT), the selection
            // of the editor is redone (thus, if the user uses F2 it'd get back to the current breakpoint
            // location because it'd be reselected).
            target.fireEvent(new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.UNSPECIFIED));
        }
    }

    public boolean hasVariables() throws DebugException {
        return true;
    }

    /**
     * Note: line 1-based.
     */
    public int getLineNumber() throws DebugException {
        return line;
    }

    public int getCharStart() throws DebugException {
        return -1;
    }

    public int getCharEnd() throws DebugException {
        return -1;
    }

    private boolean currentStackFrame = false;

    public void setCurrentStackFrame() {
        this.currentStackFrame = true;
    }

    public String getName() throws DebugException {
        String ret = StringUtils.join("", name, " [", path.lastSegment(), ":", Integer.toString(line), "]");
        if (currentStackFrame) {
            ret += "   <-- Current frame";
        }
        return ret;
    }

    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        return new IRegisterGroup[0];
    }

    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }

    public String getModelIdentifier() {
        return thread.getModelIdentifier();
    }

    public IDebugTarget getDebugTarget() {
        return thread.getDebugTarget();
    }

    public ILaunch getLaunch() {
        return thread.getLaunch();
    }

    public boolean canStepInto() {
        return thread.canStepInto();
    }

    public boolean canStepOver() {
        return thread.canStepOver();
    }

    public boolean canStepReturn() {
        return thread.canStepReturn();
    }

    public boolean isStepping() {
        return thread.isStepping();
    }

    public void stepInto() throws DebugException {
        thread.stepInto();
    }

    public void stepOver() throws DebugException {
        thread.stepOver();
    }

    public void stepReturn() throws DebugException {
        thread.stepReturn();
    }

    public boolean canResume() {
        return thread.canResume();
    }

    public boolean canSuspend() {
        return thread.canSuspend();
    }

    public boolean isSuspended() {
        return thread.isSuspended();
    }

    public void resume() throws DebugException {
        thread.resume();
    }

    public void suspend() throws DebugException {
        thread.suspend();
    }

    public boolean canTerminate() {
        return thread.canTerminate();
    }

    public boolean isTerminated() {
        return thread.isTerminated();
    }

    public void terminate() throws DebugException {
        thread.terminate();
    }

    @Override
    public Object getAdapter(Class adapter) {
        AdapterDebug.print(this, adapter);

        if (adapter.equals(ILaunch.class) || adapter.equals(IResource.class)) {
            return thread.getAdapter(adapter);
        }

        if (adapter.equals(ITaskListResourceAdapter.class)) {
            return null;
        }

        if (adapter.equals(IDebugTarget.class)) {
            return thread.getDebugTarget();
        }

        if (adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class)) {
            return this.target.getRunToLineTarget();
        }

        if (adapter.equals(IPropertySource.class) || adapter.equals(ITaskListResourceAdapter.class)
                || adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class)) {
            return super.getAdapter(adapter);
        }

        if (adapter.equals(IDeferredWorkbenchAdapter.class)) {
            return new DeferredWorkbenchAdapter(this);
        }

        AdapterDebug.printDontKnow(this, adapter);
        // ongoing, I do not fully understand all the interfaces they'd like me to support
        return super.getAdapter(adapter);
    }

    /**
     * fixed - this was bug http://sourceforge.net/tracker/index.php?func=detail&aid=1174821&group_id=85796&atid=577329
     * in the forum (unable to get stack correctly when recursing)
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * fixed - this was bug http://sourceforge.net/tracker/index.php?func=detail&aid=1174821&group_id=85796&atid=577329
     * in the forum (unable to get stack correctly when recursing)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PyStackFrame) {
            PyStackFrame sf = (PyStackFrame) obj;
            return this.id.equals(sf.id) && this.path.toString().equals(sf.path.toString()) && this.line == sf.line
                    && this.getThreadId().equals(sf.getThreadId());
        }
        return false;
    }

    public GetVariableCommand getFrameCommand(AbstractDebugTarget dbg) {
        return new GetFrameCommand(dbg, frameLocator.getPyDBLocation());
    }

    public String getPyDBLocation() {
        return this.frameLocator.getPyDBLocation();
    }

    public AbstractRemoteDebugger getDebugger() {
        return target.getDebugger();
    }

    @Override
    public String toString() {
        return "PyStackFrame: " + this.id;
    }

    private String fileContents = null;

    public String getFileContents() {
        if (fileContents == null) {
            // send the command, and then busy-wait
            GetFileContentsCommand cmd = new GetFileContentsCommand(target, this.path.toOSString());

            final Object lock = new Object();
            final String[] response = new String[1];

            cmd.setCompletionListener(new ICommandResponseListener() {

                public void commandComplete(AbstractDebuggerCommand cmd) {
                    try {
                        response[0] = ((GetFileContentsCommand) cmd).getResponse();
                    } catch (CoreException e) {
                        response[0] = "";
                    }
                    try {
                        synchronized (lock) {
                            lock.notify();
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            });

            target.postCommand(cmd);
            int timeout = PySourceLocatorPrefs.getFileContentsTimeout();
            long initialTimeMillis = System.currentTimeMillis();
            while (response[0] == null) {
                synchronized (lock) {
                    try {
                        lock.wait(50);
                    } catch (Exception e) {
                        //ignore
                    }
                }
                if (System.currentTimeMillis() - initialTimeMillis > timeout) {
                    break;
                }
            }
            fileContents = response[0];
        }
        return fileContents;
    }
}
