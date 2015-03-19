package org.python.pydev.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.ui.ColorAndStyleCache;

public abstract class AbstractStringScanner implements ITokenScanner {
    protected final ColorAndStyleCache colorCache;
    protected Token fDocStringMarkupTextReturnToken;
    protected IToken fStringReturnToken;
    protected char[] fChars;
    protected int fOffset;
    protected int fCurrIndex;
    protected int fstart;

    public AbstractStringScanner(ColorAndStyleCache colorCache) {
        super();
        this.colorCache = colorCache;
        updateColorAndStyle();
    }

    protected abstract void updateColorAndStyle();

    /*
     * @see ITokenScanner#setRange(IDocument, int, int)
     */
    public void setRange(final IDocument document, int offset, int length) {
        Assert.isLegal(document != null);
        final int documentLength = document.getLength();
        checkRange(offset, length, documentLength);

        fOffset = offset;
        fCurrIndex = 0;
        fstart = 0;
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
    public int getTokenOffset() {
        return fOffset + fstart;
    }

    /*
     * @see ITokenScanner#getTokenLength()
     */
    public int getTokenLength() {
        return fCurrIndex - fstart;
    }

    /*
     * @see ITokenScanner#nextToken()
     */
    public IToken nextToken() {
        fstart = fCurrIndex;

        int c = read();
        if (c == -1) {
            //This isn't really in the contract, but it should work anyways: users do a setRange, then:
            //consume tokens until EOF (at which point we can clear our buffer).
            fChars = null;
            return Token.EOF;
        }
        if (Character.isWhitespace(c)) {
            while (Character.isWhitespace(c) && c != -1) {
                c = read();
            }
            unread();
            return fStringReturnToken;
        }

        if (c == '@' || c == ':') {
            //Looking for @ or : in the start of the line
            c = read();
            if (c == -1) {
                unread();
                return fDocStringMarkupTextReturnToken;
            }
            while (Character.isJavaIdentifierPart(c)) {
                c = read();
            }
            unread();
            return fDocStringMarkupTextReturnToken;

        } else {
            // read to the end of the line
            while (c != -1 && c != '\r' && c != '\n') {
                c = read();
            }
            if (c == -1) {
                unread();
                return fStringReturnToken;
            }
            while (c == '\r' && c == '\n') {
                c = read();
            }
            unread();
        }

        return fStringReturnToken;
    }

    private int read() {
        if (fCurrIndex >= fChars.length) {
            fCurrIndex++;
            return -1;
        }
        char c = fChars[fCurrIndex];
        fCurrIndex++;
        return c;
    }

    private void unread() {
        fCurrIndex--;
    }
}
