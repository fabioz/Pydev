/*
 * Created on Apr 30, 2006
 */
package org.python.pydev.editor;

import org.python.pydev.core.docutils.PySelection;

/**
 * Used if the interface also wants to be notified of when the cursor position is changed.
 * 
 * This is an extension to the IPyEditListener
 */
public interface IPyEditListener2 {

    /**
     * Called when the cursor position changes.
     * 
     * Note: the listeners of this method should be very efficient, as in any change, it will be called.
     * 
     * @param edit the editor that had its cursor position changed.
     * @param ps the new selection (after the cursor changed its position)
     */
    void handleCursorPositionChanged(PyEdit edit, PySelection ps);
}
