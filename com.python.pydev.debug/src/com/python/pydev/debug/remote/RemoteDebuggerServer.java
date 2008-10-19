package com.python.pydev.debug.remote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PySourceLocator;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;

import com.python.pydev.debug.DebugPluginPrefsInitializer;
import com.python.pydev.debug.model.ProcessServer;
import com.python.pydev.debug.model.PyDebugTargetServer;

/**
 * After this class is created once, it will stay alive 'forever', as it will block in the server socket accept.
 * Note that if it for some reason exits (in the case of an exception), the thread will be recreated.
 */
public class RemoteDebuggerServer extends AbstractRemoteDebugger implements Runnable {
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
     */
    private volatile boolean terminated;
    
    /**
     * An emulation of a process, to make Eclipse happy (and so that we have somewhere to write to).
     */
    private volatile ProcessServer serverProcess;
    
    /**
     * The iprocess that is created for the debug server
     */
    private volatile IProcess iProcess;

    private volatile static int remoteDebuggerPort=-1;
    
    /**
     * This is the server
     */
    private volatile static RemoteDebuggerServer remoteServer;
    
    /**
     * The thread for the debug
     */
    private volatile static Thread remoteServerThread;
    
    private RemoteDebuggerServer() {    
    }
    
    public static synchronized RemoteDebuggerServer getInstance() {
        if(remoteDebuggerPort != DebugPluginPrefsInitializer.getRemoteDebuggerPort()){
            if(remoteServer != null){
                remoteServer.stopListening();
                remoteServer.dispose();
            }
            remoteServer = null;
            remoteServerThread = null;
        }
        if( remoteServer==null ) {
            remoteServer = new RemoteDebuggerServer();
        }
        if( remoteServerThread==null ) {
            remoteServerThread = new Thread(remoteServer);
            remoteDebuggerPort = DebugPluginPrefsInitializer.getRemoteDebuggerPort();
            if(serverSocket != null){
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            
            try {
                //System.out.println("starting at:"+remoteDebuggerPort);
                serverSocket = new ServerSocket( remoteDebuggerPort );
                serverSocket.setReuseAddress(true);
                serverSocket.setSoTimeout( TIMEOUT );

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            remoteServerThread.start();
        }
        return remoteServer;
    }
    
    public void run() {
        try {
            //the serverSocket is static, so, if it already existed, let's close it so it can be recreated.
            terminated = false;
            while( true ) {
                //will be blocked here until a client connects (or user starts in another port)
                startDebugging(serverSocket.accept());
            }
        } catch (SocketException e) {        
            //ignore (will create a new one later)
        } catch (Exception e) {        
            Log.log(e);
        }        
    }        
    
    private void startDebugging(Socket socket) throws InterruptedException {        
        try {
            Thread.sleep(1000);
            if( launch!= null ) {
                launch.setSourceLocator(new PySourceLocator());
            }
            target = new PyDebugTargetServer( launch, null, this );
            target.startTransmission(socket);
            target.initialize();
        } catch (IOException e) {        
            e.printStackTrace();
        }        
    }

    public synchronized void stopListening() {
        if(terminated){
            return;
        }
        terminated = true;
        try {
            if (launch != null && launch.canTerminate()){
                launch.terminate();
            }
        } catch (Exception e) {
            Log.log(e);
        }
        launch = null;
    }
    
    public void dispose() {
        if(launch != null){
            launch.removeDebugTarget( target );
        }
        if(target != null){
            target.terminate();
        }
        target = null;    
    }
    
    public void disconnect() throws DebugException {    
        //dispose() calls terminate() that calls disconnect()
    }

    
    public void setLaunch(ILaunch launch, ProcessServer p, IProcess pro) {
        if(this.launch != null){
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