/*
 * Author: atotic
 * Created on Mar 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Network interface to the remote debugger.
 */
public class RemoteDebugger implements Runnable {

	// socket communication
	Socket socket;
	PrintWriter toServer;
	BufferedReader fromServer;
	InputStream sin;
	
	ArrayList commands = new ArrayList(); // command queue
	
	public RemoteDebugger(Socket socket) throws IOException {
		this.socket = socket;
		OutputStream sout = socket.getOutputStream();
		sin = socket.getInputStream();
		fromServer = new BufferedReader(new InputStreamReader(sin));
		toServer = new PrintWriter(new OutputStreamWriter(sout));
	}

	public void sendCommand(RemoteDebuggerCommand command) {
		synchronized(commands) {
			commands.add(command);
		}
	}
	
	private void execute1Command(RemoteDebuggerCommand c) {
		String sendThis = c.getXMLMessage();
		toServer.write(sendThis);
		// TODO process result
	}
	
	/**
	 * 
	 * @param commandList - a list of RemoteDebuggerCommand's to be executed
	 */	
	private void executeCommands(ArrayList commandList) {
		Iterator iter = commandList.iterator();
		while (!iter.hasNext()) {
			execute1Command((RemoteDebuggerCommand)iter.next());
		}
	}
	/** 
	 * Execute commands in an infinite loop.
	 */
	public void run() {
		try {
			while (!socket.isClosed()) {
				if (!commands.isEmpty()) {
					ArrayList toExecute = new ArrayList();
					synchronized(commands) {
						while (!commands.isEmpty())
							toExecute.add(commands.remove(0));			
					}
					executeCommands(toExecute);
				}
				if (fromServer.ready()) {
					char[] cbuf = new char[2048];
					int howMany = fromServer.read(cbuf);
					System.out.print(new String(cbuf, 0, howMany));
				}
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO handle this
			e.printStackTrace();
		}
	}

}
