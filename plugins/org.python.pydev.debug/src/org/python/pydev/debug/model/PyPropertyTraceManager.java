package org.python.pydev.debug.model;

import org.python.pydev.core.ListenerList;
import org.python.pydev.debug.core.ConfigureExceptionsFileUtils;

public class PyPropertyTraceManager {

	// Static variables
	private static final String PROPERTY_TRACE_STATE = "property_trace_state.prefs";

	private static PyPropertyTraceManager pyPropertyTraceManager;

	// For instance
	private ListenerList<IExceptionsBreakpointListener> listeners = new ListenerList<IExceptionsBreakpointListener>(
			IExceptionsBreakpointListener.class);

	/**
	 * Singleton: private constructor.
	 */
	private PyPropertyTraceManager() {

	}

	public static synchronized PyPropertyTraceManager getInstance() {
		if (pyPropertyTraceManager == null) {
			pyPropertyTraceManager = new PyPropertyTraceManager();
		}
		return pyPropertyTraceManager;
	}

	// Getters

	public String getPyPropertyTraceState() {
		return ConfigureExceptionsFileUtils
				.readFromMetadataFile(PROPERTY_TRACE_STATE);
	}
}
