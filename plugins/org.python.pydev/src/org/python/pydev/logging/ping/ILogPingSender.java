package org.python.pydev.logging.ping;

public interface ILogPingSender {

	/**
	 * @param pingString the string that should be posted.
	 * @return true if it was properly sent and false otherwise.
	 */
	public boolean sendPing(String pingString);
}
