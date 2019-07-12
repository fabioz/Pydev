package com.python.pydev.analysis.builder;

import java.util.ListResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.shared_core.resources.DocumentChanged;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

public class MarkEditorOnSave implements IPyEditListener {

    @Override
    public void onSave(BaseEditor edit, IProgressMonitor monitor) {
        IEditorInput editorInput = edit.getEditorInput();
        if (editorInput != null) {
            IFile file = editorInput.getAdapter(IFile.class);
            if (file != null) {
                IDocument document = edit.getDocument();
                if (document != null) {
                    DocumentChanged.markSavedTimes(file, document);
                }
            }
        }
    }

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor edit, IProgressMonitor monitor) {

    }

    @Override
    public void onDispose(BaseEditor edit, IProgressMonitor monitor) {

    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor edit, IProgressMonitor monitor) {

    }

}
