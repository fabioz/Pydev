package com.python.pydev.debug.remote;

import java.io.IOException;
import java.net.ServerSocket;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.python.pydev.debug.model.PySourceLocator;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;

import com.python.pydev.debug.model.PyDebugTargetServer;

public class RemoteDebuggerServer extends AbstractRemoteDebugger implements Runnable {
	private static final boolean DEBUG = false;
    private final int PORT = 5678;
	private final int TIMEOUT = 0;
	private ServerSocket serverSocket;
	private ILaunch launch;
	private boolean terminated;
	
	private static RemoteDebuggerServer remoteServer;;
	
	private RemoteDebuggerServer() {	
	}
	
	public static RemoteDebuggerServer getInstance() {
		if( remoteServer==null ) {
			return remoteServer = new RemoteDebuggerServer();
		}
		return remoteServer;
	}
	
	public void run() {
		try {
			serverSocket = new ServerSocket( PORT );
			terminated = false;
			while( true ) {
				serverSocket.setSoTimeout( TIMEOUT );
				socket = serverSocket.accept();
				startDebugging();
                if(DEBUG){
                    System.out.println( "PASSED" );
                }
			}
		} catch (Exception e) {		
            //that's ok, client exited
		    if(DEBUG){
		        e.printStackTrace();
            }
		}		
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
		try {
			terminated = true;
			serverSocket.close();
			if( launch.canTerminate() )
				launch.terminate();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DebugException e) {
			e.printStackTrace();
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
//		try {
//			if (socket != null) {
//				socket.shutdownInput();	// trying to make my pydevd notice that the socketToWrite is gone
//				socket.shutdownOutput();	
//				socket.close();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//			// it is going away
//		}
//		socket = null;
//		if (target != null){
//			target.debuggerDisconnected();
//        }
	}

	public void setLaunch(ILaunch launch) {
		this.launch = launch;
	}

	public boolean isTerminated() {
		return terminated;
	}		
}