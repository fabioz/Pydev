/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.AbstractDebugTargetWithTransmission;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
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
        try {
            while (!done) {
                String contents;
                try {
                    if ((contents = readContents()) == null) {
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
        } finally {
            buffer = null;
            contents = null;
            byteArrayOutputStream = null;
        }
    }

    private byte[] buffer = new byte[32 * 1024];
    private FastStringBuffer contents = new FastStringBuffer();
    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    private String readContents() throws IOException {
        int bytesToRead = -1;

        while (true) {
            FileUtils.readLine(in, contents.clear());
            contents.trim(); // Remove the \r\n in the end.

            if (contents.length() == 0) {
                // Ok, real payload ahead.
                // Read once from stdin and print result to stdout
                if (bytesToRead == -1) {
                    Log.log("Error. pydevd did not respect protocol (Content-Length not passed in header).");
                    return null;
                }

                int bytesRead;
                while ((bytesRead = in.read(buffer, 0, Math.min(bytesToRead, buffer.length))) > 0) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    bytesToRead -= bytesRead;
                }
                byte[] bytes = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.reset();
                return new String(bytes, StandardCharsets.UTF_8);
            } else {
                // Header found
                String contentLen = "Content-Length: ";
                if (contents.startsWith(contentLen)) {
                    if (DEBUG) {
                        System.err.println("receive cmd: " + contents);
                    }
                    contents.deleteFirstChars(contentLen.length());
                    try {
                        bytesToRead = Integer.parseInt(contents.trim().toString());
                    } catch (NumberFormatException e) {
                        throw new IOException("Error getting number of bytes to load. Found: " + contents);
                    }
                } else {
                    // Unexpected header.
                    String msg = "It seems an old version of the PyDev Debugger is being used (please update the pydevd package being used).\n\nFound message:\n"
                            + contents;
                    RunInUiThread.async(() -> {
                        PyDialogHelpers.openCritical("Error", msg);
                    });
                    Log.log(msg);
                    return null;
                }
            }
        }
    }

}
