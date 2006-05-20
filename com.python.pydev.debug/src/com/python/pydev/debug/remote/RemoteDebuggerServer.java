package com.python.pydev.debug.remote;

import java.io.IOException;
import java.net.ServerSocket;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PySourceLocator;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;

import com.python.pydev.debug.model.ProcessServer;
import com.python.pydev.debug.model.PyDebugTargetServer;

/**
 * After this class is created once, it will stay alive 'forever', as it will block in the server socket accept.
 * Note that if it for some reason exits (in the case of an exception), the thread will be recreated.
 */
public class RemoteDebuggerServer extends AbstractRemoteDebugger implements Runnable {
    private final int PORT = 5678;
	private final int TIMEOUT = 0;
    
    /**
     * The socket that should be used to listen for clients that want a remote debug session.
     */
	private static ServerSocket serverSocket;
    
    /**
     * The launch that generated this debug server 
     */
	private ILaunch launch;
    
    /**
     * Are we terminated?
     */
	private boolean terminated;
    
    /**
     * An emulation of a process, to make Eclipse happy (and so that we have somewhere to write to).
     */
    private ProcessServer serverProcess;
    
    /**
     * The iprocess that is created for the debug server
     */
    private IProcess iProcess;
	
	private static RemoteDebuggerServer remoteServer;
	private static Thread remoteServerThread;
	
	private RemoteDebuggerServer() {	
	}
	
	public static synchronized RemoteDebuggerServer getInstance() {
		if( remoteServer==null ) {
            remoteServer = new RemoteDebuggerServer();
		}
		if( remoteServerThread==null ) {
		    remoteServerThread = new Thread(remoteServer);
		    remoteServerThread.start();
        }
		return remoteServer;
	}
	
	public void run() {
		try {
            //the serverSocket is static, so, if it already existed, let's close it so it can be recreated.
            if(serverSocket != null){
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            
			serverSocket = new ServerSocket( PORT );
            serverSocket.setReuseAddress(true);
			terminated = false;
			while( true ) {
				serverSocket.setSoTimeout( TIMEOUT );
				socket = serverSocket.accept(); //will be blocked here until a client connects
				startDebugging();
			}
		} catch (Exception e) {		
            Log.log(e);
		}		
        remoteServerThread = null;
	}		
	
	private void startDebugging() throws InterruptedException {		
		try {
			Thread.sleep(1000);
			if( launch!= null ) {
				launch.setSourceLocator(new PySourceLocator());
			}
			startTransmission();
			target = new PyDebugTargetServer( launch, null, this );
			target.initialize();
		} catch (IOException e) {		
			e.printStackTrace();
		}		
	}

	public void stopListening() {
		terminated = true;
		try {
            if (launch.canTerminate())
                launch.terminate();
        } catch (Exception e) {
            Log.log(e);
        }
		launch = null;
	}
	
	public void dispose() {
		if (writer != null) {
			writer.done();
			writer = null;
		}
		if (reader != null) {
			reader.done();
			reader = null;
		}
        try {
        	if(launch != null){
        		launch.removeDebugTarget( target );
        	}
        	if(target != null){
        		target.terminate();
        	}
        } catch (DebugException e) {
            throw new RuntimeException(e);
        }
		target = null;	
	}
	
	public void disconnect() throws DebugException {	
		//dispose() calls terminate() that calls disconnect()
	}

	public void setServerProcess(ProcessServer p) {
        this.serverProcess = p;
    }
    
	public void setLaunch(ILaunch launch) {
		this.launch = launch;
	}

	public boolean isTerminated() {
		return terminated;
	}

    public void setIProcess(IProcess pro) {
        this.iProcess = pro;
    }

    public IProcess getIProcess() {
        return this.iProcess;
    }

    public ProcessServer getServerProcess() {
        return this.serverProcess;
    }		
}