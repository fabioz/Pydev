package org.python.pydev.debug.model.remote;

import java.net.ServerSocket;
import java.net.Socket;

public abstract class AbstractListenConnector implements Runnable {

    protected int port;
    protected int timeout;
    protected ServerSocket serverSocket;
    protected Socket socket;     // what got accepted
    protected Exception e;
    
}
