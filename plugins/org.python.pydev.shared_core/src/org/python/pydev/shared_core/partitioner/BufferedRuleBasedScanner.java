package org.python.pydev.shared_core.partitioner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * A buffered rule based scanner. The buffer always contains a section
 * of a fixed size of the document to be scanned. Completely adheres to
 * the contract of <code>RuleBasedScanner</code>.
 */
public class BufferedRuleBasedScanner extends RuleBasedScanner {

    /** The default buffer size. Value = 500 */
    private final static int DEFAULT_BUFFER_SIZE = 500;
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

    /**
     * Creates a new buffered rule based scanner which does
     * not have any rule and a default buffer size of 500 characters.
     */
    protected BufferedRuleBasedScanner() {
        super();
    }

    /**
     * Creates a new buffered rule based scanner which does
     * not have any rule. The buffer size is set to the given
     * number of characters.
     *
     * @param size the buffer size
     */
    public BufferedRuleBasedScanner(int size) {
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

    @Override
    public void setRange(IDocument document, int offset, int length) {

        super.setRange(document, offset, length);

        fDocumentLength = document.getLength();
        shiftBuffer(offset);
    }

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

    @Override
    public void unread() {

        if (fOffset == fStart) {
            shiftBuffer(Math.max(0, fStart - (fBufferSize / 2)));
        }

        --fOffset;
        fColumn = UNDEFINED;
    }
}
