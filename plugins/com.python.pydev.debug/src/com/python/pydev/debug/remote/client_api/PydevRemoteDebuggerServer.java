package com.python.pydev.debug.remote.client_api;

import org.eclipse.core.resources.IResource;

import com.python.pydev.debug.remote.RemoteDebuggerServer;
import com.python.pydev.debug.ui.launching.PydevdServerLaunchShortcut;

/**
 * This is the public interface for accessing the remote debugger API.
 * 
 * It provides methods to start the server, finish the server and gets its current state.
 */
public class PydevRemoteDebuggerServer{

    /**
     * This method will start the debug server.
     */
    public static void startServer(){
        RemoteDebuggerServer.getInstance(); //doing that, it will automatically start it
        
        PydevdServerLaunchShortcut s = new PydevdServerLaunchShortcut();
        s.launch((IResource[])null, "run");
    }
    
    /**
     * This method will stop the debug server.
     */
    public static void stopServer(){
        RemoteDebuggerServer.getInstance().stopListening();
    }

    /**
     * @return true if the debug server is running and false otherwise.
     */
    public static boolean isRunning(){
        RemoteDebuggerServer instance = RemoteDebuggerServer.getInstance();
        return !instance.isTerminated();
    }
    
    
}
