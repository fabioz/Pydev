/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.StringUtils;

public class PyDocIterator implements Iterator<String> {

    private int offset;
    private IDocument doc;

    private boolean addNewLinesToRet = true;
    private boolean returnNewLinesOnLiterals = false;
    private boolean inLiteral = false;
    private int literalEnd = 0;
    private boolean changeLiteralsForSpaces = false;
    private int lastReturned = -1;
    private boolean addComments = false;

    public PyDocIterator(IDocument doc, boolean addNewLinesToRet) {
        this(doc, addNewLinesToRet, false, false);
    }

    public PyDocIterator(IDocument doc, boolean addNewLinesToRet, boolean returnNewLinesOnLiterals,
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
    public PyDocIterator(IDocument doc, boolean addNewLinesToRet, boolean returnNewLinesOnLiterals,
            boolean changeLiteralsForSpaces, boolean addComments) {
        this(doc);
        this.addNewLinesToRet = addNewLinesToRet;
        this.returnNewLinesOnLiterals = returnNewLinesOnLiterals;
        this.changeLiteralsForSpaces = changeLiteralsForSpaces;
        this.addComments = addComments;
    }

    public PyDocIterator(IDocument doc) {
        this.doc = doc;
    }

    /**
     * Changes the current offset in the document. Note: this method is not safe for use after the iteration
     * started!
     * 
     * @param offset the offset where this class should start parsing (note: the offset must be a 
     * code partition, otherwise the yielded values will be wrong).
     */
    public void setStartingOffset(int offset) {
        this.offset = offset;
    }

    public boolean hasNext() {
        return offset < doc.getLength();
    }

    public int getLastReturnedLine() {
        try {
            lastReturned = doc.getLineOfOffset(offset - 1);
        } catch (BadLocationException e) {
            //ignore (keep the last one)
        }
        return lastReturned;
    }

    private String nextInLiteral() {
        StringBuffer buf = new StringBuffer();
        try {

            char ch = doc.getChar(offset);
            while (offset < literalEnd && ch != '\n' && ch != '\r') {
                ch = doc.getChar(offset);
                offset++;
                if (changeLiteralsForSpaces && ch != '\n' && ch != '\r') {
                    buf.append(' ');
                }
            }
            if (offset >= literalEnd) {
                inLiteral = false;
                offset++;
                if (changeLiteralsForSpaces) {
                    buf.append(' ');
                }
                return buf.toString();
            }

            if (ch == '\r') {
                ch = doc.getChar(offset + 1);
                if (ch == '\n') {
                    offset++;
                    ch = '\n';
                }
            }
            buf.append(ch);
            return buf.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the next line in the document
     */
    public String next() {

        try {
            StringBuffer buf = new StringBuffer();

            if (inLiteral) {
                int initialOffset = offset;
                String ret = nextInLiteral();
                if (ret.length() > 0 && initialOffset < offset) { //if it didn't move in the offset, disregard the results
                    if (StringUtils.endsWith(ret, '\r') || StringUtils.endsWith(ret, '\n')) {
                        if (!addNewLinesToRet) {
                            ret = ret.substring(0, ret.length() - 1);
                        }
                        buf.append(ret);
                        return ret;
                    } else {
                        buf.append(ret);
                    }
                }
            }

            char ch = 0;

            int docLen = doc.getLength();
            ParsingUtils parsingUtils = ParsingUtils.create(doc);
            while (ch != '\r' && ch != '\n' && offset < docLen) {
                ch = doc.getChar(offset);
                if (ch == '#') {

                    while (offset < docLen && ch != '\n' && ch != '\r') {
                        ch = doc.getChar(offset);
                        if (addComments && ch != '\n' && ch != '\r') {
                            buf.append(ch);
                        }
                        offset++;
                    }

                } else if (ch == '\'' || ch == '"') {
                    if (returnNewLinesOnLiterals) {
                        inLiteral = true;
                        literalEnd = parsingUtils.getLiteralEnd(offset, ch);
                        String ret = nextInLiteral();
                        if (ret.length() > 0) {
                            if (StringUtils.endsWith(ret, '\r') || StringUtils.endsWith(ret, '\n')) {
                                if (!addNewLinesToRet) {
                                    ret = ret.substring(0, ret.length() - 1);
                                }
                                buf.append(ret);
                                return buf.toString();
                            } else {
                                buf.append(ret);
                            }
                        }
                    } else {
                        if (this.changeLiteralsForSpaces) {
                            throw new RuntimeException("Not supported in this case.");
                        }
                        offset = parsingUtils.getLiteralEnd(offset, ch);
                        offset++;
                    }

                } else if (ch != '\n' && ch != '\r') {
                    //will be added later
                    buf.append(ch);
                    offset++;
                } else {
                    offset++;
                }
            }

            //handle the \r, \n or \r\n
            if (ch == '\n' || ch == '\r') {
                if (addNewLinesToRet) {
                    buf.append(ch);
                }
                if (ch == '\r') {
                    if (offset < docLen && doc.getChar(offset) == '\n') {
                        offset++;
                        if (addNewLinesToRet) {
                            buf.append('\n');
                        }
                    }
                }
            }

            return buf.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void remove() {
        throw new RuntimeException("Not Impl.");
    }

}
