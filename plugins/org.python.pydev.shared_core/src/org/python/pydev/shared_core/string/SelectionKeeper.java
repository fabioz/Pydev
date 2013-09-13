/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.string;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;

/**
 * This is a helper class to try to keep a given selection and re-apply it later based  
 * on the actual lines and columns that were selected.
 */
public class SelectionKeeper {

    private final int startLine;
    private final int endLine;
    private final int startCol;
    private final int endCol;

    public SelectionKeeper(TextSelectionUtils ps) {
        ITextSelection selection = ps.getTextSelection();
        startLine = selection.getStartLine();
        endLine = selection.getEndLine();
        startCol = selection.getOffset() - ps.getLineOffset(startLine);
        endCol = (selection.getOffset() + selection.getLength()) - ps.getLineOffset(endLine);
    }

    /**
     * Restores the selection previously gotten.
     */
    public void restoreSelection(ISelectionProvider selectionProvider, IDocument doc) {
        //OK, now, the start line and the end line should not change -- because the document changed,
        //the columns may end up being wrong, so, we must update things so that the selection stays OK.
        int numberOfLines = doc.getNumberOfLines();
        int startLine = fixBasedOnNumberOfLines(this.startLine, numberOfLines);
        int endLine = fixBasedOnNumberOfLines(this.endLine, numberOfLines);

        final int startLineOffset = getOffset(doc, startLine);
        final int startLineLen = getLineLength(doc, startLine);
        final int startLineDelimiterLen = getLineDelimiterLen(doc, startLine);

        int startOffset = fixOffset(startLineOffset + startCol, startLineOffset, startLineOffset + startLineLen
                - startLineDelimiterLen);

        final int endLineOffset = getOffset(doc, endLine);
        final int endLineLen = getLineLength(doc, endLine);
        final int endLineDelimiterLen = getLineDelimiterLen(doc, endLine);
        int endOffset = fixOffset(endLineOffset + endCol, endLineOffset, endLineOffset + endLineLen
                - endLineDelimiterLen);

        selectionProvider.setSelection(new TextSelection(startOffset, endOffset - startOffset));
    }

    private int getLineDelimiterLen(IDocument doc, int line) {
        try {
            String lineDelimiter = doc.getLineDelimiter(line);
            if (lineDelimiter == null) {
                return 0;
            }
            return lineDelimiter.length();
        } catch (BadLocationException e) {
            return 0;
        }
    }

    private int getLineLength(IDocument doc, int line) {
        try {
            return doc.getLineLength(line);
        } catch (BadLocationException e) {
            return 0;
        }
    }

    private int fixOffset(int offset, int minOffset, int maxOffset) {
        if (offset > maxOffset) {
            offset = maxOffset;
        }
        if (offset < minOffset) {
            offset = minOffset;
        }
        return offset;
    }

    private int getOffset(IDocument doc, int startLine) {
        try {
            return doc.getLineOffset(startLine);
        } catch (BadLocationException e) {
            return 0;
        }
    }

    private int fixBasedOnNumberOfLines(int line, int numberOfLines) {
        if (line > numberOfLines - 1) {
            line = numberOfLines - 1;
        }
        if (line < 0) {
            line = 0;
        }
        return line;
    }

}
