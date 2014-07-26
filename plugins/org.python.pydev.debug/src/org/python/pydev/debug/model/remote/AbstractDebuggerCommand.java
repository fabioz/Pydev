/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Mar 23, 2004
 */
package org.python.pydev.debug.model.remote;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Superclass of all debugger commands.
 *
 * Debugger commands know how to interact with pydevd.py.
 * See pydevd.py for protocol information.
 *
 * Command lifecycle:
 *  cmd = new Command() // creation
 *  cmd.getSequence()    // get the sequence number of the command
 *  cmd.getOutgoing()    // asks command for outgoing message
 *  cmd.aboutToSend()    // called right before we go on wire
 *                         // by default, if command needs response
 *                         // it gets posted to in the response queue
 *     if (cmd.needsResponse())
 *         post the command to response queue, otherwise we are done
 *  when response arrives:
 *  if response is an error
 *         cmd.processResponse()
 *     else
 *         cmd.processErrorResponse()
 *
 */
public abstract class AbstractDebuggerCommand {

    static public final int CMD_RUN = 101;
    static public final int CMD_LIST_THREADS = 102;
    static public final int CMD_THREAD_CREATED = 103;
    static public final int CMD_THREAD_KILL = 104;
    static public final int CMD_THREAD_SUSPEND = 105;
    static public final int CMD_THREAD_RUN = 106;
    static public final int CMD_STEP_INTO = 107;
    static public final int CMD_STEP_OVER = 108;
    static public final int CMD_STEP_RETURN = 109;
    static public final int CMD_GET_VARIABLE = 110;
    static public final int CMD_SET_BREAK = 111;
    static public final int CMD_REMOVE_BREAK = 112;
    static public final int CMD_EVALUATE_EXPRESSION = 113;
    static public final int CMD_GET_FRAME = 114;
    static public final int CMD_EXEC_EXPRESSION = 115;
    static public final int CMD_WRITE_TO_CONSOLE = 116;
    static public final int CMD_CHANGE_VARIABLE = 117;
    static public final int CMD_RUN_TO_LINE = 118;
    static public final int CMD_RELOAD_CODE = 119;
    static public final int CMD_GET_COMPLETIONS = 120;

    static public final int CMD_CONSOLE_EXEC = 121;
    static public final int CMD_ADD_EXCEPTION_BREAK = 122;
    static public final int CMD_REMOVE_EXCEPTION_BREAK = 123;
    static public final int CMD_LOAD_SOURCE = 124;
    static public final int CMD_ADD_DJANGO_EXCEPTION_BREAK = 125;
    static public final int CMD_REMOVE_DJANGO_EXCEPTION_BREAK = 126;
    static public final int CMD_SET_NEXT_STATEMENT = 127;
    static public final int CMD_SMART_STEP_INTO = 128;
    static public final int CMD_EXIT = 129;
    static public final int CMD_SIGNATURE_CALL_TRACE = 130;

    static public final int CMD_SET_PY_EXCEPTION = 131;
    static public final int CMD_GET_FILE_CONTENTS = 132;
    static public final int CMD_SET_PROPERTY_TRACE = 133;
    static public final int CMD_EVALUATE_CONSOLE_EXPRESSION = 134;
    static public final int CMD_RUN_CUSTOM_OPERATION = 135;
    static public final int CMD_GET_BREAKPOINT_EXCEPTION = 136;
    static public final int CMD_STEP_CAUGHT_EXCEPTION = 137;
    static public final int CMD_SEND_CURR_EXCEPTION_TRACE = 138;
    static public final int CMD_SEND_CURR_EXCEPTION_TRACE_PROCEEDED = 139;
    static public final int CMD_IGNORE_THROWN_EXCEPTION_AT = 140;
    static public final int CMD_ENABLE_DONT_TRACE = 141;

    static public final int CMD_ERROR = 901;
    static public final int CMD_VERSION = 501;
    static public final int CMD_RETURN = 502;

    protected AbstractDebugTarget target;
    protected ICommandResponseListener responseListener;
    int sequence;

    public AbstractDebuggerCommand(AbstractDebugTarget debugger) {
        this.target = debugger;
        this.responseListener = null;
        sequence = debugger.getNextSequence();
    }

    public void setCompletionListener(ICommandResponseListener listener) {
        this.responseListener = listener;
    }

    /**
     * @return outgoing message
     */
    public abstract String getOutgoing();

    /**
     * Notification right before the command is sent.
     * If subclassed, call super()
     */
    public void aboutToSend() {
        // if we need a response, put me on the waiting queue
        if (needResponse()) {
            target.addToResponseQueue(this);
        }
    }

    /**
     * Does this command require a response?
     *
     * This is meant to be overriden by subclasses if they need a response.
     */
    public boolean needResponse() {
        return false;
    }

    /**
     * returns Sequence #
     */
    public final int getSequence() {
        return sequence;
    }

    /**
     * Called when command completes, if needResponse was true
     */
    public final void processResponse(int cmdCode, String payload) {
        if (cmdCode / 100 == 9) {
            processErrorResponse(cmdCode, payload);
        } else {
            processOKResponse(cmdCode, payload);
        }

        if (responseListener != null) {
            responseListener.commandComplete(this);
        }
    }

    /**
     * notification of the response to the command.
     * You'll get either processResponse or processErrorResponse
     */
    public void processOKResponse(int cmdCode, String payload) {
        PydevDebugPlugin.log(IStatus.ERROR, "Debugger command ignored response " + getClass().toString() + payload,
                null);
    }

    /**
     * notification of the response to the command.
     * You'll get either processResponse or processErrorResponse
     */
    public void processErrorResponse(int cmdCode, String payload) {
        PydevDebugPlugin.log(IStatus.ERROR, "Debugger command ignored error response " + getClass().toString()
                + payload, null);
    }

    public static String makeCommand(int code, int sequence, String payload) {
        FastStringBuffer s = new FastStringBuffer(payload.length() + 20);
        s.append(code);
        s.append("\t");
        s.append(sequence);
        s.append("\t");
        s.append(payload);
        return s.toString();
    }
}
