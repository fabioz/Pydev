package org.python.pydev.debug.model.remote;

import java.net.ServerSocket;
import java.net.Socket;

public abstract class AbstractListenConnector implements Runnable {

    protected int portToRead;
    protected int portToWrite;
	protected int timeout;
	protected ServerSocket serverSocket;
	protected Socket socketToWrite;	 // what got accepted
	protected Socket socketToRead;	 // what got accepted
	protected Exception e;
    
    public Socket getSocketToWrite() {
        return socketToRead;
    }
    
    public Socket getSocketToRead() {
        return socketToWrite;
    }

}
