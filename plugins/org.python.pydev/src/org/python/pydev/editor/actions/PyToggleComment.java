/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;

/**
 * Same toggle comment action as we are used to it in the java perspective
 * 
 * @author e0525580 at student.tuwien.ac.at
 * Created from patch: https://sourceforge.net/tracker/?func=detail&atid=577329&aid=1999389&group_id=85796
 */
public class PyToggleComment extends PyComment {

    @Override
    public Tuple<Integer, Integer> perform(final PySelection ps) throws BadLocationException {
        ps.selectCompleteLine();

        final boolean shouldAddCommentSign = PyToggleComment.allLinesStartWithCommentSign(ps) == false;
        String endLineDelim = ps.getEndLineDelim();
        int endLineIndex = ps.getEndLineIndex();
        int startLineIndex = ps.getStartLineIndex();

        final FastStringBuffer sb = new FastStringBuffer(ps.getSelLength() + (endLineIndex - startLineIndex) + 10);

        for (int i = startLineIndex, n = endLineIndex; i <= n; i++) {
            final String line = ps.getLine(i);
            if (shouldAddCommentSign) {
                sb.append("#");
                sb.append(line);
            } else { // remove comment sign
                sb.append(line.replaceFirst("#", ""));
            }
            //add a new line if we're not in the last line.
            sb.append((i < endLineIndex ? endLineDelim : ""));
        }

        final int start = ps.getStartLine().getOffset();
        final String replacement = sb.toString();

        ps.getDoc().replace(start, ps.getSelLength(), replacement);
        return new Tuple<Integer, Integer>(start, replacement.length());
    }

    /**
     * Checks if all lines start with '#' 
     */
    private static boolean allLinesStartWithCommentSign(final PySelection ps) {
        int endLineIndex = ps.getEndLineIndex();

        for (int i = ps.getStartLineIndex(), n = endLineIndex; i <= n; i++) {
            final String line = ps.getLine(i);
            if (line.trim().startsWith("#") == false) {
                return false;
            }
        }
        return true;
    }
}
