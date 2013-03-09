package org.python.pydev.debug.newconsole;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.AbstractHandleBackspaceAction;

/**
 * Executes a backspace action.
 * 
 * @author fabioz
 */
public class HandleBackspaceAction extends AbstractHandleBackspaceAction {

    public void execute(IDocument doc, ITextSelection selection, int commandLineOffset) {

        PyBackspace pyBackspace = new PyBackspace();
        pyBackspace.setDontEraseMoreThan(commandLineOffset);
        PySelection ps = new PySelection(doc, selection);

        pyBackspace.perform(ps);
    }

}
