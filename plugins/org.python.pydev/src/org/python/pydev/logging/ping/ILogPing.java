package org.python.pydev.logging.ping;

public interface ILogPing {

	/**
	 * Whether to force the log in development mode
	 */
	boolean FORCE_SEND_WHEN_IN_DEV_MODE = false;

	void addPingOpenEditor();
	
	void addPingStartPlugin();

	/**
	 * Sends the contents to the ping server. If all went ok, clears the memory and disk-contents.
	 */
	void send();

	/**
	 * Clears in-memory contents and flushes buffered contents to file
	 */
	void stop();

}