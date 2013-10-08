/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Same toggle comment action as we are used to it in the java perspective
 * 
 * @author e0525580 at student.tuwien.ac.at
 * Created from patch: https://sourceforge.net/tracker/?func=detail&atid=577329&aid=1999389&group_id=85796
 */
public class PyToggleComment extends PyUncomment {

    public PyToggleComment(FormatStd std) {
        super(std);
    }

    public PyToggleComment() {
        this(null);
    }

    @Override
    public Tuple<Integer, Integer> perform(final TextSelectionUtils ps) throws BadLocationException {
        ps.selectCompleteLine();

        final boolean shouldAddCommentSign = PyToggleComment.allLinesStartWithCommentSign(ps, "#") == false;
        if (shouldAddCommentSign) {
            return performComment(ps);

        } else {
            return performUncomment(ps);
        }
    }

    /**
     * Checks if all lines start with '#' 
     */
    private static boolean allLinesStartWithCommentSign(final TextSelectionUtils ps, String commentStart) {
        int endLineIndex = ps.getEndLineIndex();

        for (int i = ps.getStartLineIndex(), n = endLineIndex; i <= n; i++) {
            final String line = ps.getLine(i);
            if (line.trim().startsWith(commentStart) == false) {
                return false;
            }
        }
        return true;
    }
}
