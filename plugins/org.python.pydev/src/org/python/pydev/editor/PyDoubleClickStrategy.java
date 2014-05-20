/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic, fabioz
 * Created on Apr 14, 2004
 */
package org.python.pydev.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.docutils.PythonPairMatcher;

/**
 * Our double-click implementation. Based on org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickStrategy.
 */
public class PyDoubleClickStrategy implements ITextDoubleClickStrategy {

    protected PythonPairMatcher fPairMatcher = new PythonPairMatcher(PyStringUtils.BRACKETS);
    private String contentType;

    public PyDoubleClickStrategy(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @see ITextDoubleClickStrategy#doubleClicked
     */
    public void doubleClicked(ITextViewer textViewer) {

        int offset = textViewer.getSelectedRange().x;

        if (offset < 0) {
            return;
        }

        IDocument document = textViewer.getDocument();

        IRegion region = fPairMatcher.match(document, offset);
        if (region != null && region.getLength() >= 2) {
            textViewer.setSelectedRange(region.getOffset() + 1, region.getLength() - 2);
        } else {
            selectWord(textViewer, document, offset);
        }
    }

    protected void selectWord(ITextViewer textViewer, IDocument document, final int anchor) {

        try {

            int offset = anchor;
            char c;

            while (offset >= 0) {
                c = document.getChar(offset);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }

                --offset;
            }

            int start = offset;

            offset = anchor;
            final int length = document.getLength();

            while (offset < length) {
                c = document.getChar(offset);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
                ++offset;
            }

            int end = offset;

            if (start == end) {
                //Nothing to select... let's check if we can select whitespaces
                offset = anchor;

                while (offset >= 0) {
                    c = document.getChar(offset);
                    if (c != ' ' && c != '\t') {
                        break;
                    }

                    --offset;
                }

                start = offset;

                offset = anchor;

                while (offset < length) {
                    c = document.getChar(offset);
                    if (c != ' ' && c != '\t') {
                        break;
                    }
                    ++offset;
                }

                end = offset;
            }

            if (start == end) {
                textViewer.setSelectedRange(start, 0);
            } else {
                textViewer.setSelectedRange(start + 1, end - start - 1);
            }

        } catch (BadLocationException x) {
        }
    }
}
