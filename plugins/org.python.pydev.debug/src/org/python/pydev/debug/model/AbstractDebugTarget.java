/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.internal.console.IOConsolePartition;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.python.pydev.ast.location.FindWorkspaceFiles;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.IConsoleInputListener;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.core.PydevDebugPreferencesInitializer;
import org.python.pydev.debug.curr_exception.CurrentExceptionView;
import org.python.pydev.debug.model.XMLUtils.StoppedStack;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.debug.model.remote.AddIgnoreThrownExceptionIn;
import org.python.pydev.debug.model.remote.RemoveBreakpointCommand;
import org.python.pydev.debug.model.remote.RunCommand;
import org.python.pydev.debug.model.remote.SendPyExceptionCommand;
import org.python.pydev.debug.model.remote.SetBreakpointCommand;
import org.python.pydev.debug.model.remote.SetDjangoExceptionBreakpointCommand;
import org.python.pydev.debug.model.remote.SetDontTraceEnabledCommand;
import org.python.pydev.debug.model.remote.SetJinja2ExceptionBreakpointCommand;
import org.python.pydev.debug.model.remote.SetPropertyTraceCommand;
import org.python.pydev.debug.model.remote.SetShowReturnValuesEnabledCommand;
import org.python.pydev.debug.model.remote.ThreadListCommand;
import org.python.pydev.debug.model.remote.VersionCommand;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * This is the target for the debug (
 *
 * @author Fabio
 */
@SuppressWarnings("restriction")
public abstract class AbstractDebugTarget extends AbstractDebugTargetWithTransmission implements IDebugTarget,
        ILaunchListener, IExceptionsBreakpointListener, IPropertyTraceListener {

    private static final boolean DEBUG = false;

    /**
     * Path pointing to the file that started the debug (e.g.: file with __name__ == '__main__')
     */
    protected IPath[] file;

    /**
     * The threads found in the debugger.
     */
    protected PyThread[] threads;

    /**
     * Indicates whether we've already disconnected from the debugger.
     */
    protected boolean disconnected = false;

    /**
     * This is the instance used to pass messages to the debugger.
     */
    protected AbstractRemoteDebugger debugger;

    /**
     * Launch that triggered the debug session.
     */
    protected ILaunch launch;

    private PyRunToLineTarget runToLineTarget;

    public AbstractDebugTarget() {
    }

    private Set<Integer> currentBreakpointsAdded = new HashSet<>();

    @Override
    public abstract boolean canTerminate();

    @Override
    public abstract boolean isTerminated();

    @Override
    public void terminate() {
        PydevPlugin plugin = PydevPlugin.getDefault();
        if (plugin != null) {
            IPreferenceStore preferenceStore = plugin.getPreferenceStore();
            if (preferenceStore != null) {
                preferenceStore.removePropertyChangeListener(listener);
            }
        }
        if (socket != null) {
            try {
                socket.shutdownInput(); // trying to make my pydevd notice that the socket is gone
            } catch (Exception e) {
                // ok, ignore
            }
            try {
                socket.shutdownOutput();
            } catch (Exception e) {
                // ok, ignore
            }
            try {
                socket.close();
            } catch (Exception e) {
                // ok, ignore
            }
        }
        socket = null;
        disconnected = true;

        if (writer != null) {
            writer.done();
            writer = null;
        }
        if (reader != null) {
            reader.done();
            reader = null;
        }

        if (DEBUG) {
            System.out.println("TERMINATE");
        }

        threads = new PyThread[0];
        fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));

    }

    public AbstractRemoteDebugger getDebugger() {
        return debugger;
    }

    @Override
    public void launchAdded(ILaunch launch) {
        // noop
    }

    @Override
    public void launchChanged(ILaunch launch) {
        // noop
    }

    // From IDebugElement
    @Override
    public String getModelIdentifier() {
        return PyDebugModelPresentation.PY_DEBUG_MODEL_ID;
    }

    // From IDebugElement
    @Override
    public IDebugTarget getDebugTarget() {
        return this;
    }

    @Override
    public String getName() throws DebugException {
        if (file != null) {
            return PythonRunnerConfig.getRunningName(file);
        } else {
            return "unknown"; //TODO: SHOW PROPER PROCESS ID!
        }
    }

    @Override
    public boolean canResume() {
        for (int i = 0; i < threads.length; i++) {
            if (threads[i].canResume()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canSuspend() {
        for (int i = 0; i < threads.length; i++) {
            if (threads[i].canSuspend()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSuspended() {
        return false;
    }

    @Override
    public void resume() throws DebugException {
        for (int i = 0; i < threads.length; i++) {
            threads[i].resume();
        }
    }

    @Override
    public void suspend() throws DebugException {
        for (int i = 0; i < threads.length; i++) {
            threads[i].suspend();
        }
    }

    @Override
    public IThread[] getThreads() throws DebugException {
        if (debugger == null) {
            return null;
        }

        if (threads == null) {
            ThreadListCommand cmd = new ThreadListCommand(this);
            this.postCommand(cmd);
            try {
                cmd.waitUntilDone(1000);
                threads = cmd.getThreads();
            } catch (InterruptedException e) {
                threads = new PyThread[0];
            }
        }
        return threads;
    }

    @Override
    public boolean hasThreads() throws DebugException {
        return true;
    }

    //Breakpoints ------------------------------------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.python.pydev.debug.model.IExceptionsBreakpointListener#onSetConfiguredExceptions()
     */
    @Override
    public void onSetConfiguredExceptions() {
        // Sending python exceptions to the debugger
        SendPyExceptionCommand sendCmd = new SendPyExceptionCommand(this);
        this.postCommand(sendCmd);
    }

    /**
     * Same as onAddIgnoreThrownExceptionIn, but bulk-created with all available.
     */
    @Override
    public void onUpdateIgnoreThrownExceptions() {
        AddIgnoreThrownExceptionIn cmd = new AddIgnoreThrownExceptionIn(this);
        this.postCommand(cmd);
    }

    @Override
    public void onAddIgnoreThrownExceptionIn(File file, int lineNumber) {
        AddIgnoreThrownExceptionIn cmd = new AddIgnoreThrownExceptionIn(this, file, lineNumber);
        this.postCommand(cmd);
    }

    /*
     * (non-Javadoc)
     * @see org.python.pydev.debug.model.IPropertyTraceListener#onSetPropertyTraceConfiguration()
     */
    @Override
    public void onSetPropertyTraceConfiguration() {
        // Sending whether to trace python property
        SetPropertyTraceCommand sendCmd = new SetPropertyTraceCommand(this);
        this.postCommand(sendCmd);
    }

    /**
     * @return true if the given breakpoint is supported by this target
     */
    @Override
    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        return breakpoint instanceof PyBreakpoint;
    }

    /**
     * @return true if all the breakpoints should be skipped. Patch from bug:
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1960983&group_id=85796&atid=577329
     */
    private boolean shouldSkipBreakpoints() {
        DebugPlugin manager = DebugPlugin.getDefault();
        return manager != null && !manager.getBreakpointManager().isEnabled();
    }

    /**
     * Adds a breakpoint if it's enabled.
     */
    @Override
    public void breakpointAdded(IBreakpoint breakpoint) {
        try {
            if (breakpoint instanceof PyBreakpoint) {
                PyBreakpoint b = (PyBreakpoint) breakpoint;
                if (b.isEnabled() && !shouldSkipBreakpoints()) {
                    String condition = null;
                    if (b.isConditionEnabled()) {
                        condition = b.getCondition();
                        if (condition != null) {
                            condition = StringUtils.replaceAll(condition, "\n",
                                    "@_@NEW_LINE_CHAR@_@");
                            condition = StringUtils.replaceAll(condition, "\t",
                                    "@_@TAB_CHAR@_@");
                        }
                    }
                    String file2 = b.getFile();
                    Object line = b.getLine();
                    if (file2 == null || line == null) {
                        Log.log("Trying to add breakpoint with invalid file: " + file2 + " or line: " + line);
                    } else {
                        this.currentBreakpointsAdded.add(b.breakpointId);
                        SetBreakpointCommand cmd = new SetBreakpointCommand(this, b.breakpointId, file2, line,
                                condition, b.getFunctionName(), b.getType());
                        this.postCommand(cmd);
                    }
                }
            }
        } catch (CoreException e) {
            Log.log(e);
        }
    }

    /**
     * Removes an existing breakpoint from the debug target.
     */
    @Override
    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
        if (breakpoint instanceof PyBreakpoint) {
            PyBreakpoint b = (PyBreakpoint) breakpoint;
            if (currentBreakpointsAdded.contains(b.breakpointId)) {
                currentBreakpointsAdded.remove(b.breakpointId);
                RemoveBreakpointCommand cmd = new RemoveBreakpointCommand(this, b.breakpointId, b.getFile(),
                        b.getType());
                this.postCommand(cmd);
            }
        }
    }

    /**
     * Called when a breakpoint is changed.
     * E.g.:
     *  - When line numbers change in the file
     *  - When the manager decides to enable/disable all existing markers
     *  - When the breakpoint properties (hit condition) are edited
     */
    @Override
    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
        if (breakpoint instanceof PyBreakpoint) {
            breakpointRemoved(breakpoint, null);
            breakpointAdded(breakpoint);
        }
    }

    //End Breakpoints --------------------------------------------------------------------------------------------------

    // Storage retrieval is not supported
    @Override
    public boolean supportsStorageRetrieval() {
        return false;
    }

    @Override
    public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
        return null;
    }

    /**
     * When a command that originates from daemon is received,
     * this routine processes it.
     * The responses to commands originating from here
     * are processed by commands themselves
     */
    public void processCommand(String sCmdCode, String sSeqCode, String payload) {
        if (DEBUG) {
            System.out.println("process command:" + sCmdCode +
                    "\tseq:" + sSeqCode +
                    "\tpayload:" + payload +
                    "\n\n");
        }
        try {
            int cmdCode = Integer.parseInt(sCmdCode);

            if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_CREATED) {
                processThreadCreated(payload);

            } else if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_KILL) {
                processThreadKilled(payload);

            } else if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_SUSPEND) {
                processThreadSuspended(payload);

            } else if (cmdCode == AbstractDebuggerCommand.CMD_THREAD_RUN) {
                processThreadRun(payload);

            } else if (cmdCode == AbstractDebuggerCommand.CMD_GET_BREAKPOINT_EXCEPTION) {
                processBreakpointException(payload);

            } else if (cmdCode == AbstractDebuggerCommand.CMD_SEND_CURR_EXCEPTION_TRACE) {
                processCaughtExceptionTraceSent(payload);

            } else if (cmdCode == AbstractDebuggerCommand.CMD_SEND_CURR_EXCEPTION_TRACE_PROCEEDED) {
                processCaughtExceptionTraceProceededSent(payload);

            } else if (cmdCode == AbstractDebuggerCommand.CMD_INPUT_REQUESTED) {
                if ("true".equalsIgnoreCase(payload.trim())) {
                    this.setWaitingForInput(true);

                } else if ("false".equalsIgnoreCase(payload.trim())) {
                    this.setWaitingForInput(false);

                } else {
                    PydevDebugPlugin.log(IStatus.WARNING, "Unexpected payload for CMD_INPUT_REQUESTED" +
                            "\npayload:" + payload, null);
                }

            } else if (cmdCode == AbstractDebuggerCommand.CMD_SET_PROTOCOL) {
                // Just ignore

            } else if (cmdCode == AbstractDebuggerCommand.CMD_PROCESS_CREATED) {
                // We don't really need to handle process created for now.

            } else if (cmdCode == AbstractDebuggerCommand.CMD_SET_NEXT_STATEMENT) {
                // This is just an acknowledgement.

            } else if (cmdCode == AbstractDebuggerCommand.CMD_RETURN) {
                // This is just an acknowledgement.

            } else if (cmdCode == AbstractDebuggerCommand.CMD_EXIT) {
                // May be sent when about to exit.

            } else {
                PydevDebugPlugin.log(IStatus.WARNING, "Unexpected debugger command:" + sCmdCode +
                        "\nseq:" + sSeqCode
                        +
                        "\npayload:" + payload, null);
            }
        } catch (Exception e) {
            PydevDebugPlugin.log(IStatus.ERROR, "Error processing: " + sCmdCode +
                    "\npayload: " + payload, e);
        }
    }

    public void fireEvent(DebugEvent event) {
        DebugPlugin manager = DebugPlugin.getDefault();
        if (manager != null) {
            manager.fireDebugEventSet(new DebugEvent[] { event });
        }
    }

    /**
     * @return an existing thread with a given id (null if none)
     */
    protected PyThread findThreadByID(String thread_id) {
        for (IThread thread : threads) {
            if (thread_id.equals(((PyThread) thread).getId())) {
                return (PyThread) thread;
            }
        }
        return null;
    }

    /**
     * Add it to the list of threads
     */
    private void processThreadCreated(String payload) {

        PyThread[] newThreads;
        try {
            newThreads = XMLUtils.ThreadsFromXML(this, payload);
        } catch (CoreException e) {
            PydevDebugPlugin.errorDialog("Error in processThreadCreated", e);
            return;
        }

        // Hide Pydevd threads if requested
        if (PydevDebugPlugin.getDefault().getPreferenceStore()
                .getBoolean(PydevDebugPreferencesInitializer.HIDE_PYDEVD_THREADS)) {
            int removeThisMany = 0;

            for (int i = 0; i < newThreads.length; i++) {
                if (newThreads[i].isPydevThread()) {
                    removeThisMany++;
                }
            }

            if (removeThisMany > 0) {
                int newSize = newThreads.length - removeThisMany;

                if (newSize == 0) { // no threads to add
                    return;

                } else {

                    PyThread[] newnewThreads = new PyThread[newSize];
                    int i = 0;

                    for (PyThread newThread : newThreads) {
                        if (!newThread.isPydevThread()) {
                            newnewThreads[i] = newThread;
                            i += 1;
                        }
                    }

                    newThreads = newnewThreads;

                }
            }
        }

        // add threads to the thread list, and fire event
        if (threads == null) {
            threads = newThreads;

        } else {
            threads = ArrayUtils.concatArrays(threads, newThreads);
        }
        // Now notify debugger that new threads were added
        for (int i = 0; i < newThreads.length; i++) {
            fireEvent(new DebugEvent(newThreads[i], DebugEvent.CREATE));
        }
    }

    // Remote this from our thread list
    private void processThreadKilled(String thread_id) {
        PyThread threadToDelete = findThreadByID(thread_id);
        if (threadToDelete != null) {
            int j = 0;
            PyThread[] newThreads = new PyThread[threads.length - 1];
            for (int i = 0; i < threads.length; i++) {
                if (threads[i] != threadToDelete) {
                    newThreads[j++] = threads[i];
                }
            }
            threads = newThreads;
            fireEvent(new DebugEvent(threadToDelete, DebugEvent.TERMINATE));
        }
    }

    private void processThreadSuspended(String payload) {
        StoppedStack threadNstack;
        try {
            threadNstack = XMLUtils.XMLToStack(this, payload);
        } catch (CoreException e) {
            PydevDebugPlugin.errorDialog("Error reading ThreadSuspended", e);
            return;
        }

        PyThread t = threadNstack.thread;
        int reason = DebugEvent.UNSPECIFIED;
        String stopReason = threadNstack.stopReason;

        if (stopReason != null) {
            int stopReason_i = Integer.parseInt(stopReason);

            if (stopReason_i == AbstractDebuggerCommand.CMD_STEP_OVER
                    || stopReason_i == AbstractDebuggerCommand.CMD_STEP_INTO
                    || stopReason_i == AbstractDebuggerCommand.CMD_STEP_CAUGHT_EXCEPTION
                    || stopReason_i == AbstractDebuggerCommand.CMD_STEP_RETURN
                    || stopReason_i == AbstractDebuggerCommand.CMD_RUN_TO_LINE
                    || stopReason_i == AbstractDebuggerCommand.CMD_SET_NEXT_STATEMENT) {

                //Code which could be used to know where a caught exception broke the debugger.
                //if (stopReason_i == AbstractDebuggerCommand.CMD_STEP_CAUGHT_EXCEPTION) {
                //    System.out.println("Stopped: caught exception");
                //    IStackFrame stackFrame[] = (IStackFrame[]) threadNstack[2];
                //    if (stackFrame.length > 0) {
                //        IStackFrame currStack = stackFrame[0];
                //        if (currStack instanceof PyStackFrame) {
                //            PyStackFrame pyStackFrame = (PyStackFrame) currStack;
                //            try {
                //                System.out.println(pyStackFrame.getPath() + " " + pyStackFrame.getLineNumber());
                //            } catch (DebugException e) {
                //                Log.log(e);
                //            }
                //        }
                //    }
                //
                //}
                reason = DebugEvent.STEP_END;

            } else if (stopReason_i == AbstractDebuggerCommand.CMD_THREAD_SUSPEND) {
                reason = DebugEvent.CLIENT_REQUEST;

            } else if (stopReason_i == AbstractDebuggerCommand.CMD_SET_BREAK) {
                reason = DebugEvent.BREAKPOINT;

            } else if (stopReason_i == AbstractDebuggerCommand.CMD_ADD_EXCEPTION_BREAK) {
                reason = DebugEvent.BREAKPOINT; // exception breakpoint

            } else {
                PydevDebugPlugin.log(IStatus.ERROR, "Unexpected reason for suspension: " + stopReason_i, null);
                reason = DebugEvent.UNSPECIFIED;
            }
        }
        if (t != null) {
            IStackFrame stackFrame[] = threadNstack.stack;
            t.setSuspended(true, stackFrame);
            fireEvent(new DebugEvent(t, DebugEvent.SUSPEND, reason));
        }
    }

    /**
     * @param payload a string in the format: thread_id\tresume_reason
     * E.g.: pid3720_zad_seq1\t108
     *
     * @return a tuple with the thread id and the reason it stopped.
     * @throws CoreException
     */
    public static Tuple<String, String> getThreadIdAndReason(String payload) throws CoreException {
        List<String> split = StringUtils.split(payload.trim(), '\t');
        if (split.size() != 2) {
            String msg = "Unexpected threadRun payload " + payload +
                    "(unable to match)";
            throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, msg, new RuntimeException(msg)));
        }
        return new Tuple<String, String>(split.get(0), split.get(1));
    }

    /**
     * ThreadRun event processing
     */
    private void processThreadRun(String payload) {
        try {
            Tuple<String, String> threadIdAndReason = getThreadIdAndReason(payload);
            int resumeReason = DebugEvent.UNSPECIFIED;
            try {
                int raw_reason = Integer.parseInt(threadIdAndReason.o2);
                if (raw_reason == AbstractDebuggerCommand.CMD_STEP_OVER) {
                    resumeReason = DebugEvent.STEP_OVER;
                } else if (raw_reason == AbstractDebuggerCommand.CMD_STEP_RETURN) {
                    resumeReason = DebugEvent.STEP_RETURN;
                } else if (raw_reason == AbstractDebuggerCommand.CMD_STEP_INTO
                        || raw_reason == AbstractDebuggerCommand.CMD_STEP_CAUGHT_EXCEPTION) {
                    resumeReason = DebugEvent.STEP_INTO;
                } else if (raw_reason == AbstractDebuggerCommand.CMD_RUN_TO_LINE) {
                    resumeReason = DebugEvent.UNSPECIFIED;
                } else if (raw_reason == AbstractDebuggerCommand.CMD_SET_NEXT_STATEMENT) {
                    resumeReason = DebugEvent.UNSPECIFIED;
                } else if (raw_reason == AbstractDebuggerCommand.CMD_THREAD_RUN || raw_reason == -1) {
                    resumeReason = DebugEvent.CLIENT_REQUEST;
                } else {
                    PydevDebugPlugin.log(IStatus.ERROR,
                            "Unexpected resume reason code: " + raw_reason + " payload: " + payload, null);
                    resumeReason = DebugEvent.UNSPECIFIED;
                }
            } catch (NumberFormatException e) {
                // expected, when pydevd reports "None"
                resumeReason = DebugEvent.UNSPECIFIED;
            }

            String threadID = threadIdAndReason.o1;
            PyThread t = findThreadByID(threadID);
            if (t != null) {
                t.setSuspended(false, null);
                fireEvent(new DebugEvent(t, DebugEvent.RESUME, resumeReason));

            } else {
                FastStringBuffer buf = new FastStringBuffer();
                for (PyThread thread : threads) {
                    if (buf.length() > 0) {
                        buf.append(", ");
                    }
                    buf.append("id: " + thread.getId());
                }
                String msg = "Unable to find thread: " + threadID +
                        " available: " + buf;
                PydevDebugPlugin.log(IStatus.ERROR, msg, new RuntimeException(msg));
            }
        } catch (CoreException e1) {
            Log.log(e1);
        }

    }

    /**
     * Handle the exception received while evaluating the breakpoint condition
     *
     * @param payload
     */
    private void processBreakpointException(String payload) {
        PyConditionalBreakPointManager.getInstance().handleBreakpointException(this, payload);
    }

    private Object currExceptionsLock = new Object();
    private List<CaughtException> currExceptions = new ArrayList<>();

    public List<CaughtException> getCurrExceptions() {
        synchronized (currExceptionsLock) {
            return new ArrayList<>(currExceptions);
        }
    }

    public boolean hasCurrExceptions() {
        synchronized (currExceptionsLock) {
            return currExceptions.size() > 0;
        }
    }

    private void processCaughtExceptionTraceProceededSent(String payload) {
        synchronized (currExceptionsLock) {
            for (Iterator<CaughtException> it = currExceptions.iterator(); it.hasNext();) {
                CaughtException s = it.next();
                if (payload.equals(s.threadNstack.thread.getId())) {
                    it.remove();
                    break;
                }

            }
        }
        updateView();
    }

    /**
     * Handle the exception received while evaluating the breakpoint condition
     *
     * @param payload
     */
    private void processCaughtExceptionTraceSent(String payload) {
        List<String> split = StringUtils.split(payload, '\t', 4);
        StoppedStack threadNstack;
        try {
            threadNstack = XMLUtils.XMLToStack(this, split.get(3));
        } catch (CoreException e) {
            PydevDebugPlugin.errorDialog("Error on processCaughtExceptionTraceSent", e);
            return;
        }
        synchronized (currExceptionsLock) {
            //payload is: currentFrameId, excType, msg, xml with thread/stack
            currExceptions.add(new CaughtException(split.get(0), split.get(1), split.get(2), threadNstack));
        }

        updateView();
    }

    private void updateView() {
        RunInUiThread.async(new Runnable() {

            @Override
            public void run() {
                CurrentExceptionView view = CurrentExceptionView.getView(true);
                view.update();
            }
        });
    }

    /**
     * Listens to the (org) PydevPlugin preferences.
     */
    private final IPropertyChangeListener listener = new IPropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String property = event.getProperty();
            if (property.equals(PyDevEditorPreferences.DONT_TRACE_ENABLED)) {
                sendDontTraceEnabledCommand();

            } else if (property.equals(PyDevEditorPreferences.SHOW_RETURN_VALUES)) {
                sendShowReturnValuesEnabledCommand();

            } else if (property.equals(PyDevEditorPreferences.TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS)) {
                sendSetDjangoExceptionBreakpointCommand();

            } else if (property.equals(PyDevEditorPreferences.TRACE_JINJA2_TEMPLATE_RENDER_EXCEPTIONS)) {
                sendSetJinja2ExceptionBreakpointCommand();
            }
        }
    };

    /**
     * Called after debugger has been connected.
     *
     * Here we send all the initialization commands
     * and exceptions on which pydev debugger needs to break
     */
    public void initialize() {
        // we post version command just for fun
        // it establishes the connection
        this.postCommand(new VersionCommand(this));

        // now, register all the breakpoints in all projects
        addBreakpointsFor(ResourcesPlugin.getWorkspace().getRoot());

        // Sending python exceptions and property trace state before sending run command
        this.onSetConfiguredExceptions();
        this.onSetPropertyTraceConfiguration();
        this.onUpdateIgnoreThrownExceptions();
        this.sendSetDjangoExceptionBreakpointCommand();
        this.sendSetJinja2ExceptionBreakpointCommand();
        this.sendDontTraceEnabledCommand();
        this.sendShowReturnValuesEnabledCommand();

        IPreferenceStore pyPrefsStore = PydevPlugin.getDefault().getPreferenceStore();
        pyPrefsStore.addPropertyChangeListener(listener);

        // Send the run command, and we are off
        RunCommand run = new RunCommand(this);
        this.postCommand(run);
    }

    private void sendDontTraceEnabledCommand() {
        IPreferenceStore pyPrefsStore = PydevPlugin.getDefault().getPreferenceStore();
        SetDontTraceEnabledCommand cmd = new SetDontTraceEnabledCommand(this,
                pyPrefsStore.getBoolean(PyDevEditorPreferences.DONT_TRACE_ENABLED));
        this.postCommand(cmd);
    }

    private void sendShowReturnValuesEnabledCommand() {
        IPreferenceStore pyPrefsStore = PydevPlugin.getDefault().getPreferenceStore();
        SetShowReturnValuesEnabledCommand cmd = new SetShowReturnValuesEnabledCommand(this,
                pyPrefsStore.getBoolean(PyDevEditorPreferences.SHOW_RETURN_VALUES));
        this.postCommand(cmd);
    }

    private void sendSetDjangoExceptionBreakpointCommand() {
        IPreferenceStore pyPrefsStore = PydevPlugin.getDefault().getPreferenceStore();
        SetDjangoExceptionBreakpointCommand cmd = new SetDjangoExceptionBreakpointCommand(
                this, pyPrefsStore.getBoolean(PyDevEditorPreferences.TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS));
        this.postCommand(cmd);
    }

    private void sendSetJinja2ExceptionBreakpointCommand() {
        IPreferenceStore pyPrefsStore = PydevPlugin.getDefault().getPreferenceStore();
        SetJinja2ExceptionBreakpointCommand cmd = new SetJinja2ExceptionBreakpointCommand(
                this, pyPrefsStore.getBoolean(PyDevEditorPreferences.TRACE_JINJA2_TEMPLATE_RENDER_EXCEPTIONS));
        this.postCommand(cmd);
    }

    /**
     * Adds the breakpoints associated with a container.
     * @param container the container we're interested in (usually workspace root)
     */
    private void addBreakpointsFor(IContainer container) {
        try {
            IMarker[] markers = container.findMarkers(PyBreakpoint.PY_BREAK_MARKER, true, IResource.DEPTH_INFINITE);
            IMarker[] condMarkers = container.findMarkers(PyBreakpoint.PY_CONDITIONAL_BREAK_MARKER, true,
                    IResource.DEPTH_INFINITE);
            IMarker[] djangoMarkers = container.findMarkers(PyBreakpoint.DJANGO_BREAK_MARKER, true,
                    IResource.DEPTH_INFINITE);
            IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();

            for (IMarker marker : markers) {
                PyBreakpoint brk = (PyBreakpoint) breakpointManager.getBreakpoint(marker);
                breakpointAdded(brk);
            }

            for (IMarker marker : condMarkers) {
                PyBreakpoint brk = (PyBreakpoint) breakpointManager.getBreakpoint(marker);
                breakpointAdded(brk);
            }

            for (IMarker marker : djangoMarkers) {
                PyBreakpoint brk = (PyBreakpoint) breakpointManager.getBreakpoint(marker);
                breakpointAdded(brk);
            }
        } catch (Throwable t) {
            PydevDebugPlugin.errorDialog("Error setting breakpoints", t);
        }
    }

    /**
     * This function adds the input listener extension point, so that plugins that only care about
     * the input in the console can know about it.
     */
    @SuppressWarnings({ "unchecked" })
    public void addConsoleInputListener() {
        IConsole console = DebugUITools.getConsole(this.getProcess());
        if (console instanceof ProcessConsole) {
            final ProcessConsole c = (ProcessConsole) console;
            final List<IConsoleInputListener> participants = ExtensionHelper
                    .getParticipants(ExtensionHelper.PYDEV_DEBUG_CONSOLE_INPUT_LISTENER);
            final AbstractDebugTarget target = this;

            target.addProcessConsole(c);

            //let's listen the doc for the changes
            c.getDocument().addDocumentListener(new IDocumentListener() {

                @Override
                public void documentAboutToBeChanged(DocumentEvent event) {
                    if (target.isWaitingForInput()) {
                        return;
                    }

                    //only report when we have a new line
                    if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
                        try {
                            ITypedRegion partition = event.fDocument.getPartition(event.fOffset);
                            if (partition instanceof IOConsolePartition) {
                                IOConsolePartition p = (IOConsolePartition) partition;

                                //we only communicate about inputs (because we only care about what the user writes)
                                if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
                                    if (event.fText.length() <= 2) {
                                        //the user typed something
                                        final String inputFound = event.getDocument().get(p.getOffset(), p.getLength());
                                        for (IConsoleInputListener listener : participants) {
                                            listener.newLineReceived(inputFound, target);
                                        }
                                    }

                                }
                            }
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }

                }

                @Override
                public void documentChanged(DocumentEvent event) {
                    if (target.isWaitingForInput()) {
                        return;
                    }

                    //only report when we have a new line
                    if (event.fText.indexOf('\r') != -1 || event.fText.indexOf('\n') != -1) {
                        try {
                            ITypedRegion partition = event.fDocument.getPartition(event.fOffset);
                            if (partition instanceof IOConsolePartition) {
                                IOConsolePartition p = (IOConsolePartition) partition;

                                //we only communicate about inputs (because we only care about what the user writes)
                                if (p.getType().equals(IOConsolePartition.INPUT_PARTITION_TYPE)) {
                                    if (event.fText.length() > 2) {
                                        //the user pasted something
                                        for (IConsoleInputListener listener : participants) {
                                            listener.pasteReceived(event.fText, target);
                                        }
                                    }

                                }
                            }
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                }

            });
        }
    }

    @Override
    public boolean canDisconnect() {
        return !disconnected;
    }

    @Override
    public void disconnect() throws DebugException {
        this.terminate();
    }

    @Override
    public boolean isDisconnected() {
        return disconnected;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        AdapterDebug.print(this, adapter);

        // Not really sure what to do here, but I am trying
        if (adapter.equals(ILaunch.class)) {
            return (T) launch;

        } else if (adapter.equals(IResource.class)) {
            // used by Variable ContextManager, and Project:Properties menu item
            if (file != null && file.length > 0) {
                return (T) FindWorkspaceFiles.getFileForLocation(file[0], null);
            } else {
                return null;
            }

        } else if (adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class)) {
            return (T) this.getRunToLineTarget();

        } else if (adapter.equals(IPropertySource.class)) {
            return launch.getAdapter(adapter);

        } else if (adapter.equals(ITaskListResourceAdapter.class)
                || adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class)) {
            return super.getAdapter(adapter);
        }

        AdapterDebug.printDontKnow(this, adapter);
        return super.getAdapter(adapter);
    }

    public PyRunToLineTarget getRunToLineTarget() {
        if (this.runToLineTarget == null) {
            this.runToLineTarget = new PyRunToLineTarget();
        }
        return this.runToLineTarget;
    }

    //From IDebugElement
    @Override
    public ILaunch getLaunch() {
        return launch;
    }

}
