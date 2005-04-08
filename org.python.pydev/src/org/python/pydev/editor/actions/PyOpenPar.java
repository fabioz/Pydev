/*
 * Created on Apr 7, 2005
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
public class PyOpenPar extends PyAction{

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
		try 
		{
			PySelection ps = new PySelection ( getTextEditor ( ), false );
		    String endLineDelim = ps.endLineDelim;
			IDocument doc = ps.doc;
			performOpenPar(doc, ps.cursorLine, ps.absoluteCursorOffset);

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
     * @param 
     * @param 
     * @throws BadLocationException
     */
    public void performOpenPar(IDocument doc, int cursorLine, int cursorOffset) throws BadLocationException {
        String line = PySelection.getLine(doc, cursorLine);
        
        if(shouldClose(line)){
        
	        if(line.indexOf(":") == -1 && (line.indexOf("class") != -1 || line.indexOf("def") != -1)){
	            doc.replace(cursorOffset, 0, "():");
	        }else{
	            doc.replace(cursorOffset, 0, "()");
	        }
        }
        else{
            
            doc.replace(cursorOffset, 0, "(");
        }
    }

    /**
     * @param line
     * @return
     */
    private boolean shouldClose(String line) {
        int i = PyAction.countChars('(', line);
        int j = PyAction.countChars(')', line);
        
        if(j > i){
            return false;
        }
        
        return true;
    }

}
