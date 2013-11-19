/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.actions;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class ToggleLineCommentAction {
    private TextSelectionUtils ps;
    private String commentPattern;
    private int spacesInStart;

    public ToggleLineCommentAction(TextSelectionUtils ps, String commentPattern, int spacesInStart) {
        this.ps = ps;
        this.commentPattern = commentPattern;
        this.spacesInStart = spacesInStart;
    }

    public Tuple<Integer, Integer> execute() throws BadLocationException {
        // What we'll be replacing the selected text with

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        String selectedText = ps.getSelectedText();
        List<String> ret = StringUtils.splitInLines(selectedText);
        boolean allStartWithComments = true;
        for (String string : ret) {
            if (!StringUtils.leftTrim(string).startsWith(commentPattern)) {
                allStartWithComments = false;
                break;
            }
        }

        if (allStartWithComments) {
            return new LineUncommentAction(ps, commentPattern, spacesInStart).execute();
        } else {
            return new LineCommentAction(ps, commentPattern, spacesInStart).execute();
        }
    }
}
