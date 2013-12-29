/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug.model;

import java.io.InputStream;
import java.io.OutputStream;

import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.io.PipedInputStream;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.debug.DebugPluginPrefsInitializer;
import com.python.pydev.debug.remote.RemoteDebuggerServer;

/**
 * This is the process for the remote debugger. This is a process 'mock' and only
 * one process is available for any number of clients connecting at it.
 * 
 * The stdout and stderr are 'MyPipedInputStream' objects so that we can write to it and get
 * clients listening to them to hear it.
 */
public class ProcessServer extends Process {

    private PipedInputStream inputStream;
    private PipedInputStream errorStream;
    private OutputStream outputStream;
    private Object lock;

    public ProcessServer() {
        super();
        try {

            inputStream = new PipedInputStream();
            inputStream.write(StringUtils.format("Debug Server at port: %s\r\n",
                    DebugPluginPrefsInitializer.getRemoteDebuggerPort()).getBytes());
            errorStream = new PipedInputStream();
            outputStream = new ProcessServerOutputStream();

            lock = new Object();
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
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

        //Let it manage if it was already disposed or not.
        RemoteDebuggerServer.getInstance().dispose();

        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
        } catch (Exception e) {
            Log.log(e);
        }

        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
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

    /**
     * Print something to the stdout in the server console
     */
    public void writeToStdOut(String str) {
        try {
            PipedInputStream p = inputStream;
            if (p != null) {
                p.write(str.getBytes());
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * Print something to the stdout in the server console
     */
    public void writeToStdErr(String str) {
        try {
            PipedInputStream p = errorStream;
            if (p != null) {
                p.write(str.getBytes());
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }
}
