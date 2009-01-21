package org.python.pydev.dltk.console.ui.internal.actions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyBackspace;

/**
 * Executes a backspace action.
 * 
 * @author fabioz
 */
public class HandleBackspaceAction {

    public void execute(IDocument doc, ITextSelection selection, int commandLineOffset) {

        
        PyBackspace pyBackspace = new PyBackspace();
        pyBackspace.setDontEraseMoreThan(commandLineOffset);
        PySelection ps = new PySelection(doc, selection);
        
        pyBackspace.perform(ps);
    }

}
