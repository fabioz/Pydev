/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model.remote;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;

/**
 * Network interface to the remote debugger.
 */
public class RemoteDebugger extends Object {

	private int sequence = -1;		// sequence seed for command numbers

	private Socket socket;	// connection socket
	private Reader reader;	// reading thread
	private Writer writer;	// writing thread
	private ListenConnector connector;	// Runnable that connects to the debugger
	private Thread connectThread;	//
	private PythonRunnerConfig config;	
	private PyDebugTarget target = null;
	
	protected class ListenConnector implements Runnable {
		int port;
		int timeout;
		ServerSocket serverSocket;
		Socket socket;	 // what got accepted
		Exception e;

		boolean terminated;
		
		public ListenConnector(int port, int timeout) throws IOException {
			this.port = port;
			this.timeout = timeout;
			serverSocket = new ServerSocket(port);
		}
		
		Exception getException() {
			return e;
		}
	
		public Socket getSocket() {
			return socket;
		}

		public void stopListening() {
			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					PydevDebugPlugin.log(IStatus.WARNING, "Error closing pydevd socket", e);
				}
			terminated = true;
		}
	
		public void run() {
			try {
				serverSocket.setSoTimeout(timeout);
				socket = serverSocket.accept();
			}
			catch (IOException e) {
				this.e = e;
			}
		}
	}

	/**
	 * Writer writes debugger commands to the network.
	 * Use postCommand to put new ones in queue.
	 */
	protected class Writer implements Runnable {
		ArrayList cmdQueue;	// a list of RemoteDebuggerCommands
		OutputStreamWriter out;
		boolean done;

		public Writer(Socket s) throws IOException {
			done = false;
			cmdQueue = new ArrayList();
			OutputStream sout;
			sout = s.getOutputStream();
			out = new OutputStreamWriter(sout);
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
					Thread.sleep(100);
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
	
	/**
	 * Reads and dispatches commands
	 */
	protected class Reader implements Runnable {
		boolean done;
		Hashtable responseQueue;	// commands waiting for response. Their keys are the sequence ids
		BufferedReader in;
		
		public Reader(Socket socket) throws IOException {
			done = false;
			responseQueue = new Hashtable();
			InputStream sin = socket.getInputStream();
			in = new BufferedReader(new InputStreamReader(sin));
		}
		
		public void done() {
			this.done = true;
		}
		
		public void addToResponseQueue(AbstractDebuggerCommand cmd) {
			responseQueue.put(new Integer(cmd.getSequence()), cmd);
			Object o = responseQueue.remove(new Integer(cmd.getSequence()));
			responseQueue.put(new Integer(cmd.getSequence()), cmd);
		}
		
		/**
		 * Parses & dispatches the command
		 */
		private void processCommand(String cmdLine) {
			int cmdCode;
			int seqCode;
			String payload;
			String[] cmdParsed = cmdLine.split("\t", 3);
			cmdCode = Integer.parseInt(cmdParsed[0]);
			seqCode = Integer.parseInt(cmdParsed[1]);
			payload = URLDecoder.decode(cmdParsed[2]);
			// is there a response waiting
			AbstractDebuggerCommand cmd = (AbstractDebuggerCommand)responseQueue.remove(new Integer(seqCode));
			if (cmd == null)
				if (target != null)
					target.processCommand(cmdParsed[0], cmdParsed[1], payload);
				else
					PydevDebugPlugin.log(IStatus.ERROR, "internal error, command received no target", null);
			else 
				cmd.processResponse(cmdCode, payload);
		}

		public void run() {
			while (!done) {
				try {
					if (in.ready()) {
						String cmdLine = in.readLine();
						processCommand(cmdLine);
					}
					Thread.sleep(100);
				} catch (IOException e) {
					done = true;
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				if ((socket == null) || !socket.isConnected()) {
					if (target != null) {
						target.debuggerDisconnected();
					}
					done = true;
				}
			}
		}
	}

	public RemoteDebugger(PythonRunnerConfig config) {
		this.config = config;
	}
	
	public void setTarget(PyDebugTarget target) {
		this.target = target;
	}
	
	public void startTransmission() throws IOException {
		this.reader = new Reader(socket);
		this.writer = new Writer(socket);
		Thread t = new Thread(reader, "pydevd.reader");
		t.start();
		t = new Thread(writer, "pydevd.writer");
		t.start();
	}

	/**
	 * @return next available debugger command sequence number
	 */
	public int getNextSequence() {
		sequence += 2;
		return sequence;
	}

	public void startConnect(IProgressMonitor monitor) throws IOException, CoreException {
		monitor.subTask("Finding free socket...");
		connector = new ListenConnector(config.getDebugPort(), config.acceptTimeout);
		connectThread = new Thread(connector, "pydevd.connect");
		connectThread.start();
	}
	
	/**
	 * Wait for the connection to the debugger to complete.
	 * 
	 * If this method returns without an exception, we've connected.
	 * @return true if operation was cancelled by user
	 */
	public boolean waitForConnect(IProgressMonitor monitor, Process p, IProcess ip) throws Exception {
		// Launch the debug listener on a thread, and wait until it completes
		while (connectThread.isAlive()) {
			if (monitor.isCanceled()) {
				connector.stopListening();
				p.destroy();
				return true;
			}
			try {
				p.exitValue(); // throws exception if process has terminated
				// process has terminated - stop waiting for a connection
				connector.stopListening(); 
				String errorMessage= ip.getStreamsProxy().getErrorStreamMonitor().getContents();
				if (errorMessage.length() != 0)
					// not sure if this is really an error
					throw new CoreException(PydevDebugPlugin.makeStatus(IStatus.ERROR, "Something got printed in the error stream", null));
			} catch (IllegalThreadStateException e) {
				// expected while process is alive
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		if (connector.getException() != null)
			throw connector.getException();
		connected(connector.getSocket());
		return false;
	}
	
	/**
	 * Remote debugger has connected
	 */
	public void connected(Socket socket) throws IOException  {
		this.socket = socket;
	}
	
	public void disconnect() {
		try {
			if (socket != null) {
				socket.shutdownInput();	// trying to make my pydevd notice that the socket is gone
				socket.shutdownOutput();	
				socket.close();
			}
			} catch (IOException e) {
				e.printStackTrace();
				// it is going away
			}
		socket = null;
		if (target != null)
			target.debuggerDisconnected();
	}
	
	/**
	 * Dispose must be called to clean up.
	 * Because we call this from PyDebugTarget.terminate, we can be called multiple times
	 * But, once dispose() is called, no other calls will be made.
	 */
	public void dispose() {
		if (connector != null) {
			connector.stopListening();
			connector = null;
		}
		if (writer != null) {
			writer.done();
			writer = null;
		}
		if (reader != null) {
			reader.done();
			reader = null;
		}
		disconnect();
		target = null;
	}
	
	public void addToResponseQueue(AbstractDebuggerCommand cmd) {
		reader.addToResponseQueue(cmd);
	}
	
	public void postCommand(AbstractDebuggerCommand cmd) {
		writer.postCommand(cmd);
	}

}
