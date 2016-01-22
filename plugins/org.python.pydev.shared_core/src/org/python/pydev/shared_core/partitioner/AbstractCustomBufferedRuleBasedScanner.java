/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.python.pydev.shared_core.partitioner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * A buffered rule based scanner. The buffer always contains a section
 * of a fixed size of the document to be scanned. Completely adheres to
 * the contract of <code>RuleBasedScanner</code>.
 */
public abstract class AbstractCustomBufferedRuleBasedScanner extends AbstractCustomRuleBasedScanner
        implements IMarkScanner, IContentsScanner {

    /** The default buffer size. Value = 2000 -- note: default was 500 in original */
    private final static int DEFAULT_BUFFER_SIZE = 2000;
    /** The actual size of the buffer. Initially set to <code>DEFAULT_BUFFER_SIZE</code> */
    private int fBufferSize = DEFAULT_BUFFER_SIZE;
    /** The buffer */
    private char[] fBuffer = new char[DEFAULT_BUFFER_SIZE];
    /** The offset of the document at which the buffer starts */
    private int fStart;
    /** The offset of the document at which the buffer ends */
    private int fEnd;
    /** The cached length of the document */
    private int fDocumentLength;

    private int lastRegexpMatchOffset;

    public void setLastRegexpMatchOffset(int endOffset) {
        this.lastRegexpMatchOffset = endOffset;
    }

    public int getLastRegexpMatchOffset() {
        return lastRegexpMatchOffset;
    }

    /**
     * Creates a new buffered rule based scanner which does
     * not have any rule and a default buffer size of 500 characters.
     */
    protected AbstractCustomBufferedRuleBasedScanner() {
        super();
    }

    /**
     * Creates a new buffered rule based scanner which does
     * not have any rule. The buffer size is set to the given
     * number of characters.
     *
     * @param size the buffer size
     */
    public AbstractCustomBufferedRuleBasedScanner(int size) {
        super();
        setBufferSize(size);
    }

    /**
     * Sets the buffer to the given number of characters.
     *
     * @param size the buffer size
     */
    protected void setBufferSize(int size) {
        Assert.isTrue(size > 0);
        fBufferSize = size;
        fBuffer = new char[size];
    }

    /**
     * Shifts the buffer so that the buffer starts at the
     * given document offset.
     *
     * @param offset the document offset at which the buffer starts
     */
    private void shiftBuffer(int offset) {

        fStart = offset;
        fEnd = fStart + fBufferSize;
        if (fEnd > fDocumentLength) {
            fEnd = fDocumentLength;
        }

        try {

            String content = fDocument.get(fStart, fEnd - fStart);
            content.getChars(0, fEnd - fStart, fBuffer, 0);

        } catch (BadLocationException x) {
        }
    }

    /*
     * @see RuleBasedScanner#setRange(IDocument, int, int)
     */
    @Override
    public void setRange(IDocument document, int offset, int length) {

        super.setRange(document, offset, length);

        fDocumentLength = document.getLength();
        shiftBuffer(offset);
    }

    public int getMark() {
        return fOffset;
    }

    @Override
    public void getContents(int offset, int length, FastStringBuffer buffer) {
        buffer.resizeForMinimum(buffer.length() + length);
        int mark = this.getMark();
        this.setMark(offset);
        try {
            for (int i = 0; i < length; i++) {
                buffer.append((char) this.read());
            }
        } finally {
            this.setMark(mark);
        }
    }

    public void setMark(int offset) {
        fOffset = offset;
        fColumn = UNDEFINED;

        if (fOffset == fStart) {
            shiftBuffer(Math.max(0, fStart - (fBufferSize / 2)));

        } else if (fOffset == fEnd) {
            shiftBuffer(fEnd);

        } else if (fOffset < fStart || fEnd < fOffset) {
            shiftBuffer(fOffset);

        }
    }

    // Support for temporarily pushing a sub-range during a partitioning.
    private FastStack<TempStacked> rangeStack = new FastStack<>(3);

    private static class TempStacked {

        private int offset;
        private int rangeEnd;
        private int lastRegexpMatchOffset;

        public TempStacked(int offset, int rangeEnd, int lastRegexpMatchOffset) {
            this.offset = offset;
            this.rangeEnd = rangeEnd;
            this.lastRegexpMatchOffset = lastRegexpMatchOffset;
        }

    }

    public void pushRange(int offset, int len) {
        rangeStack.push(new TempStacked(fOffset, fRangeEnd, lastRegexpMatchOffset));
        this.fOffset = offset;
        this.fRangeEnd = offset + len;
        this.setMark(fOffset);
    }

    public void popRange() {
        TempStacked pop = rangeStack.pop();
        this.fOffset = pop.offset;
        this.fRangeEnd = pop.rangeEnd;
        //Although it's not changed at push, it must be restored.
        this.lastRegexpMatchOffset = pop.lastRegexpMatchOffset;
        this.setMark(fOffset);
    }

    /*
     * @see RuleBasedScanner#read()
     */
    @Override
    public int read() {
        fColumn = UNDEFINED;
        if (fOffset >= fRangeEnd) {
            ++fOffset;
            return EOF;
        }

        if (fOffset == fEnd) {
            shiftBuffer(fEnd);
        } else if (fOffset < fStart || fEnd < fOffset) {
            shiftBuffer(fOffset);
        }

        return fBuffer[fOffset++ - fStart];
    }

    /*
     * @see RuleBasedScanner#unread()
     */
    @Override
    public void unread() {

        if (fOffset == fStart) {
            shiftBuffer(Math.max(0, fStart - (fBufferSize / 2)));
        }

        --fOffset;
        fColumn = UNDEFINED;
    }
}
