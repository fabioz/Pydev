package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;

public class ListenConnector extends AbstractListenConnector {
    boolean terminated;
    
    public ListenConnector(int port, int timeout) throws IOException {
        this.port = port;
        this.timeout = timeout;
        serverSocket = new ServerSocket(port);
    }
    
    Exception getException() {
        return e;
    }

    public Socket getSocket() {
        return socket;
    }

    public void stopListening() {
        if (serverSocket != null)
            try {
                serverSocket.close();
            } catch (IOException e) {
                PydevDebugPlugin.log(IStatus.WARNING, "Error closing pydevd socket", e);
            }
        terminated = true;
    }

    public void run() {
        try {
            serverSocket.setSoTimeout(timeout);
            socket = serverSocket.accept();
        }
        catch (IOException e) {
            this.e = e;
        }
    }
}
