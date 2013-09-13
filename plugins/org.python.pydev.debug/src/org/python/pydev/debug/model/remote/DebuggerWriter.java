/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Writer writes debugger commands to the network. Use postCommand to put new
 * ones in queue.
 */
public class DebuggerWriter implements Runnable {

    /**
     * connection socket
     */
    private Socket socket;

    /**
     * a list of RemoteDebuggerCommands
     */
    private List<AbstractDebuggerCommand> cmdQueue = new ArrayList<AbstractDebuggerCommand>();

    private OutputStreamWriter out;

    /**
     * Volatile, as multiple threads may ask it to be 'done'
     */
    private volatile boolean done = false;

    /**
     * Lock object for sleeping.
     */
    private Object lock = new Object();

    public DebuggerWriter(Socket s) throws IOException {
        socket = s;
        out = new OutputStreamWriter(s.getOutputStream());
    }

    /**
     * Add command for processing
     */
    public void postCommand(AbstractDebuggerCommand cmd) {
        synchronized (cmdQueue) {
            cmdQueue.add(cmd);
        }
    }

    public void done() {
        this.done = true;
    }

    /**
     * Loops and writes commands to the output
     */
    public void run() {
        while (!done) {
            AbstractDebuggerCommand cmd = null;
            synchronized (cmdQueue) {
                if (cmdQueue.size() > 0) {
                    cmd = (AbstractDebuggerCommand) cmdQueue.remove(0);
                }
            }
            try {
                if (cmd != null) {
                    cmd.aboutToSend();
                    out.write(cmd.getOutgoing());
                    out.write("\n");
                    out.flush();
                }
                synchronized (lock) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                done = true;
            } catch (IOException e1) {
                done = true;
            }
            if ((socket == null) || !socket.isConnected()) {
                done = true;
            }
        }
    }
}
