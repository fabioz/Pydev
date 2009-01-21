package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author Fabio Zadrozny 
 */
public class FirstCharAction extends PyAction {
  
  /**
   * Run to the first char (other than whitespaces) or to the real first char. 
   */
  public void run(IAction action) {
    
    try{
        ITextEditor textEditor = getTextEditor();
        IDocument doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        ITextSelection selection = (ITextSelection)textEditor.getSelectionProvider().getSelection();
        
        boolean isAtFirstChar = isAtFirstVisibleChar(doc, selection.getOffset());
        if (! isAtFirstChar){
            gotoFirstVisibleChar(doc, selection.getOffset());
        }else{
            gotoFirstChar(doc, selection.getOffset());
        }
    }catch(Exception e){
        beep(e);
    }
  }
}