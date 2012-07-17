/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: fabioz
 * Created: January 2004
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;

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

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        // What we'll be replacing the selected text with
        FastStringBuffer strbuf = new FastStringBuffer(ps.getSelLength() + 1); //no, it won't be more that the current sel

        // For each line, uncomment it
        int endLineIndex = ps.getEndLineIndex();
        String endLineDelim = ps.getEndLineDelim();

        for (int i = ps.getStartLineIndex(); i <= endLineIndex; i++) {
            String l = ps.getLine(i);
            if (l.trim().startsWith("#")) { // we may want to remove comment that are not really in the beggining...
                strbuf.append(l.replaceFirst("#", ""));
            } else {
                strbuf.append(l);
            }
            //add a new line if we're not in the last line.
            strbuf.append(i < endLineIndex ? endLineDelim : "");
        }

        int start = ps.getStartLine().getOffset();
        String replacement = strbuf.toString();
        // Replace the text with the modified information
        ps.getDoc().replace(start, ps.getSelLength(), replacement);
        return new Tuple<Integer, Integer>(start, replacement.length());
    }

}