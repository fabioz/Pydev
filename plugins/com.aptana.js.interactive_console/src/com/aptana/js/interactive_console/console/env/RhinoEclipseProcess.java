/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console.env;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.aptana.js.interactive_console.rhino.RhinoConsoleMain;
import com.aptana.shared_core.io.PipedInputStream;
import com.aptana.shared_core.log.Log;

/**
 * Process used so that we can create an interactive console using the eclipse
 * IDE itself.
 * 
 * Should be handy when experimenting with Eclipse, but can potentially halt the
 * IDE depending on what's done.
 */
public class RhinoEclipseProcess extends Process {

    private PipedInputStream outputStream;
    private PipedInputStream errorStream;
    private Object lock;
    private Thread thread;

    public RhinoEclipseProcess(final int port,
            final int clientPort) {
        super();
        try {

            outputStream = new PipedInputStream();
            errorStream = new PipedInputStream();

            lock = new Object();

            thread = new Thread() {
                public void run() {
                    RhinoConsoleMain rhinoConsoleMain = new RhinoConsoleMain();
                    try {
                        rhinoConsoleMain.setErr(errorStream.internalOutputStream);
                        rhinoConsoleMain.setOut(outputStream.internalOutputStream);
                        rhinoConsoleMain.startXmlRpcServer(port);
                    } catch (IOException e) {
                        Log.log(e);
                    }
                };
            };
            thread.start();

        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }
    }

    public OutputStream getOutputStream() {

        return outputStream.internalOutputStream;
    }

    public InputStream getInputStream() {

        return outputStream;
    }

    public InputStream getErrorStream() {

        return errorStream;
    }

    @Override
    public int waitFor() throws InterruptedException {
        synchronized (lock) {
            lock.wait();
        }

        return 0;
    }

    @Override
    public int exitValue() {
        throw new IllegalThreadStateException();
    }

    @Override
    public void destroy() {
        synchronized (lock) {
            lock.notify();
        }

        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (Exception e) {
            Log.log(e);
        }

        try {
            if (errorStream != null) {
                errorStream.close();
                errorStream = null;
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

}
