/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;

/**
 * @author fabioz
 */
public class PyUncomment extends PyComment {
    /* Selection element */



    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @return the new selection
     * @throws BadLocationException 
     */
    public Tuple<Integer, Integer> perform(PySelection ps) throws BadLocationException {
        // What we'll be replacing the selected text with
        StringBuffer strbuf = new StringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        int i;
        // For each line, comment them out
        for (i = ps.getStartLineIndex(); i <= ps.getEndLineIndex(); i++) {
            String l = ps.getLine(i);
            if (l.trim().startsWith("#")) { // we may want to remove comment that are not really in the beggining...
                strbuf.append(l.replaceFirst("#", "") + (i < ps.getEndLineIndex() ? ps.getEndLineDelim() : ""));
            } else {
                strbuf.append(l + (i < ps.getEndLineIndex() ? ps.getEndLineDelim() : ""));
            }
        }

        int start = ps.getStartLine().getOffset();
        String replacement = strbuf.toString();
        // Replace the text with the modified information
        ps.getDoc().replace(start, ps.getSelLength(), replacement);
        return new Tuple<Integer, Integer>(start, replacement.length());
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