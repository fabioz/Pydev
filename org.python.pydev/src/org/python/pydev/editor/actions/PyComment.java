/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.docutils.PySelection;

/**
 * Creates a bulk comment. Comments all selected lines
 * 
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public class PyComment extends PyAction {
    /**
     * Grabs the selection information and performs the action.
     */
    public void run(IAction action) {
        try {
            // Select from text editor
            PySelection ps = new PySelection(getTextEditor());
            // Perform the action
            perform(ps);

            // Put cursor at the first area of the selection
            getTextEditor().selectAndReveal(ps.getEndLine().getOffset(), 0);
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @return boolean The success or failure of the action
     */
    public static boolean perform(PySelection ps) {
        // What we'll be replacing the selected text with
        StringBuffer strbuf = new StringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        int i;
        try {
            // For each line, comment them out
            for (i = ps.getStartLineIndex(); i < ps.getEndLineIndex(); i++) {
                strbuf.append("#" + ps.getLine(i) + ps.getEndLineDelim());
            }
            // Last line shouldn't add the delimiter
            strbuf.append("#" + ps.getLine(i));

            // Replace the text with the modified information
            ps.getDoc().replace(ps.getStartLine().getOffset(), ps.getSelLength(), strbuf.toString());
            return true;
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return false
        return false;
    }
}
