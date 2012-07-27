/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.shared_core.utils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

public class TextSelectionUtils {

    protected IDocument doc;
    protected ITextSelection textSelection;

    /**
     * @param document the document we are using to make the selection
     * @param selection that's the actual selection. It might have an offset and a number of selected chars
     */
    public TextSelectionUtils(IDocument doc, ITextSelection selection) {
        this.doc = doc;
        this.textSelection = selection;
    }

    /**
     * @param document the document we are using to make the selection
     * @param offset the offset where the selection will happen (0 characters will be selected)
     */
    public TextSelectionUtils(IDocument doc, int offset) {
        this(doc, new TextSelection(doc, offset, 0));
    }

    /**
     * @return the offset of the line where the cursor is
     */
    public final int getLineOffset() {
        return getLineOffset(getCursorLine());
    }

    /**
     * @return the offset of the specified line
     */
    public final int getLineOffset(int line) {
        try {
            return getDoc().getLineInformation(line).getOffset();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * @return Returns the doc.
     */
    public final IDocument getDoc() {
        return doc;
    }

    /**
     * @return Returns the cursorLine.
     */
    public final int getCursorLine() {
        return this.getTextSelection().getEndLine();
    }

    /**
     * @return Returns the textSelection.
     */
    public final ITextSelection getTextSelection() {
        return textSelection;
    }

    /**
     * @return Returns the absoluteCursorOffset.
     */
    public final int getAbsoluteCursorOffset() {
        return this.getTextSelection().getOffset();
    }

    /**
     * @param src
     * @return
     */
    public static int getFirstCharPosition(String src) {
        int i = 0;
        boolean breaked = false;
        while (i < src.length()) {
            if (Character.isWhitespace(src.charAt(i)) == false && src.charAt(i) != '\t') {
                i++;
                breaked = true;
                break;
            }
            i++;
        }
        if (!breaked) {
            i++;
        }
        return (i - 1);
    }

    /**
     * @return the offset mapping to the end of the line passed as parameter.
     * @throws BadLocationException 
     */
    public final int getEndLineOffset(int line) throws BadLocationException {
        IRegion lineInformation = doc.getLineInformation(line);
        return lineInformation.getOffset() + lineInformation.getLength();
    }

    /**
     * @return the offset mapping to the end of the current 'end' line.
     */
    public final int getEndLineOffset() {
        IRegion endLine = getEndLine();
        return endLine.getOffset() + endLine.getLength();
    }

    /**
     * @return Returns the endLine.
     */
    public final IRegion getEndLine() {
        try {
            int endLineIndex = getEndLineIndex();
            if (endLineIndex == -1) {
                return null;
            }
            return getDoc().getLineInformation(endLineIndex);
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return null;
    }

    /**
     * @return Returns the endLineIndex.
     */
    public final int getEndLineIndex() {
        return this.getTextSelection().getEndLine();
    }
}
