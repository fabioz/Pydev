/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * TAKEN FROM 
 * 
 * org.eclipse.jdt.internal.ui.text.JavaPairMatcher
 */
package org.python.pydev.core.docutils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.python.pydev.shared_core.string.ICharacterPairMatcher2;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * A character pair matcher finds to a character at a certain document offset the matching peer character. It
 * is the matchers responsibility to define the concepts of "matching" and "peer". The matching process starts
 * at a given offset. Starting of this offset, the matcher chooses a character close to this offset. The
 * anchor defines whether the chosen character is left or right of the initial offset. The matcher then
 * searches for the matching peer character of the chosen character and if it finds one, delivers the minimal
 * region of the document that contains both characters.
 * 
 * Typical usage of this class is something like the following:
 * 
 * @code
   PythonPairMatcher matcher = new PythonPairMatcher(PyDoubleClickStrategy.BRACKETS);
   IRegion region = matcher.match(document, offset);
   if (region != null)
   {
     // do something
   }
   @endcode
 * 
 * @author Fabio Zadrozny
 * @see org.eclipse.jface.text.source.ICharacterPairMatcher
 */
public class PythonPairMatcher implements ICharacterPairMatcher, ICharacterPairMatcher2 {

    protected char[] fPairs;

    protected IDocument fDocument;

    protected int fOffset;

    protected int fStartPos;

    protected int fEndPos;

    protected int fAnchor;

    protected PythonCodeReader fReader = new PythonCodeReader();

    public PythonPairMatcher() {
        this(PyStringUtils.BRACKETS);
    }

    /**
     * Constructor which accepts an array of array of characters you want to interpreted as pairs.
     * 
     * Most commonly, you'll simply use STANDARD_PYTHON_PAIRS.
     * 
     * @param pairs an array of characters to be interprested as pairs; the array size must be a multiple of
     *            two, and the first element of the "pair" must be the beginning brace, and the "second"
     *            element of the pair must be the ending brace. For example, pairs[0] = '(', pairs[1] = ')'
     */
    public PythonPairMatcher(char[] pairs) {
        fPairs = pairs;
    }

