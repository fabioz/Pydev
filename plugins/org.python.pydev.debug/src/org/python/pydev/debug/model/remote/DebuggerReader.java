/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.AbstractDebugTargetWithTransmission;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.HttpProtocolUtils;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.dialogs.PyDialogHelpers;

/**
 * Reads and dispatches commands
 */
public class DebuggerReader implements Runnable {
    /**
     * can be specified to debug this class
     */
    private static final boolean DEBUG = false;

    /**
     * socket we are reading
     */
    private Socket socket;

    /**
     * Volatile, as multiple threads may ask it to be 'done'
     */
    private volatile boolean done = false;

    /**
     * commands waiting for response. Their keys are the sequence ids
     */
    private Dictionary<Integer, AbstractDebuggerCommand> responseQueue = new Hashtable<Integer, AbstractDebuggerCommand>();

    /**
     * we read from this
     */
    private InputStream in;

    /**
     * that's the debugger that made us... we have to finish it when we are done
     */
    private AbstractDebugTarget remote;

    /**
     * Create it
     *
     * @param s socket we are reading from
     * @param r the debugger associated
     *
     * @throws IOException
     */
    public DebuggerReader(Socket s, AbstractDebugTargetWithTransmission r) throws IOException {
        remote = (AbstractDebugTarget) r;
        socket = s;
        in = socket.getInputStream();
    }

    /**
     * mark things as done
     */
    public void done() {
        this.done = true;
    }

    /**
     * @param cmd
     */
    public void addToResponseQueue(AbstractDebuggerCommand cmd) {
        int sequence = cmd.getSequence();
        synchronized (responseQueue) {
            responseQueue.put(sequence, cmd);
        }
    }

    /**
     * Parses & dispatches the command
     */
    private void processCommand(String cmdLine) {
        try {
            String[] cmdParsed = cmdLine.split("\t", 3);
            int cmdCode = Integer.parseInt(cmdParsed[0]);
            int seqCode = Integer.parseInt(cmdParsed[1]);
            String payload = cmdParsed[2];

            // is there a response waiting
            AbstractDebuggerCommand cmd;
            synchronized (responseQueue) {
                cmd = responseQueue.remove(new Integer(seqCode));
            }

            if (cmd == null) {
                if (remote != null) {
                    remote.processCommand(cmdParsed[0], cmdParsed[1], payload);
                } else {
                    PydevDebugPlugin.log(IStatus.ERROR, "internal error, command received no target", null);
                }
            } else {
                cmd.processResponse(cmdCode, payload);
            }
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * keep reading until we finish (that should happen when an exception is thrown, or if it is set as
     * done from outside)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        HttpProtocolUtils httpProtocol = new HttpProtocolUtils();
        ICallback<String, Object> onUnexpectedMessage = (unexpectedMessage) -> {
            String msg = "It seems an old version of the PyDev Debugger is being used (please update the pydevd package being used).\n\nFound message:\n"
                    + unexpectedMessage;
            RunInUiThread.async(() -> {
                PyDialogHelpers.openCritical("Error", msg);
            });
            Log.log(msg);
            return null;
        };

        while (!done) {
            String contents;
            try {
                if ((contents = httpProtocol.readContents(in, onUnexpectedMessage)) == null) {
                    done = true;
                } else {
                    if (contents.length() > 0) {
                        processCommand(contents.toString());
                    }
                }
            } catch (Exception e1) {
                done = true;
                //that's ok, it means that the client finished
                if (DEBUG) {
                    e1.printStackTrace();
                }
            }

            if (done || socket == null || !socket.isConnected()) {
                AbstractDebugTarget target = remote;

                if (target != null) {
                    target.terminate();
                }
                done = true;
            }
        }
    }

}
