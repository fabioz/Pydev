package org.python.pydev.debug.ui;
import java.io.*;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.debug.core.PydevDebugPlugin;


/**
 * Used to receive output from processes.
 * 
 * Copied from Ant code, and used by InterpreterEditor
 */
public class StreamConsumer extends Thread {
	BufferedReader bReader;
	private String lastLine;
	public StreamConsumer(InputStream inputStream) {
		super();
		setDaemon(true);
		bReader = new BufferedReader(new InputStreamReader(inputStream));
	}
	public void run() {
		try {
			String line;
			while (null != (line = bReader.readLine())) {
				lastLine = line;
				// DebugPlugin.log(line);
			}
			bReader.close();
		} catch (IOException ioe) {
			PydevDebugPlugin.log(IStatus.ERROR, "Error in stream consumer", ioe);
		}
	}
	/**
	 * @return last line obtained, can be null
	 */
	public String getLastLine() {
		return lastLine;
	}

}