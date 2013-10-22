/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.AbstractDebugTargetWithTransmission;


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
     * Lock object for sleeping.
     */
    private Object lock = new Object();

    /**
     * commands waiting for response. Their keys are the sequence ids
     */
    private Dictionary<Integer, AbstractDebuggerCommand> responseQueue = new Hashtable<Integer, AbstractDebuggerCommand>();

    /**
     * we read from this
     */
    private InputStreamReader in;

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
        InputStream sin = socket.getInputStream();
        in = new InputStreamReader(sin);
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
            responseQueue.put(new Integer(sequence), cmd);
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
            String payload = URLDecoder.decode(cmdParsed[2], "UTF-8");

            // is there a response waiting
            AbstractDebuggerCommand cmd;
            synchronized (responseQueue) {
                cmd = (AbstractDebuggerCommand) responseQueue.remove(new Integer(seqCode));
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
    public void run() {
        while (!done) {
            try {
                String cmdLine = readLine();
                if (DEBUG) {
                    System.err.println("receive cmd: " + cmdLine);
                }
                if (cmdLine != null && cmdLine.trim().length() > 0) {
                    processCommand(cmdLine);
                }
                synchronized (lock) {
                    Thread.sleep(50);
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

    /**
     * Implemented our own: with the BufferedReader, when the socket was closed, it still appeared stuck in the method.
     * 
     * @return a line that was read from the debugger.
     * @throws IOException
     */
    private String readLine() throws IOException {
        StringBuffer contents = new StringBuffer();
        int i;
        while ((i = in.read()) != -1) {
            char c = (char) i;
            if (c == '\n' || c == '\r') {
                return contents.toString();
            }
            contents.append(c);
        }
        throw new IOException("Done");
    }
}
