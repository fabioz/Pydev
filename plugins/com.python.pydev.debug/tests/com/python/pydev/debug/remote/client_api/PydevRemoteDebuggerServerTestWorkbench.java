/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.remote.client_api;

import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;

import com.python.pydev.debug.remote.RemoteDebuggerServer;

public class PydevRemoteDebuggerServerTestWorkbench extends AbstractWorkbenchTestCase {

    public void testDebugger() throws Exception {

        assertFalse(PydevRemoteDebuggerServer.isRunning());
        PydevRemoteDebuggerServer.startServer();
        assertTrue(PydevRemoteDebuggerServer.isRunning());
        PydevRemoteDebuggerServer.stopServer();
        assertFalse(PydevRemoteDebuggerServer.isRunning());

        PydevRemoteDebuggerServer.startServer();
        assertTrue(PydevRemoteDebuggerServer.isRunning());
        PydevRemoteDebuggerServer.startServer();
        assertTrue(PydevRemoteDebuggerServer.isRunning());

        PydevRemoteDebuggerServer.stopServer();
        assertFalse(PydevRemoteDebuggerServer.isRunning());
        PydevRemoteDebuggerServer.stopServer();
        assertFalse(PydevRemoteDebuggerServer.isRunning());

        PydevRemoteDebuggerServer.startServer();
        assertTrue(PydevRemoteDebuggerServer.isRunning());
        RemoteDebuggerServer instance = RemoteDebuggerServer.getInstance();
        instance.getServerProcess().destroy();
        assertFalse(PydevRemoteDebuggerServer.isRunning());

        PydevRemoteDebuggerServer.startServer();
        assertTrue(PydevRemoteDebuggerServer.isRunning());
        instance.stopListening();
        assertFalse(PydevRemoteDebuggerServer.isRunning());

        PydevRemoteDebuggerServer.startServer();
        assertTrue(PydevRemoteDebuggerServer.isRunning());
        instance.disconnect();
        assertFalse(PydevRemoteDebuggerServer.isRunning());

        PydevRemoteDebuggerServer.startServer();
        assertTrue(PydevRemoteDebuggerServer.isRunning());
        instance.dispose();
        assertFalse(PydevRemoteDebuggerServer.isRunning());

    }

}
