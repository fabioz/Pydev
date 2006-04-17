package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.net.Socket;

import org.eclipse.debug.core.DebugException;
import org.python.pydev.debug.model.AbstractDebugTarget;

public abstract class AbstractRemoteDebugger {
    /**
     * connection socket
     */
	protected Socket socket;
    
    /**
     * reading thread
     */
	protected DebuggerReader reader;
    
    /**
     * writing thread
     */
	protected DebuggerWriter writer;
    
    /**
     * sequence seed for command numbers
     */
	protected int sequence = -1;		
    
	protected AbstractDebugTarget target = null;
	
    /**
     * debugger should finish when this is called
     */
	public abstract void dispose();
    
    /**
     * debugger is disconnected when this is called
     * 
     * @throws DebugException
     */
	public abstract void disconnect() throws DebugException;

	public AbstractDebugTarget getTarget() {
		return target;
	}

	public void setTarget(AbstractDebugTarget target) {
		this.target = target;
	}
	
	/**
	 * @return next available debugger command sequence number
	 */
	public int getNextSequence() {
		sequence += 2;
		return sequence;
	}
	
	public void addToResponseQueue(AbstractDebuggerCommand cmd) {
		reader.addToResponseQueue(cmd);
	}
	
	public void postCommand(AbstractDebuggerCommand cmd) {		
		if( writer!=null ) {
			writer.postCommand(cmd);
		}
	}
	
	public void startTransmission() throws IOException {
		//socket = connector.getSocket();
		this.reader = new DebuggerReader( socket, this );		
		this.writer = new DebuggerWriter(socket);
		Thread t = new Thread(reader, "pydevd.reader");
		t.start();
		t = new Thread(writer, "pydevd.writer");
		t.start();
	}
	
}