    /**
     * Match the brace specified by the arguments and return the region.
     * 
     * @param document the document in which to search
     * @param offset the offset where the brace is
     * @return the region describing the
     * @see org.eclipse.jface.text.source.ICharacterPairMatcher#match(org.eclipse.jface.text.IDocument, int)
     */
    public IRegion match(IDocument document, int offset) {

        fOffset = offset;

        if (fOffset < 0) {
            return null;
        }

        fDocument = document;

        if (fDocument != null && matchPairsAt() && fStartPos != fEndPos) {
            return new Region(fStartPos, fEndPos - fStartPos + 1);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.source.ICharacterPairMatcher#getAnchor()
     */
    public int getAnchor() {
        return fAnchor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.source.ICharacterPairMatcher#dispose()
     */
    public void dispose() {
        clear();
        fDocument = null;
        fReader = null;
    }

    /*
     * @see org.eclipse.jface.text.source.ICharacterPairMatcher#clear()
     */
    public void clear() {
        if (fReader != null) {
            try {
                fReader.close();
            } catch (IOException x) {
                // ignore
            }
        }
    }

    protected boolean matchPairsAt() {

        int i;
        int pairIndex1 = fPairs.length;
        int pairIndex2 = fPairs.length;

        fStartPos = -1;
        fEndPos = -1;

        // get the chars preceding and following the start position
        try {

            char prevChar = fDocument.getChar(Math.max(fOffset - 1, 0));
            // modified behavior for http://dev.eclipse.org/bugs/show_bug.cgi?id=16879
            // char nextChar= fDocument.getChar(fOffset);

            // search for opening peer character next to the activation point
            for (i = 0; i < fPairs.length; i = i + 2) {
                // if (nextChar == fPairs[i]) {
                // fStartPos= fOffset;
                // pairIndex1= i;
                // } else
                if (prevChar == fPairs[i]) {
                    fStartPos = fOffset - 1;
                    pairIndex1 = i;
                }
            }

            // search for closing peer character next to the activation point
            for (i = 1; i < fPairs.length; i = i + 2) {
                if (prevChar == fPairs[i]) {
                    fEndPos = fOffset - 1;
                    pairIndex2 = i;
                }
                // else if (nextChar == fPairs[i]) {
                // fEndPos= fOffset;
                // pairIndex2= i;
                // }
            }

            if (fEndPos > -1) {
                fAnchor = RIGHT;
                fStartPos = searchForOpeningPeer(fEndPos, fPairs[pairIndex2 - 1], fPairs[pairIndex2], fDocument);
                if (fStartPos > -1) {
                    return true;
                } else {
                    fEndPos = -1;
                }
            } else if (fStartPos > -1) {
                fAnchor = LEFT;
                fEndPos = searchForClosingPeer(fStartPos, fPairs[pairIndex1], fPairs[pairIndex1 + 1], fDocument);
                if (fEndPos > -1) {
                    return true;
                } else {
                    fStartPos = -1;
                }
            }

        } catch (BadLocationException x) {
        }

        return false;
    }

    /**
     * If you found an opening peer, you'll want to look for a closing peer.
     * 
     * @param offset
     * @param openingPeer
     * @param closingPeer
     * @param document
     * @return the offset of the closing peer
     * @throws IOException
     */
    public int searchForClosingPeer(int offset, char openingPeer, char closingPeer, IDocument document) {
        try {
            fReader.configureForwardReader(document, offset + 1, document.getLength(), true, true, true);

            int stack = 1;
            int c = fReader.read();
            while (c != PythonCodeReader.EOF) {
                if (c == openingPeer && c != closingPeer) {
                    stack++;
                } else if (c == closingPeer) {
                    stack--;
                }

                if (stack <= 0) { //<= 0 because if we have a closing peer without an opening one, we'll return it.
                    return fReader.getOffset();
                }

                c = fReader.read();
            }

            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * If you found a closing peer, you'll want to search for an opening peer.
     * 
     * @param offset
     * @param openingPeer
     * @param closingPeer
     * @param document
     * @return the offset of the opening peer
     * @throws IOException
     */
    public int searchForOpeningPeer(int offset, char openingPeer, char closingPeer, IDocument document) {

        try {
            fReader.configureBackwardReader(document, offset, true, true, true);

            int stack = 1;
            int c = fReader.read();
            while (c != PythonCodeReader.EOF) {
                if (c == closingPeer && c != openingPeer) {
                    stack++;
                } else if (c == openingPeer) {
                    stack--;
                }

                if (stack <= 0) {//<= 0 because if we have an opening peer without a closing one, we'll return it.
                    return fReader.getOffset();
                }

                c = fReader.read();
            }

            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int searchForAnyOpeningPeer(int offset, IDocument document) {
        try {
            fReader.configureBackwardReader(document, offset, true, true, true);

            Map<Character, Integer> stack = new HashMap<Character, Integer>();

            HashSet<Character> closing = new HashSet<Character>();
            HashSet<Character> opening = new HashSet<Character>();

            for (int i = 0; i < fPairs.length; i++) {
                stack.put(fPairs[i], 1);
                if (i % 2 == 0) {
                    opening.add(fPairs[i]);
                } else {
                    closing.add(fPairs[i]);
                }
            }

            int c = fReader.read();
            while (c != PythonCodeReader.EOF) {
                if (closing.contains((char) c)) { // c == ')' || c == ']' || c == '}' 
                    char peer = StringUtils.getPeer((char) c);
                    Integer iStack = stack.get(peer);
                    iStack++;
                    stack.put(peer, iStack);

                } else if (opening.contains((char) c)) { //c == '(' || c == '[' || c == '{'
                    Integer iStack = stack.get((char) c);
                    iStack--;
                    stack.put((char) c, iStack);

                    if (iStack == 0) {
                        return fReader.getOffset();
                    }
                }

                c = fReader.read();
            }

            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
