/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.model.IProcess;
import org.python.pydev.debug.core.PydevDebugPlugin;
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
		PrintWriter out;
		boolean done;

		public Writer(Socket s) throws IOException {
			done = false;
			cmdQueue = new ArrayList();
			OutputStream sout;
			sout = s.getOutputStream();
			out = new PrintWriter(new OutputStreamWriter(sout));
		}
		
		/**
		 * Add command for processing
		 */
		public void postCommand(RemoteDebuggerCommand cmd) {
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
				RemoteDebuggerCommand cmd = null;
				synchronized (cmdQueue) {
					if (cmdQueue.size() > 0)
						cmd = (RemoteDebuggerCommand) cmdQueue.remove(0);
				}
				if (cmd != null) {
					cmd.aboutToSend();
					out.write(cmd.getOutgoing());
					out.write("\n");
					out.flush();
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
		
		public void addToResponseQueue(RemoteDebuggerCommand cmd) {
			responseQueue.put(new Integer(cmd.getSequence()), cmd);
			Object o = responseQueue.remove(new Integer(cmd.getSequence()));
			System.out.println(o.toString());
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
			payload = cmdParsed[2];
			// is there a response waiting
			RemoteDebuggerCommand cmd = (RemoteDebuggerCommand)responseQueue.remove(new Integer(seqCode));
			if (cmd == null)
				if (target != null)
					target.processCommand(cmdParsed[0], cmdParsed[1], cmdParsed[2]);
				else
					PydevDebugPlugin.log(IStatus.ERROR, "internal error, command received no target", null);
			else {
				if (cmdParsed[0].startsWith("9"))
					cmd.processErrorResponse(cmdCode, payload);	
				else
					cmd.processResponse(cmdCode, payload);
			}
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
					PydevDebugPlugin.log(IStatus.WARNING, "Unexpected termination of remote dbg input stream", e);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				if (socket.isConnected() == false)
					System.err.println("no longer connected");
			}
		}
	}

	public RemoteDebugger(PythonRunnerConfig config) {
		this.config = config;
	}
	
	public void setTarget(PyDebugTarget target) {
		this.target = target;
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
					throw new CoreException(new Status(IStatus.ERROR, PydevDebugPlugin.getPluginID(), 0, "Something got printed in the error stream", null));
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
		this.reader = new Reader(socket);
		this.writer = new Writer(socket);
		Thread t = new Thread(reader, "pydevd.reader");
		t.start();
		t = new Thread(writer, "pydevd.writer");
		t.start();
		writer.postCommand(new VersionCommand(this));
	}
	/**
	 * Dispose must be called to clean up.
	 */
	public void dispose() {
		if (connector != null)
			connector.stopListening();
		if (reader != null)
			reader.done();
		if (writer != null)
			writer.done();
		if (socket != null && socket.isConnected())
			try {
				socket.close();
			} catch (IOException e) {
				// other end might have closed first
			}
		target = null;
	}
	
	public void addToResponseQueue(RemoteDebuggerCommand cmd) {
		reader.addToResponseQueue(cmd);
	}
	
	public void postCommand(RemoteDebuggerCommand cmd) {
		writer.postCommand(cmd);
	}

}
