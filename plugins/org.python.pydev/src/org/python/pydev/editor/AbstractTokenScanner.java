package org.python.pydev.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.python.pydev.ui.ColorAndStyleCache;

public abstract class AbstractTokenScanner implements ITokenScanner {

    protected final ColorAndStyleCache colorCache;
    protected char[] fChars;
    protected int fInitialOffset;
    protected int fCurrentTokenIndexStartRelativeToInitialOffset;
    protected int fCurrentIndexRelativeToInitialOffset;

    public AbstractTokenScanner(ColorAndStyleCache colorCache) {
        super();
        this.colorCache = colorCache;
        updateColorAndStyle();
    }

    protected abstract void updateColorAndStyle();

    /*
     * @see ITokenScanner#setRange(IDocument, int, int)
     */
    @Override
    public void setRange(final IDocument document, int offset, int length) {
        Assert.isLegal(document != null);
        final int documentLength = document.getLength();
        checkRange(offset, length, documentLength);

        fInitialOffset = offset;
        fCurrentTokenIndexStartRelativeToInitialOffset = 0;
        fCurrentIndexRelativeToInitialOffset = 0;
        try {
            fChars = document.get(offset, length).toCharArray();
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Checks that the given range is valid.
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=69292
     *
     * @param offset the offset of the document range to scan
     * @param length the length of the document range to scan
     * @param documentLength the document's length
     * @since 3.3
     */
    private void checkRange(int offset, int length, int documentLength) {
        Assert.isLegal(offset > -1);
        Assert.isLegal(length > -1);
        Assert.isLegal(offset + length <= documentLength);
    }

    /*
     * @see ITokenScanner#getTokenOffset()
     */
    @Override
    public int getTokenOffset() {
        return fInitialOffset + fCurrentTokenIndexStartRelativeToInitialOffset;
    }

    /*
     * @see ITokenScanner#getTokenLength()
     */
    @Override
    public int getTokenLength() {
        return fCurrentIndexRelativeToInitialOffset - fCurrentTokenIndexStartRelativeToInitialOffset;
    }

    protected int read() {
        if (fCurrentIndexRelativeToInitialOffset >= fChars.length) {
            fCurrentIndexRelativeToInitialOffset++;
            return -1;
        }
        char c = fChars[fCurrentIndexRelativeToInitialOffset];
        fCurrentIndexRelativeToInitialOffset++;
        return c;
    }

    protected void unread() {
        fCurrentIndexRelativeToInitialOffset--;
    }

}
