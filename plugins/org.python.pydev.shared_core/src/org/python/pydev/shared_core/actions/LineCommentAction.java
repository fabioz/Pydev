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
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class LineCommentAction {

    private TextSelectionUtils ps;
    private String commentPattern;
    private int spacesInStart;

    public LineCommentAction(TextSelectionUtils ps, String commentPattern, int spacesInStart) {
        this.ps = ps;
        this.commentPattern = commentPattern;
        this.spacesInStart = spacesInStart;
    }

    public FastStringBuffer commentLines(String selectedText) {
        List<String> ret = StringUtils.splitInLines(selectedText);

        FastStringBuffer strbuf = new FastStringBuffer(selectedText.length() + ret.size()
                + ((spacesInStart + 2) * ret.size()));
        String spacesInStartComment = null;
        if (spacesInStart > 0) {
            spacesInStartComment = StringUtils.createSpaceString(spacesInStart);
        }

        for (String line : ret) {
            strbuf.append(commentPattern);
            if (spacesInStartComment != null) {
                strbuf.append(spacesInStartComment);
            }
            strbuf.append(line);
        }
        return strbuf;
    }

    public Tuple<Integer, Integer> execute() throws BadLocationException {
        // What we'll be replacing the selected text with

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        String selectedText = ps.getSelectedText();

        FastStringBuffer strbuf = commentLines(selectedText);
        ITextSelection txtSel = ps.getTextSelection();
        int start = txtSel.getOffset();
        int len = txtSel.getLength();

        String replacement = strbuf.toString();
        // Replace the text with the modified information
        ps.getDoc().replace(start, len, replacement);
        return new Tuple<Integer, Integer>(start, replacement.length());
    }

}
