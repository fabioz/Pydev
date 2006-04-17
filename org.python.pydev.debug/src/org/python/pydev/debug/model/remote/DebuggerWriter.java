package org.python.pydev.debug.model.remote;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Writer writes debugger commands to the network.
 * Use postCommand to put new ones in queue.
 */
public class DebuggerWriter implements Runnable {
    
    /**
     * connection socket
     */
	private Socket socket;
    
    /**
     * a list of RemoteDebuggerCommands
     */
	private ArrayList cmdQueue = new ArrayList();
	private OutputStreamWriter out;
	private boolean done = false;

	public DebuggerWriter(Socket s) throws IOException {
		socket = s;
		out = new OutputStreamWriter(s.getOutputStream());
	}
	
	/**
	 * Add command for processing
	 */
	public void postCommand(AbstractDebuggerCommand cmd) {
		synchronized(cmdQueue) {
			cmdQueue.add(cmd);
		}
	}
	
	public void done() {
		this.done = true;
	}

	/**
	 * Loops and writes commands to the output
	 */
	public void run() {
		while (!done) {
			AbstractDebuggerCommand cmd = null;
			synchronized (cmdQueue) {
				if (cmdQueue.size() > 0)
					cmd = (AbstractDebuggerCommand) cmdQueue.remove(0);
			}
			try {
				if (cmd != null) {
					cmd.aboutToSend();
						out.write(cmd.getOutgoing());
						out.write("\n");
						out.flush();
				}
				synchronized (this) {
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				done = true;
			} catch (IOException e1) {
				done = true;
			}
			if ((socket == null) || !socket.isConnected()) {
				done = true;
			}
		}
	}
}
