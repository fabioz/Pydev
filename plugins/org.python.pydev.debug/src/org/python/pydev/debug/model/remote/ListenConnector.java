/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.shared_core.net.SocketUtil;


public class ListenConnector implements Runnable {

    protected int timeout;
    protected ServerSocket serverSocket;
    protected Socket socket; // what got accepted
    protected Exception e;

    public ListenConnector(int timeout) throws IOException {
        this.timeout = timeout;
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            Log.log("Error when creating server socket.", e);
            throw e;
        }
    }

    Exception getException() {
        return e;
    }

    public Socket getSocket() {
        return socket;
    }

    public void stopListening() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                PydevDebugPlugin.log(IStatus.WARNING, "Error closing pydevd socket", e);
            }
            serverSocket = null;
        }
    }

    public void run() {
        try {
            serverSocket.setSoTimeout(timeout);
            socket = serverSocket.accept();
        } catch (IOException e) {
            this.e = e;
        }
    }

    public int getLocalPort() throws IOException {
        int localPort = serverSocket.getLocalPort();
        SocketUtil.checkValidPort(localPort);
        return localPort;
    }

    @Override
    protected void finalize() throws Throwable {
        //Clear resources when garbage-collected.
        try {
            this.stopListening();
        } catch (Throwable e) {
            //Never fail!
            PydevDebugPlugin.log(IStatus.WARNING, "Error finalizing ListenConnector", e);
        }
    }
}
