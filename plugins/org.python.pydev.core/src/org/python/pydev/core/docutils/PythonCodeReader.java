/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.io.IOException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * The reader works well as long as we are not inside a string at the current offset (this is not enforced here, so,
 * use at your own risk).
 * 
 * @author Fabio Zadrozny
 */
public class PythonCodeReader {

    /** The EOF character */
    public static final int EOF = -1;

    private boolean fForward = false;

    private IDocument fDocument;
    private int fOffset;

    private int fEnd = -1;

    private boolean fOnlyInCurrentStmt;

    private ParsingUtils fParsingUtils;

    private FastStringBuffer wordBuffer = new FastStringBuffer();
    private int wordBufferOffset = -1;

    public PythonCodeReader() {
    }

    /**
     * Returns the offset of the last read character. Should only be called after read has been called.
     */
    public int getOffset() {
        return fForward ? fOffset - 1 : fOffset;
    }

    public void configureForwardReader(IDocument document, int offset, int length, boolean skipComments,
            boolean skipStrings, boolean onlyInCurrentStmt) throws IOException {
        //currently not implemented without skip, so, that's the reason the asserts are here...
        Assert.isTrue(skipComments);
        Assert.isTrue(skipStrings);

        fParsingUtils = ParsingUtils.create(document);
        fOnlyInCurrentStmt = onlyInCurrentStmt;
        fDocument = document;
        fOffset = offset;

        fForward = true;
        fEnd = Math.min(fDocument.getLength(), fOffset + length);
    }

    public void configureBackwardReader(IDocument document, int offset, boolean skipComments, boolean skipStrings,
            boolean onlyInCurrentStmt) throws IOException {
        //currently not implemented without skip, so, that's the reason the asserts are here...
        Assert.isTrue(skipComments);
        Assert.isTrue(skipStrings);

        fParsingUtils = ParsingUtils.create(document);
        fOnlyInCurrentStmt = onlyInCurrentStmt;
        fDocument = document;
        fOffset = offset;

        fForward = false;
    }

    /*
     * @see Reader#close()
     */
    public void close() throws IOException {
        fDocument = null;
    }

    /*
     * @see SingleCharReader#read()
     */
    public int read() throws IOException {
        try {
            return fForward ? readForwards() : readBackwards();
        } catch (BadLocationException x) {
            return EOF; //Document may have changed...
        }
    }

    private int readForwards() throws BadLocationException {
        if (wordBufferOffset >= 0) {
            if (wordBufferOffset < wordBuffer.length()) {
                fOffset++;
                return wordBuffer.charAt(wordBufferOffset++);
            }
            wordBuffer.clear();
            wordBufferOffset = -1;
        }
        while (fOffset < fEnd) {
            char current = fDocument.getChar(fOffset++);

            switch (current) {
                case '#':
                    fOffset = fParsingUtils.eatComments(null, fOffset);
                    return current;

                case '"':
                case '\'':
                    try {
                        fOffset = fParsingUtils.eatLiterals(null, fOffset - 1) + 1;
                    } catch (SyntaxErrorException e) {
                        return EOF;
                    }
                    //go on to the next loop (returns no char in this step)
                    continue;
            }

            if (fOnlyInCurrentStmt) {
                if (Character.isJavaIdentifierPart(current)) {
                    wordBuffer.clear().append(current);
                    int offset = fOffset;
                    while (offset < fEnd) {
                        char c = fDocument.getChar(offset++);
                        if (Character.isJavaIdentifierPart(c)) {
                            wordBuffer.append(c);
                        } else {
                            break;
                        }
                    }
                    if (PySelection.STATEMENT_TOKENS.contains(wordBuffer.toString())) {
                        return EOF;
                    }
                    wordBufferOffset = 1; //We've just returned the one at pos == 0
                    return current;
                }
            }

            return current;
        }

        return EOF;
    }

    private int readBackwards() throws BadLocationException {
        if (wordBufferOffset >= 0) {
            if (wordBufferOffset > 0) {
                //Note that we already returned the one at pos 0
                fOffset--;
                return wordBuffer.charAt(--wordBufferOffset);
            }
            wordBuffer.clear();
            wordBufferOffset = -1;
        }

        while (0 < fOffset) {
            --fOffset;

            handleComment();
            if (fOffset < 0) {
                return EOF;
            }
            char current = fDocument.getChar(fOffset);
            switch (current) {

                case '"':
                case '\'':
                    try {
                        fOffset = fParsingUtils.eatLiteralsBackwards(null, fOffset);
                    } catch (SyntaxErrorException e) {
                        return EOF;
                    }
                    continue;
            }

            if (fOnlyInCurrentStmt) {
                if (Character.isJavaIdentifierPart(current)) {
                    wordBuffer.clear();
                    int offset = fOffset;
                    while (offset >= 0) {
                        char c = fDocument.getChar(offset--);
                        if (Character.isJavaIdentifierPart(c)) {
                            wordBuffer.append(c);
                        } else {
                            break;
                        }
                    }
                    wordBuffer.reverse();
                    if (PySelection.STATEMENT_TOKENS.contains(wordBuffer.toString())) {
                        return EOF;
                    }
                    wordBufferOffset = wordBuffer.length() - 1;
                    return current;
                }
            }

            return current;

        }

        return EOF;
    }

    //works as a cache so that we don't have to handle some line over and over again for comments
    private int handledLine = -1;

    private void handleComment() throws BadLocationException {
        int lineOfOffset = fDocument.getLineOfOffset(fOffset);
        if (handledLine == lineOfOffset) {
            return;
        }
        handledLine = lineOfOffset;
        String line = PySelection.getLine(fDocument, lineOfOffset);
        int i;
        int fromIndex = 0;
        IRegion lineInformation = null;
        //first we check for a comment possibility
        while ((i = line.indexOf('#', fromIndex)) != -1) {
            fromIndex = i + 1;

            if (lineInformation == null) {
                lineInformation = fDocument.getLineInformation(lineOfOffset);
            }
            int offset = lineInformation.getOffset() + i;

            String contentType = ParsingUtils.getContentType(fDocument, offset + 1);
            if (contentType.equals(ParsingUtils.PY_COMMENT)) {
                //We need to check this because it may be that the # is found inside a string (which should be ignored)
                if (offset < fOffset) {
                    fOffset = offset;
                    return;
                }
            }
        }
    }
}
