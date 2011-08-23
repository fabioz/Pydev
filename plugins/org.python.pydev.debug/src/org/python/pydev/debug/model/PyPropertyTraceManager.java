package org.python.pydev.debug.model;

import org.python.pydev.core.ListenerList;
import org.python.pydev.debug.core.ConfigureExceptionsFileUtils;

public class PyPropertyTraceManager {

	// Static variables
	public static final String PROPERTY_TRACE_STATE = "property_trace_state.prefs";

	private static PyPropertyTraceManager pyPropertyTraceManager;

	// For instance
	private ListenerList<IPropertyTraceListener> listeners = new ListenerList<IPropertyTraceListener>(
			IPropertyTraceListener.class);

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

	public void setPyPropertyTraceState(boolean replaceProperty,
			boolean disableGetterTrace, boolean disableSetterTrace,
			boolean disableDelterTrace) {

		String propertyTrace = Boolean.toString(replaceProperty)
				+ ConfigureExceptionsFileUtils.DELIMITER
				+ Boolean.toString(disableGetterTrace)
				+ ConfigureExceptionsFileUtils.DELIMITER
				+ Boolean.toString(disableSetterTrace)
				+ ConfigureExceptionsFileUtils.DELIMITER
				+ Boolean.toString(disableDelterTrace);
		ConfigureExceptionsFileUtils.writeToFile(
				PyPropertyTraceManager.PROPERTY_TRACE_STATE, propertyTrace,
				false);

		for (IPropertyTraceListener listener : this.listeners.getListeners()) {
			listener.onSetPropertyTraceConfiguration();
		}
	}

	// Listeners

	public void addListener(IPropertyTraceListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(IPropertyTraceListener listener) {
		this.listeners.remove(listener);
	}
}
