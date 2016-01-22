/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple3;

/**
 * Class to help iterating through the document's indentation strings.
 *
 * It will yield Tuples with Strings (whitespaces/tabs), starting offset, boolean (true if line has more contents than the spaces/tabs)
 *
 * the indentations within literals, [, (, {, after \ are not considered
 * (only the ones actually considered indentations are yielded through).
 */
public class TabNannyDocIterator {

    //Mutable on iteration.
    private int offset;
    private Tuple3<String, Integer, Boolean> nextString;
    private boolean firstPass = true;

    //Final fields.
    private final ParsingUtils parsingUtils;
    private final FastStringBuffer tempBuf = new FastStringBuffer();
    private final boolean yieldEmptyIndents;
    private final boolean yieldOnLinesWithoutContents;
    private final IDocument doc;

    public TabNannyDocIterator(IDocument doc) throws BadLocationException {
        this(doc, false, true);
    }

    public TabNannyDocIterator(IDocument doc, boolean yieldEmptyIndents, boolean yieldOnLinesWithoutContents)
            throws BadLocationException {
        this(doc, yieldEmptyIndents, yieldOnLinesWithoutContents, 0);
    }

    public TabNannyDocIterator(IDocument doc, boolean yieldEmptyIndents, boolean yieldOnLinesWithoutContents,
            int initialOffset)
                    throws BadLocationException {
        parsingUtils = ParsingUtils.create(doc, true);
        this.offset = initialOffset;
        this.doc = doc;
        this.yieldEmptyIndents = yieldEmptyIndents;
        this.yieldOnLinesWithoutContents = yieldOnLinesWithoutContents;
        buildNext(true);
    }

    public boolean hasNext() {
        return nextString != null;
    }

    /**
     * @return tuple with the indentation, the offset where it started and a boolean identifying if the line
     * has more than only whitespaces (i.e.: there are contents in the line).
     */
    public Tuple3<String, Integer, Boolean> next() throws BadLocationException {
        if (!hasNext()) {
            throw new RuntimeException("Cannot iterate anymore.");
        }

        Tuple3<String, Integer, Boolean> ret = nextString;
        buildNext(false);
        return ret;
    }

    private void buildNext(boolean first) throws BadLocationException {
        while (!internalBuildNext()) {
            //just keep doing it... -- lot's of nothing ;-)
        }
    }

    private boolean internalBuildNext() throws BadLocationException {
        try {
            //System.out.println("buildNext");
            char c = '\0';

            int initial = -1;
            while (true) {

                //safeguard... it must walk a bit every time...
                if (initial == -1) {
                    initial = offset;
                } else {
                    if (initial == offset) {
                        Log.log("Error: TabNannyDocIterator didn't walk.\n" + "Curr char:" + c + "\n"
                                + "Curr char (as int):" + (int) c + "\n" + "Offset:" + offset + "\n" + "DocLen:"
                                + doc.getLength() + "\n");
                        offset++;
                        return true;
                    } else {
                        initial = offset;
                    }
                }

                //keep in this loop until we finish the document or until we're able to find some indent string...
                if (offset >= doc.getLength()) {
                    nextString = null;
                    return true;
                }
                c = doc.getChar(offset);

                if (firstPass) {
                    //that could happen if we have comments in the 1st line...
                    firstPass = false;
                    if ((c == ' ' || c == '\t')) {
                        break;
                    } else {
                        if (yieldEmptyIndents) {
                            nextString = new Tuple3<String, Integer, Boolean>(tempBuf.toString(), offset, c != '\r'
                                    && c != '\n');
                            if (!yieldOnLinesWithoutContents) {
                                if (!nextString.o3) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }

                }

                if (c == '#') {
                    //comment (doesn't consider the escape char)
                    offset = parsingUtils.eatComments(null, offset);

                } else if (c == '{' || c == '[' || c == '(') {
                    //starting some call, dict, list, tuple... we're at the same indentation until it is finished
                    try {
                        offset = parsingUtils.eatPar(offset, null, c);
                    } catch (SyntaxErrorException e) {
                        //Ignore unbalanced parens.
                        offset++;
                    }

                } else if (c == '\r') {
                    //line end (time for a break to see if we have some indentation just after it...)
                    if (!continueAfterIncreaseOffset()) {
                        return true;
                    }
                    c = doc.getChar(offset);
                    if (c == '\n') {
                        if (!continueAfterIncreaseOffset()) {
                            return true;
                        }
                    }
                    break;

                } else if (c == '\n') {
                    //line end (time for a break to see if we have some indentation just after it...)
                    if (!continueAfterIncreaseOffset()) {
                        return true;
                    }
                    break;

                } else if (c == '\\') {
                    //escape char found... if it's the last in the line, we don't have a break (we're still in the same line)
                    boolean lastLineChar = false;

                    if (!continueAfterIncreaseOffset()) {
                        return true;
                    }

                    c = doc.getChar(offset);
                    if (c == '\r') {
                        if (!continueAfterIncreaseOffset()) {
                            return true;
                        }
                        c = doc.getChar(offset);
                        lastLineChar = true;
                    }

                    if (c == '\n') {
                        if (!continueAfterIncreaseOffset()) {
                            return true;
                        }
                        lastLineChar = true;
                    }
                    if (!lastLineChar) {
                        break;
                    }

                } else if (c == '\'' || c == '\"') {
                    //literal found... skip to the end of the literal
                    try {
                        offset = parsingUtils.getLiteralEnd(offset, c) + 1;
                    } catch (SyntaxErrorException e) {
                        //Ignore unbalanced string
                        offset++;
                    }

                } else {
                    // ok, a char is found... go to the end of the line and gather
                    // the spaces to return
                    if (!continueAfterIncreaseOffset()) {
                        return true;
                    }
                }

            }

            if (offset < doc.getLength()) {
                c = doc.getChar(offset);
            } else {
                nextString = null;
                return true;
            }

            //ok, if we got here, we're in a position to get the indentation string as spaces and tabs...
            tempBuf.clear();
            int startingOffset = offset;
            while (c == ' ' || c == '\t') {
                tempBuf.append(c);
                offset++;
                if (offset >= doc.getLength()) {
                    break;
                }
                c = doc.getChar(offset);
            }
            //true if we are in a line that has more contents than only the whitespaces/tabs
            nextString = new Tuple3<String, Integer, Boolean>(tempBuf.toString(), startingOffset, c != '\r'
                    && c != '\n');

            if (!yieldOnLinesWithoutContents) {
                if (!nextString.o3) {
                    return false;
                }
            }
            //now, if we didn't have any indentation, try to make another build
            if (nextString.o1.length() == 0) {
                if (yieldEmptyIndents) {
                    return true;
                }
                return false;
            }

        } catch (RuntimeException e) {
            if (e.getCause() instanceof BadLocationException) {
                throw (BadLocationException) e.getCause();
            }
            throw e;

        }
        return true;
    }

    /**
     * Increase the offset and see whether we should continue iterating in the document after that...
     * @return true if we should continue iterating and false otherwise.
     */
    private boolean continueAfterIncreaseOffset() {
        offset++;
        boolean ret = true;
        if (offset >= doc.getLength()) {
            nextString = null;
            ret = false;
        }
        return ret;
    }

    public void remove() {
        throw new RuntimeException("Not implemented");
    }
}
