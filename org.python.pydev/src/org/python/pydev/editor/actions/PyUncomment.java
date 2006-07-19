/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.docutils.PySelection;

/**
 * @author fabioz
 */
public class PyUncomment extends PyComment {
    /* Selection element */

    /**
     * Grabs the selection information and performs the action.
     */
    public void run(IAction action) {
        PySelection ps = new PySelection(getTextEditor());
        try {
            // Perform the action
            perform(ps);

            // Put cursor at the first area of the selection
            int docLen = ps.getDoc().getLength()-1;
            IRegion endLine = ps.getEndLine();
            if(endLine != null){
                int curOffset = endLine.getOffset();
                getTextEditor().selectAndReveal(curOffset<docLen?curOffset:docLen, 0);
            }
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
            for (i = ps.getStartLineIndex(); i <= ps.getEndLineIndex(); i++) {
                String l = ps.getLine(i);
                if (l.trim().startsWith("#")) { // we may want to remove comment that are not really in the beggining...
                    strbuf.append(l.replaceFirst("#", "") + (i < ps.getEndLineIndex() ? ps.getEndLineDelim() : ""));
                } else {
                    strbuf.append(l + (i < ps.getEndLineIndex() ? ps.getEndLineDelim() : ""));
                }
            }

            // Replace the text with the modified information
            ps.getDoc().replace(ps.getStartLine().getOffset(), ps.getSelLength(), strbuf.toString());
            return true;
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return false
        return false;
    }

    /**
     * Same as comment, but remove the first char.
     */
    protected String replaceStr(String str, String endLineDelim) {
        str = str.replaceAll(endLineDelim + "#", endLineDelim);
        if (str.startsWith("#")) {
            str = str.substring(1);
        }
        return str;
    }

}