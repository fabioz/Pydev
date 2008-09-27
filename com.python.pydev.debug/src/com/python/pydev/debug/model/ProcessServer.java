package com.python.pydev.debug.model;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;

import com.python.pydev.debug.DebugPluginPrefsInitializer;
import com.python.pydev.debug.remote.RemoteDebuggerServer;

public class ProcessServer extends Process {    
    private InputStream inputStream;
    private InputStream errorStream;
    private ByteArrayOutputStream outputStream;    
    private Object lock;
    
    /**
     * Writing to this stream will make the data available to the inputStream.
     */
    private PipedOutputStream pipedOutputStream;    
    private PipedOutputStream pipedErrOutputStream;    
    
    public ProcessServer() {
        super();
        try {
            pipedOutputStream = new PipedOutputStream();
            pipedErrOutputStream= new PipedOutputStream();
            
            inputStream = new PipedInputStream(pipedOutputStream);
            pipedOutputStream.write(StringUtils.format("Debug Server at port: %s\r\n",DebugPluginPrefsInitializer.getRemoteDebuggerPort()).getBytes());
            pipedOutputStream.flush();
            
            
            errorStream = new PipedInputStream(pipedErrOutputStream);
            
            outputStream = new ByteArrayOutputStream();

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
        synchronized (lock ) {
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
        synchronized ( lock ) {         
            lock.notify();                  
        }                   
        
        if( !RemoteDebuggerServer.getInstance().isTerminated() ) {
            RemoteDebuggerServer.getInstance().dispose();
        }
        try {
            pipedOutputStream.close();
            pipedOutputStream = null;
        } catch (Exception e) {
            Log.log(e);
        }
        
        try {
            pipedErrOutputStream.close();
            pipedErrOutputStream = null;
        } catch (Exception e) {
            Log.log(e);
        }
        
        try {
            inputStream.close();
            inputStream = null;
        } catch (Exception e) {
            Log.log(e);
        }
        
        try {
            errorStream.close();
            errorStream = null;
        } catch (Exception e) {
            Log.log(e);
        }
    }


    /**
     * Print something to the stdout in the server console
     */
    public void writeToStdOut(String str) {
        try {
            pipedOutputStream.write(str.getBytes());
            pipedOutputStream.flush();
        } catch (Exception e) {
            Log.log(e);
        }
    }
    
    
    /**
     * Print something to the stdout in the server console
     */
    public void writeToStdErr(String str) {
        try {
            pipedErrOutputStream.write(str.getBytes());
            pipedErrOutputStream.flush();
        } catch (Exception e) {
            Log.log(e);
        }
    }
}
