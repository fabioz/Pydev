/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.changed_lines;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.log.Log;


/**
 * Copy of org.eclipse.jdt.internal.ui.text.LineComparator.
 * 
 * This implementation of <code>IRangeComparator</code> compares lines of a document.
 * The lines are compared using a DJB hash function.
 */
public class LineComparator implements IRangeComparator {

    private final IDocument fDocument;
    private final ArrayList<Integer> fHashes;

    /**
     * Create a line comparator for the given document.
     *
     * @param document
     */
    public LineComparator(IDocument document) {
        fDocument = document;

        Integer[] nulls = new Integer[fDocument.getNumberOfLines()];
        fHashes = new ArrayList<Integer>(Arrays.asList(nulls));
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#getRangeCount()
     */
    @Override
    public int getRangeCount() {
        return fDocument.getNumberOfLines();
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#rangesEqual(int, org.eclipse.compare.rangedifferencer.IRangeComparator, int)
     */
    @Override
    public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
        try {
            return getHash(thisIndex).equals(((LineComparator) other).getHash(otherIndex));
        } catch (BadLocationException e) {
            Log.log(e);
            return false;
        }
    }

    /*
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#skipRangeComparison(int, int, org.eclipse.compare.rangedifferencer.IRangeComparator)
     */
    @Override
    public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
        return false;
    }

    /**
     * @param line the number of the line in the document to get the hash for
     * @return the hash of the line
     * @throws BadLocationException if the line number is invalid
     */
    private Integer getHash(int line) throws BadLocationException {
        Integer hash = (Integer) fHashes.get(line);
        if (hash == null) {
            IRegion lineRegion = fDocument.getLineInformation(line);
            String lineContents = fDocument.get(lineRegion.getOffset(), lineRegion.getLength());
            hash = computeDJBHash(lineContents);
            fHashes.set(line, hash);
        }

        return hash;
    }

    /**
     * Compute a hash using the DJB hash algorithm
     *
     * @param string the string for which to compute a hash
     * @return the DJB hash value of the string
     */
    private int computeDJBHash(String string) {
        int hash = 5381;
        int len = string.length();
        for (int i = 0; i < len; i++) {
            char ch = string.charAt(i);
            hash = (hash << 5) + hash + ch;
        }

        return hash;
    }
}
