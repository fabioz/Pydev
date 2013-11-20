/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.python.pydev.debug.model.PyStackFrame;

/**
 * @author Fabio
 *
 */
public class PydevDebugConsoleFrame {

    /**
     * Last selected frame in the debug console
     */
    private PyStackFrame lastSelectedFrame;

    /**
     * By default, debug console will be linked with the selected frame
     */
    private boolean isLinkedWithDebug = true;

    /**
     * @return the currently selected / suspended frame.
     */
    public static PyStackFrame getCurrentSuspendedPyStackFrame() {
        IAdaptable context = DebugUITools.getDebugContext();

        if (context instanceof PyStackFrame) {
            PyStackFrame stackFrame = (PyStackFrame) context;
            if (!stackFrame.isTerminated() && stackFrame.isSuspended()) {
                return stackFrame;
            }
        }
        return null;
    }

    /**
     * If debug console is linked with the selected frame in debug window, then
     * it returns the current suspended frame. Otherwise it returns the frame
     * that was selected on the last line of execution.
     * 
     * @return selectedFrame in debug view
     */
    public PyStackFrame getLastSelectedFrame() {
        if (lastSelectedFrame == null) {
            lastSelectedFrame = getCurrentSuspendedPyStackFrame();
        }

        if (isLinkedWithDebug) {
            lastSelectedFrame = getCurrentSuspendedPyStackFrame();
            return lastSelectedFrame;
        } else { // Console is not linked with debug selection
            if (lastSelectedFrame == null) {
                return null;
            } else {
                if (lastSelectedFrame.getThread().isSuspended()) {
                    // Debugger is currently paused
                    return lastSelectedFrame;
                } else { // return null if debugger is not paused
                    return null;
                }
            }
        }
    }

    /**
     * Enable/Disable linking of the debug console with the suspended frame.
     * 
     * @param isLinkedWithDebug
     */
    public void linkWithDebugSelection(boolean isLinkedWithDebug) {
        this.isLinkedWithDebug = isLinkedWithDebug;
    }

}
