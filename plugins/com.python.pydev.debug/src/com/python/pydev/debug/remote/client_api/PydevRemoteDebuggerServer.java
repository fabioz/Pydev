/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.remote.client_api;

import org.python.pydev.debug.ui.launching.FileOrResource;

import com.python.pydev.debug.remote.RemoteDebuggerServer;
import com.python.pydev.debug.ui.launching.PydevdServerLaunchShortcut;

/**
 * This is the public interface for accessing the remote debugger API.
 * 
 * It provides methods to start the server, finish the server and gets its current state.
 */
public class PydevRemoteDebuggerServer {

    /**
     * This method will start the debug server.
     */
    public static void startServer() {
        RemoteDebuggerServer.getInstance().startListening(); //doing that, it will automatically start it

        PydevdServerLaunchShortcut s = new PydevdServerLaunchShortcut();
        s.launch((FileOrResource[]) null, "run");
    }

    /**
     * This method will stop the debug server.
     */
    public static void stopServer() {
        RemoteDebuggerServer.getInstance().stopListening();
    }

    /**
     * @return true if the debug server is running and false otherwise.
     */
    public static boolean isRunning() {
        RemoteDebuggerServer instance = RemoteDebuggerServer.getInstance();
        return !instance.isTerminated();
    }

}
