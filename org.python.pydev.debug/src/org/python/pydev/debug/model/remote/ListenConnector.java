package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;

public class ListenConnector extends AbstractListenConnector {
	boolean terminated;
	
	public ListenConnector(int portToWrite, int portToRead, int timeout) throws IOException {
		this.portToRead = portToRead;
		this.portToWrite = portToWrite;
		this.timeout = timeout;
		serverSocket = new ServerSocket(portToWrite);
	}
	
	Exception getException() {
		return e;
	}

	public void stopListening() {
		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (IOException e) {
				PydevDebugPlugin.log(IStatus.WARNING, "Error closing pydevd socketToWrite", e);
			}
		terminated = true;
	}

	public void run() {
		try {
			serverSocket.setSoTimeout(timeout);
			socketToWrite = serverSocket.accept();
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            socketToRead = new Socket("localhost", portToRead);
		}
		catch (IOException e) {
			this.e = e;
		}
	}

}
