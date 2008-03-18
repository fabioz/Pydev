package org.python.pydev.dltk.console.ui.internal;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.dltk.console.ui.IConsoleStyleProvider;

/**
 * Interface created just so that we can test the ScriptConsoleDocument listener (with the interfaces
 * it relies from the IScriptConsoleViewer2
 */
public interface IScriptConsoleViewer2ForDocumentListener {

    void setCaretPosition(int length);

    IConsoleStyleProvider getStyleProvider();

    IDocument getDocument();

    void revealEndOfDocument();

}
