package org.python.pydev.debug.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

public interface IPyToggleBreakpointsTarget {

    void addBreakpointMarker(IDocument document, int line, ITextEditor fTextEditor);

}
