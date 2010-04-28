package org.python.pydev.logging.ping;

/**
 * Interface used by the SynchedLogPing to provide some information (which can be changed for testing 
 * purposes).
 */
public interface ILogPingProvider {

	long getCurrentTime();

	String getApplicationId();

}
