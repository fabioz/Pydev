/*
 * Created on Mar 19, 2006
 */
package com.python.pydev.interactiveconsole;

import java.util.ListResourceBundle;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.AbstractIndentPrefs;

public class PyEditConsoleListener implements IPyEditListener, IDocumentListener {

    /**
     * The document we're currently observing
     */
    private IDocument doc;
    private PyEdit edit;
    private EvaluateActionSetter setter;

    public PyEditConsoleListener(EvaluateActionSetter setter, PyEdit edit) {
        this.setter = setter;
        this.edit = edit;
        this.edit.addPyeditListener(this);
        IDocument document = this.edit.getDocument();
        //start listening the doc
        onSetDocument(document, edit);
    }

    /**
     * When the editor is disposed, we have to stop listening to its doc (and remove the PyEdit reference).
     */
    public void onDispose(PyEdit edit) {
        if(this.doc != null){
            this.doc.removeDocumentListener(this);
            this.doc = null;
        }
        this.edit = null;
    }

    /**
     * Ok, we have to listen to changes in the document
     */
    public void onSetDocument(IDocument document, PyEdit edit) {
        if(this.doc != null){
            this.doc.removeDocumentListener(this);
        }
        
        this.doc = document;

        if(this.doc != null){
            document.addDocumentListener(this);
        }
    }

    /**
     * Checks if some change is a new-line change
     */
    private boolean isNewLineText(IDocument document, int length, String text) {
        return length == 0 && text != null && AbstractIndentPrefs.endsWithNewline(document, text);
    }
    
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    /**
     * When the document is changed, if it is a new line of text, we have to send it to execution
     * (if the user is in 'eval on new line' mode).
     */
    public void documentChanged(DocumentEvent event) {
        if(!isNewLineText(doc, event.fLength, event.getText())){
            return;
        }
        
        if(!InteractiveConsolePreferencesPage.evalOnNewLine()){
            return;
        }
        
        if(setter.isConsoleEnvActive(edit)){
            PySelection selection = new PySelection(edit);
            String endLineDelim = selection.getEndLineDelim();
            String code = selection.getLine();
            try {
                setter.getConsoleEnv(edit.getProject(), edit).execute(code, endLineDelim);
            } catch (UserCanceledException e) {
                //ok
            }
        }
    }

    public void onSave(PyEdit edit) {
        //ignore
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit) {
        //ignore
    }
}
