package org.python.pydev.debug.model;

/**
 * @author hussain.bohra
 */
public interface IPropertyTraceListener {

	/**
     * Called when user disable/re-enable property tracing
     */
    void onSetPropertyTraceConfiguration();

}
