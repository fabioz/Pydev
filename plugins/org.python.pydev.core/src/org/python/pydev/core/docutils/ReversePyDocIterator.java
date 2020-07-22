/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.StringUtils;

public class ReversePyDocIterator implements IPyDocIterator {

    private int offset;
    private IDocument doc;

    private boolean addNewLinesToRet = true;
    private boolean returnNewLinesOnLiterals = false;
    private boolean inLiteral = false;
    private int literalStart = 0;
    private boolean changeLiteralsForSpaces = false;
    private int lastReturned = -1;
    private boolean addComments = false;
    private boolean considerAfterLiteralEnd = true;

    public ReversePyDocIterator(IDocument doc, boolean addNewLinesToRet) {
        this(doc, addNewLinesToRet, false, false);
    }

    public ReversePyDocIterator(IDocument doc, boolean addNewLinesToRet, boolean returnNewLinesOnLiterals,
            boolean changeLiteralsForSpaces) {
        this(doc, addNewLinesToRet, returnNewLinesOnLiterals, changeLiteralsForSpaces, false);
    }

    /**
     * @param doc the document where we will iterate
     * @param addNewLinesToRet whether the new line character should be added to the return
     * @param returnNewLinesOnLiterals whether we should return the new lines found in the literals (not the char, but the line itself)
     * @param changeLiteralsForSpaces whether we should replace the literals with spaces (so that we don't loose offset information)
     * @param addComments if true, comments found will be yielded (otherwise, no comments will be shown)
     */

    public ReversePyDocIterator(IDocument doc, boolean addNewLinesToRet, boolean returnNewLinesOnLiterals,
            boolean changeLiteralsForSpaces, boolean addComments) {
        this(doc, addNewLinesToRet, returnNewLinesOnLiterals, changeLiteralsForSpaces, addComments, true);
    }

    public ReversePyDocIterator(IDocument doc, boolean addNewLinesToRet, boolean returnNewLinesOnLiterals,
            boolean changeLiteralsForSpaces, boolean addComments, boolean considerAfterLiteralEnd) {
        this(doc);
        this.addNewLinesToRet = addNewLinesToRet;
        this.returnNewLinesOnLiterals = returnNewLinesOnLiterals;
        this.changeLiteralsForSpaces = changeLiteralsForSpaces;
        this.addComments = addComments;
        this.considerAfterLiteralEnd = considerAfterLiteralEnd;
    }

    public ReversePyDocIterator(IDocument doc) {
        this.doc = doc;
        this.offset = doc.getLength() - 1;
    }

    /**
     * Changes the current offset in the document. Note: this method is not safe for use after the iteration
     * started!
     *
     * @param offset the offset where this class should start parsing (note: the offset must be a
     * code partition, otherwise the yielded values will be wrong).
     */
    @Override
    public void setStartingOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public void setStartingLine(int line) throws BadLocationException {
        this.offset = doc.getLineOffset(line + 1) + doc.getLineLength(line + 1) - 1;
    }

    @Override
    public boolean hasNext() {
        return offset >= 0;
    }

    @Override
    public int getLastReturnedLine() {
        if (offset + 1 != doc.getLength()) {
            try {
                lastReturned = doc.getLineOfOffset(offset + 1);
            } catch (BadLocationException e) {
                //ignore (keep the last one)
            }
        }
        return lastReturned;
    }

    private String nextInLiteral() {
        StringBuffer buf = new StringBuffer();
        try {

            char ch = doc.getChar(offset);
            if (ch == '\n') {
                offset--;
                if (doc.getChar(offset) == '\r') {
                    offset--;
                }
                buf.append(ch);
                ch = doc.getChar(offset);
            }

            while (offset >= literalStart && ch != '\n' && ch != '\r') {
                ch = doc.getChar(offset);
                if (ch != '\n' && ch != '\r') {
                    offset--;
                    if (changeLiteralsForSpaces) {
                        buf.insert(0, ' ');
                    }
                }
            }
            if (offset < literalStart) {
                inLiteral = false;
            }
            return buf.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the next line in the document
     */
    @Override
    public String next() {

        try {
            StringBuffer buf = new StringBuffer();

            if (inLiteral) {
                int initialOffset = offset;
                String ret = nextInLiteral();
                if (ret.length() > 0 && initialOffset >= offset) { //if it didn't move in the offset, disregard the results
                    if (StringUtils.endsWith(ret, '\r') || StringUtils.endsWith(ret, '\n')) {
                        if (!addNewLinesToRet) {
                            ret = ret.substring(0, ret.length() - 1);
                        }
                        buf.insert(0, ret);
                        if (inLiteral) {
                            return ret;
                        }
                    } else {
                        buf.insert(0, ret);
                    }
                }
            }

            char ch = doc.getChar(offset);

            //handle the \r, \n or \r\n
            if (ch == '\n' || ch == '\r') {
                if (addNewLinesToRet) {
                    buf.append(ch);
                }
                offset--;
                ch = doc.getChar(offset);
                if (ch == '\n') {
                    if (offset >= 0 && ch == '\r') {
                        offset--;
                        ch = doc.getChar(offset);
                        if (addNewLinesToRet) {
                            buf.append('\n');
                        }
                    }
                }

            }

            ParsingUtils parsingUtils = ParsingUtils.create(doc);
            while (ch != '\r' && ch != '\n' && offset >= 0) {
                ch = doc.getChar(offset);
                if (ch == '#') {

                    while (offset >= 0 && ch != '\n' && ch != '\r') {
                        ch = doc.getChar(offset);
                        if (addComments && ch != '\n' && ch != '\r') {
                            buf.insert(0, ch);
                        }
                        offset--;
                    }

                } else if (ch == '\'' || ch == '"') {
                    if (returnNewLinesOnLiterals) {
                        inLiteral = true;
                        literalStart = parsingUtils.getLiteralStart(offset, ch);
                        String ret = nextInLiteral();
                        if (ret.length() > 0) {
                            if (considerAfterLiteralEnd) {
                                buf.insert(0, ret);
                                return buf.toString();
                            } else {
                                return ret;
                            }
                        }
                    } else {
                        if (this.changeLiteralsForSpaces) {
                            throw new RuntimeException("Not supported in this case.");
                        }
                        offset = parsingUtils.getLiteralStart(offset, ch);
                        offset--;
                    }

                } else if (ch != '\n' && ch != '\r') {
                    //will be added later
                    buf.insert(0, ch);
                    offset--;
                }
            }

            return buf.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
        throw new RuntimeException("Not Impl.");
    }

}
