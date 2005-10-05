package org.python.pydev.debug.model.remote;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.AbstractDebugTarget;

/**
 * Reads and dispatches commands
 */
public class DebuggerReader implements Runnable {
	/**
     * can be specified to debug this class 
	 */
    private static final boolean DEBUG = false;
    
    /**
     * socket we are reading
     */
    private Socket socket;
    
    /**
     * means that we are done
     */
	private boolean done = false;
    
    /**
     * commands waiting for response. Their keys are the sequence ids
     */
	private Hashtable responseQueue = new Hashtable();
    
    /**
     * we read from this
     */
	private BufferedReader in;
    
    /**
     * that's the debugger that made us... we have to finish it when we are done
     */
	private AbstractRemoteDebugger remote;
	
    /**
     * Create it
     * 
     * @param s socket we are reading from
     * @param r the debugger associated
     * 
     * @throws IOException
     */
	public DebuggerReader(Socket s, AbstractRemoteDebugger r ) throws IOException {
		remote = r;
		socket = s;
		InputStream sin = socket.getInputStream();
		in = new BufferedReader(new InputStreamReader(sin));
	}
	
    /**
     * mark things as done
     */
	public void done() {
		this.done = true;
	}

    /**
     * @param cmd
     */
	public void addToResponseQueue(AbstractDebuggerCommand cmd) {
		int sequence = cmd.getSequence();
		responseQueue.put(new Integer(sequence), cmd);
	}
	
	/**
	 * Parses & dispatches the command
	 */
	private void processCommand(String cmdLine) {
		try {
		    String[] cmdParsed = cmdLine.split("\t", 3);
            int cmdCode = Integer.parseInt(cmdParsed[0]);
            int seqCode = Integer.parseInt(cmdParsed[1]);
            String payload = URLDecoder.decode(cmdParsed[2], "UTF-8");

            
            // is there a response waiting
            AbstractDebuggerCommand cmd = (AbstractDebuggerCommand) responseQueue.remove(new Integer(seqCode));
            
            if (cmd == null){
                if ( remote.getTarget() != null){
                    remote.getTarget().processCommand(cmdParsed[0], cmdParsed[1], payload);
                } else{ 
                    PydevDebugPlugin.log(IStatus.ERROR, "internal error, command received no target", null);
                }
            } else{
                cmd.processResponse(cmdCode, payload);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	}

    /**
     * keep reading until we finish (that should happen when an exception is thrown, or if it is set as
     * done from outside)
     * 
     * @see java.lang.Runnable#run()
     */	
	public void run() {
		while (!done) {
			try {
				String cmdLine = in.readLine();
                if(cmdLine != null){                	
					processCommand(cmdLine);
				}
				Thread.sleep(50);
			} catch (Exception e1) {
                done = true;
                //that's ok, it means that the client finished
                if(DEBUG){
                    e1.printStackTrace();
                }
			}
			
            if ((socket == null) || !socket.isConnected() ) {
				AbstractDebugTarget target = remote.getTarget();
                
                if ( target != null) {
					target.debuggerDisconnected();
				}
				done = true;
			}
		}
        remote.dispose();
	}
}
