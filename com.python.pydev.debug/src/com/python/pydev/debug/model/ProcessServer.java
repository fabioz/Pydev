package com.python.pydev.debug.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.python.pydev.debug.remote.RemoteDebuggerServer;

public class ProcessServer extends Process {	
	private ByteArrayInputStream inputStream;
	private ByteArrayInputStream errorStream;
	private ByteArrayOutputStream outputStream;	
	private Object lock;	
	
	public ProcessServer() {
		super();
		inputStream = new ByteArrayInputStream("Debug Server".getBytes());
		errorStream = new ByteArrayInputStream(new byte[0]);
		outputStream = new ByteArrayOutputStream();
		
		lock = new Object();
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
		if( !RemoteDebuggerServer.getInstance().isTerminated() ) {
			RemoteDebuggerServer.getInstance().stopListening();
		}
		synchronized ( lock ) {			
			lock.notify();					
		}					
	}
}
