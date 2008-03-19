package org.python.pydev.dltk.console.ui.internal.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Deletes the previous word (ctrl+backspace)
 * 
 * @author fabioz
 */
public class HandleDeletePreviousWord {
	
	public void execute(IDocument doc, int caretPosition, int commandLineOffset) {
        int initialCaretPosition = caretPosition;
        //remove all whitespaces
        while(caretPosition > commandLineOffset){
        	try {
				char c = doc.getChar(caretPosition-1);
				if(!Character.isWhitespace(c)){
					break;
				}
				caretPosition-=1;
			} catch (BadLocationException e) {
				break;
			}
        }

        //remove a word
        while(caretPosition > commandLineOffset){
        	try {
        		char c = doc.getChar(caretPosition-1);
        		if(!Character.isJavaIdentifierPart(c)){
        			break;
        		}
        		caretPosition-=1;
        	} catch (BadLocationException e) {
        		break;
        	}
        }
        
        if (initialCaretPosition == caretPosition && initialCaretPosition > commandLineOffset){
        	caretPosition = initialCaretPosition -1;
        }
        
        try {
			doc.replace(caretPosition, initialCaretPosition-caretPosition, "");
		} catch (BadLocationException e) {
			PydevPlugin.log(e);
		}
        
	}

}
