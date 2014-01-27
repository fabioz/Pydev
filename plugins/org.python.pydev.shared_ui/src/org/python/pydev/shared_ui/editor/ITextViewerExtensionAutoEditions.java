package org.python.pydev.shared_ui.editor;

import org.eclipse.jface.text.ITextViewer;

public interface ITextViewerExtensionAutoEditions extends ITextViewer {

    boolean getAutoEditionsEnabled();

    void setAutoEditionsEnabled(boolean b);

}
