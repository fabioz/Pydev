package com.python.pydev.analysis.builder;

import java.lang.ref.WeakReference;
import java.util.ListResourceBundle;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.shared_core.cache.LRUMap;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

public class MarkEditorOnSave implements IPyEditListener {

    public static final Map<IFile, Tuple3<Long, Long, WeakReference<IDocument>>> fileToSavedTime = new LRUMap<>(20);

    @Override
    public void onSave(BaseEditor edit, IProgressMonitor monitor) {
        IEditorInput editorInput = edit.getEditorInput();
        if (editorInput != null) {
            IFile file = editorInput.getAdapter(IFile.class);
            if (file != null) {
                IDocument document = edit.getDocument();
                if (document != null) {
                    fileToSavedTime.put(file, new Tuple3<Long, Long, WeakReference<IDocument>>(
                            ((IDocumentExtension4) document).getModificationStamp(), file.getModificationStamp(),
                            new WeakReference<>(document)));
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

    public static boolean hasDocumentChanged(IResource resource, IDocument document) {
        Tuple3<Long, Long, WeakReference<IDocument>> tuple = fileToSavedTime.get(resource);
        if (tuple == null) {
            return false;
        }
        IDocument cachedDoc = tuple.o3.get();
        if (cachedDoc == document) {
            if (((IDocumentExtension4) document).getModificationStamp() == tuple.o1) {
                return false;
            }
            return true;
        }
        return false;
    }

}
