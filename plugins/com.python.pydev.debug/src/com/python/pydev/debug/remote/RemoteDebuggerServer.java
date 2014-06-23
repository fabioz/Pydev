/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.remote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PySourceLocator;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.shared_core.callbacks.ListenerList;

import com.python.pydev.debug.DebugPluginPrefsInitializer;
import com.python.pydev.debug.model.ProcessServer;
import com.python.pydev.debug.model.PyDebugTargetServer;

/**
 * After this class is created once, it will stay alive 'forever', as it will block in the server socket accept.
 * Note that if it for some reason exits (in the case of an exception), the thread will be recreated.
 */
public class RemoteDebuggerServer extends AbstractRemoteDebugger implements Runnable {

    /**
     * 0 == infinite timeout.
     */
    private final static int TIMEOUT = 0;

    /**
     * The socket that should be used to listen for clients that want a remote debug session.
     */
    private volatile static ServerSocket serverSocket;

    /**
     * The launch that generated this debug server
     */
    private volatile ILaunch launch;

    /**
     * Are we terminated?
     * (starts as if it was terminated)
     */
    private volatile boolean terminated = true;

    /**
     * An emulation of a process, to make Eclipse happy (and so that we have somewhere to write to).
     */
    private volatile ProcessServer serverProcess;

    /**
     * The iprocess that is created for the debug server
     */
    private volatile IProcess iProcess;

    /**
     * Identifies if we're in the middle of a dispose operation (prevent recursive calls).
     */
    private volatile boolean inDispose = false;

    /**
     * Identifies if we're in the middle of a stop listening operation (prevent recursive calls).
     */
    private volatile boolean inStopListening = false;

    /**
     * This is the server
     */
    private volatile static RemoteDebuggerServer remoteServer;

    /**
     * The thread for the debug.
     */
    private volatile static Thread remoteServerThread;

    /**
     * Helper to make locking.
     */
    private static final Object lock = new Object();

    private ListenerList<IRemoteDebuggerListener> listeners = new ListenerList<>(IRemoteDebuggerListener.class);

    public void addListener(IRemoteDebuggerListener listener) {
        listeners.add(listener);
    }

    /**
     * Private (it's a singleton)
     */
    private RemoteDebuggerServer() {
    }

    public static RemoteDebuggerServer getInstance() {
        synchronized (lock) {
            if (remoteServer == null) {
                remoteServer = new RemoteDebuggerServer();
            }
            return remoteServer;
        }
    }

    public void startListening() {
        synchronized (lock) {
            boolean notify = false; //Don't notify listeners of a stop as we'll restart shortly.
            stopListening(notify); //Stops listening if it's currently listening...

            if (serverSocket == null) {
                try {
                    final int port = DebugPluginPrefsInitializer.getRemoteDebuggerPort();
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.setSoTimeout(TIMEOUT);
                    serverSocket.bind(new InetSocketAddress(port));
                } catch (Throwable e) {
                    Log.log(e);
                }
            }

            if (remoteServerThread == null) {
                remoteServerThread = new Thread(remoteServer);
                remoteServerThread.start();
            }
        }
    }

    public void run() {
        try {
            while (true) {
                //will be blocked here until a client connects (or when the socket is closed)
                startDebugging(serverSocket.accept());
            }
        } catch (SocketException e) {
            //ignore (will create a new one later)
        } catch (Exception e) {
            Log.log(e);
        } finally {
            remoteServerThread = null;
        }
    }

    private void startDebugging(Socket socket) throws InterruptedException {
        try {
            Thread.sleep(1000);
            if (launch != null) {
                launch.setSourceLocator(new PySourceLocator());
            }
            PyDebugTargetServer target = new PyDebugTargetServer(launch, null, this);
            target.startTransmission(socket);
            target.initialize();
            this.addTarget(target);
        } catch (IOException e) {
            Log.log(e);
        }
    }

    public void stopListening() {
        stopListening(true);
    }

    private void stopListening(boolean notify) {
        synchronized (lock) {
            if (terminated || this.inStopListening) {
                return;
            }
            this.inStopListening = true;
            try {
                terminated = true;
                try {
                    if (launch != null && launch.canTerminate()) {
                        launch.terminate();
                    }

                    remoteServer.dispose();

                    if (serverSocket != null) {
                        try {
                            serverSocket.close();
                        } catch (Throwable e) {
                            Log.log(e);
                        }
                        serverSocket = null;
                    }

                } catch (Exception e) {
                    Log.log(e);
                }
                launch = null;
            } finally {
                this.inStopListening = false;
            }
        }
        IRemoteDebuggerListener[] listeners2 = listeners.getListeners();
        for (IRemoteDebuggerListener iRemoteDebuggerListener : listeners2) {
            iRemoteDebuggerListener.stopped(this);
        }
    }

    @Override
    public void dispose() {
        synchronized (lock) {
            if (this.inDispose) {
                return;
            }

            this.inDispose = true;
            try {
                this.stopListening();
                if (launch != null) {
                    for (AbstractDebugTarget target : targets) {
                        launch.removeDebugTarget(target);
                        target.terminate();
                    }
                }
                targets.clear();
            } finally {
                this.inDispose = false;
            }
        }
    }

    @Override
    public void disconnect() throws DebugException {
        //dispose() calls terminate() that calls disconnect()
        //but this calls stopListening() anyways (it's responsible for checking if
        //it's already in the middle of something)
        stopListening();
    }

    public void setLaunch(ILaunch launch, ProcessServer p, IProcess pro) {
        if (this.launch != null) {
            this.stopListening();
        }
        terminated = false; //we have a launch... so, it's not finished
        this.serverProcess = p;
        this.launch = launch;
        this.iProcess = pro;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public IProcess getIProcess() {
        return this.iProcess;
    }

    public ProcessServer getServerProcess() {
        return this.serverProcess;
    }

}