/*
 * Created on Apr 8, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;

/**
 * @author Fabio Zadrozny
 */
public class PyClosePar extends PyAction{

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
		try 
		{
			PySelection ps = new PySelection ( getTextEditor ( ), false );
		    String endLineDelim = ps.endLineDelim;
			IDocument doc = ps.doc;
			performClosePar(doc, ps.cursorLine, ps.absoluteCursorOffset);

			TextSelection sel = new TextSelection(doc, ps.absoluteCursorOffset+1, 0);
            getTextEditor().getSelectionProvider().setSelection(sel);
		} 
		catch ( Exception e ) 
		{
			beep ( e );
		}		
    }

    /**
     * @param doc
     * @param cursorLine
     * @param absoluteCursorOffset
     * @throws BadLocationException
     */
    public void performClosePar(IDocument doc, int cursorLine, int cursorOffset) throws BadLocationException {
        String line = PySelection.getLine(doc, cursorLine);
        
        try {
            if (shouldMaybeEatPar(line)) {
                if(doc.get(cursorOffset, 1).equals(")"))
                    return;
            }
        } catch (Exception e) {
        }
        doc.replace(cursorOffset, 0, ")");
    }
    
    /**
     * @param line
     * @return
     */
    private boolean shouldMaybeEatPar(String line) {
        int i = PyAction.countChars('(', line);
        int j = PyAction.countChars(')', line);
        
        if(j < i){
            return false;
        }
        
        return true;
    }


}
