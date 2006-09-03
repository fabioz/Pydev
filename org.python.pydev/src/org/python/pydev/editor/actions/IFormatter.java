/*
 * Created on Feb 17, 2006
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;

/**
 * This interface is provided for clients that want to implement code-formatting
 */
public interface IFormatter {

    /**
     * Formats the whole doc
     */
    void formatAll(IDocument doc, PyEdit edit, int grammarVersion);

    /**
     * Formats the selection.
     */
    void formatSelection(IDocument doc, int startLine, int endLineIndex, PyEdit edit, PySelection ps);

}
