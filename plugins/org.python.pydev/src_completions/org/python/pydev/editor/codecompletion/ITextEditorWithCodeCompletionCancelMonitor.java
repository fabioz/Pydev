package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.IProgressMonitor;

public interface ITextEditorWithCodeCompletionCancelMonitor {

    IProgressMonitor getCancelMonitor();

}
