package org.python.pydev.shared_ui.editor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.python.pydev.shared_core.log.Log;

/**
 * Class to notify clients that the cursor position changed.
 */
class BaseEditorCursorListener implements MouseListener, KeyListener {

    /**
     * 
     */
    private final BaseEditor editor;

    BaseEditorCursorListener(BaseEditor editor) {
        this.editor = editor;
    }

    private int lastOffset = -1;

    public void mouseDoubleClick(MouseEvent e) {
    }

    public void mouseDown(MouseEvent e) {
    }

    /**
     * notify when the user makes a click
     */
    public void mouseUp(MouseEvent e) {
        lastOffset = getOffset();
        editor.notifyCursorPositionChanged();
    }

    public void keyPressed(KeyEvent e) {
    }

    private int getOffset() {
        return ((ITextSelection) this.editor.getSelectionProvider().getSelection()).getOffset();
    }

    /**
     * Notify when the user makes an arrow movement which actually changes the cursor position (because
     * while doing code-completion it could make that notification when the cursor was changed in the
     * dialog -- even if it didn't affect the cursor position).
     */
    public void keyReleased(KeyEvent e) {
        if (e.character == '\0') {
            try {
                int offset = getOffset();
                if (offset != lastOffset) {
                    editor.notifyCursorPositionChanged();
                    lastOffset = offset;
                }
            } catch (Exception ex) {
                Log.log(ex);
            }
        }
    }

}