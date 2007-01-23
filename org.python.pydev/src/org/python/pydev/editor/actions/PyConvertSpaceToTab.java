/*
 * @author: ptoofani
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;

/**
 * Converts tab-width spacing to tab characters in selection or entire document,
 * if nothing selected.
 * 
 * @author Parhaum Toofanian
 */
public class PyConvertSpaceToTab extends PyAction {
    /* Selection element */
    private static PySelection ps;

    /**
     * Grabs the selection information and performs the action.
     */
    public void run(IAction action) {
        try {
            // Select from text editor
            ps = new PySelection(getTextEditor());
            ps.selectAll(false);
            // Perform the action
            perform();

            // Put cursor at the first area of the selection
            getTextEditor().selectAndReveal(ps.getLineOffset(), 0);
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Performs the action with the class' PySelection.
     * 
     * @return boolean The success or failure of the action
     */
    public static boolean perform() {
        return perform(ps);
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps
     *            Given PySelection
     * @return boolean The success or failure of the action
     */
    public static boolean perform(PySelection ps) {
        // What we'll be replacing the selected text with
        StringBuffer strbuf = new StringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        int i;

        try {
            // For each line, strip their whitespace
            for (i = ps.getStartLineIndex(); i <= ps.getEndLineIndex(); i++) {
                String line = ps.getDoc().get(ps.getDoc().getLineInformation(i).getOffset(), ps.getDoc().getLineInformation(i).getLength());
                strbuf.append(line.replaceAll(getTabSpace(), "\t") + (i < ps.getEndLineIndex() ? ps.getEndLineDelim() : ""));
            }

            // If all goes well, replace the text with the modified information
            if (strbuf.toString() != null) {
                ps.getDoc().replace(ps.getStartLine().getOffset(), ps.getSelLength(), strbuf.toString());
                return true;
            }
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return false
        return false;
    }

    /**
     * Currently returns an int of the Preferences' Tab Width.
     * 
     * @return Tab width in preferences
     */
    protected static String getTabSpace() {
        StringBuffer sbuf = new StringBuffer();
        int tabWidth = DefaultIndentPrefs.getStaticTabWidth();
        for (int i = 0; i < tabWidth; i++) {
            sbuf.append(" ");
        }
        return sbuf.toString();
    }

}
