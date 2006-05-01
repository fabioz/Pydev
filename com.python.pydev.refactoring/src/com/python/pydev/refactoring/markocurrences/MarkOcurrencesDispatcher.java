/*
 * Created on Apr 29, 2006
 */
package com.python.pydev.refactoring.markocurrences;

import java.lang.ref.WeakReference;
import java.util.ListResourceBundle;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener2;
import org.python.pydev.editor.PyEdit;

/**
 * This class dispatches the request (and gives it info if it should keep going with it) for marking ocurrences.
 * 
 * Note: We should only let it make it if the editor has the focus (otherwise we will just be wasting cicles).
 * 
 * @author Fabio
 */
public class MarkOcurrencesDispatcher implements IPyEditListener, IDocumentListener, IPyEditListener2{
    
    public void onSave(PyEdit edit) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit) {
    }

    public void onDispose(PyEdit edit) {
    }
    
    public void onSetDocument(IDocument document, PyEdit edit) {
    }

    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    public void documentChanged(DocumentEvent event) {
    }

    public void handleCursorPositionChanged(PyEdit edit) {
        System.out.println("here");
        MarkOcurrencesJob job = MarkOcurrencesJob.get();
        job.scheduleRequest(new WeakReference<PyEdit>(edit));
    }
}
