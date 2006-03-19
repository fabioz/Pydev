/*
 * Created on Mar 7, 2006
 */
package com.python.pydev.interactiveconsole;

import java.lang.ref.WeakReference;
import java.util.ListResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.AbstractIndentPrefs;

/**
 * This class will evaluate commands.
 */
public class EvaluateActionSetter implements IPyEditListener, IDocumentListener {
    
    private static final String EVALUATE_ACTION_ID = "org.python.pydev.interactiveconsole.EvaluateActionSetter";

    private ConsoleEnv fConsoleEnv;

    private IDocument doc;
    private PyEdit edit;

    public ConsoleEnv getConsoleEnv(IProject project, PyEdit edit) {
        if(fConsoleEnv == null || fConsoleEnv.isTerminated()){
            fConsoleEnv = new ConsoleEnv(project, edit.getIFile(), InteractiveConsolePreferencesPage.showConsoleInput());
        }
        return fConsoleEnv;
    }

    public void onSave(PyEdit edit) {
        //ignore
    }

    public void onCreateActions(ListResourceBundle resources, final PyEdit edit) {
        this.edit = edit;
        edit.setAction(EVALUATE_ACTION_ID, new Action() {  

            public int getAccelerator() {
                return SWT.CTRL|'\r';
            }

            public String getText() {
                return "Evaluate Python Code in Console";
            }
            
            public  void run(){
                PySelection selection = new PySelection(edit);
                String code = selection.getTextSelection().getText();
                getConsoleEnv(edit.getProject(), edit).execute(code);
            }
        });

        edit.setActionActivationCode(EVALUATE_ACTION_ID, '\r', -1, SWT.CTRL);
    }

    public void onDispose(PyEdit edit) {
        if(this.doc != null){
            this.doc.removeDocumentListener(this);
            this.doc = null;
        }
    }

    public void onSetDocument(IDocument document, PyEdit edit) {
        if(this.doc != null){
            this.doc.removeDocumentListener(this);
        }
        
        this.doc = document;

        if(this.doc != null){
            document.addDocumentListener(this);
        }
    }

    private boolean isNewLineText(IDocument document, int length, String text) {
        return length == 0 && text != null && AbstractIndentPrefs.endsWithNewline(document, text);
    }
    
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    public void documentChanged(DocumentEvent event) {
        if(isNewLineText(doc, event.fLength, event.getText())){
            if(InteractiveConsolePreferencesPage.evalOnNewLine()){
                if(fConsoleEnv != null && !fConsoleEnv.isTerminated()){
                    PySelection selection = new PySelection(edit);
                    String code = selection.getLine()+"\r\n";
                    getConsoleEnv(edit.getProject(), edit).execute(code);
                }
            }
        }
    }

}
