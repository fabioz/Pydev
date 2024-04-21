package org.python.pydev.core;

import java.io.File;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.ICoreTextSelection;

public interface ISourceViewerForTemplates {

    boolean isCythonFile();

    File getEditorFile();

    IIndentPrefs getIndentPrefs();

    IDocument getDocument();

    ICoreTextSelection getTextSelection();

}
