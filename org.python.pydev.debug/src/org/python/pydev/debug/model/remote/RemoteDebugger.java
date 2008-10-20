/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.net.Socket;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;

/**
 * Network interface to the remote debugger.
 */
public class RemoteDebugger extends AbstractRemoteDebugger {    
    private ListenConnector connector;    // Runnable that connects to the debugger
    private Thread connectThread;    //
    private PythonRunnerConfig config;        

    public RemoteDebugger(PythonRunnerConfig config) {
        this.config = config;
    }    

    public void startConnect(IProgressMonitor monitor) throws IOException, CoreException {
        monitor.subTask("Finding free socket...");
        connector = new ListenConnector(config.getDebugPort(), config.acceptTimeout);
        connectThread = new Thread(connector, "pydevd.connect");
        connectThread.start();
    }
    
    /**
     * Wait for the connection to the debugger to complete.
     * 
     * If this method returns without an exception, we've connected.
     * @return the socket that was connected (or null if cancelled)
     */
    public Socket waitForConnect(IProgressMonitor monitor, Process p, IProcess ip) throws Exception {
        // Launch the debug listener on a thread, and wait until it completes
        while (connectThread.isAlive()) {
            if (monitor.isCanceled()) {
                connector.stopListening();
                p.destroy();
                return null;
            }
            try {
                p.exitValue(); // throws exception if process has terminated
                // process has terminated - stop waiting for a connection
                connector.stopListening(); 
                String errorMessage= ip.getStreamsProxy().getErrorStreamMonitor().getContents();
                if (errorMessage.length() != 0){
                    // not sure if this is really an error
                    throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Something got printed in the error stream", null));
                }
            } catch (IllegalThreadStateException e) {
                // expected while process is alive
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        
        Exception connectorException = connector.getException();
		if (connectorException != null){
            throw connectorException;
        }
        return connector.getSocket();
    }
    

    
    public void disconnect() {
        dispose();
    }
    
    /**
     * Dispose must be called to clean up.
     * Because we call this from PyDebugTarget.terminate, we can be called multiple times
     * But, once dispose() is called, no other calls will be made.
     */
    public void dispose() {
        if (connector != null) {
            connector.stopListening();
            connector = null;
        }
        for (AbstractDebugTarget target : targets) {
            target.terminate();
        }
    }

}
