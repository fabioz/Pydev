package org.python.pydev.shared_core.partitioner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * A generic scanner which can be "programmed" with a sequence of rules.
 * The scanner is used to get the next token by evaluating its rule in sequence until
 * one is successful. If a rule returns a token which is undefined, the scanner will proceed to
 * the next rule. Otherwise the token provided by the rule will be returned by
 * the scanner. If no rule returned a defined token, this scanner returns a token
 * which returns <code>true</code> when calling <code>isOther</code>, unless the end
 * of the file is reached. In this case the token returns <code>true</code> when calling
 * <code>isEOF</code>.
 *
 * @see IRule
 */
public class RuleBasedScanner implements ICharacterScanner, ITokenScanner {

    /** The list of rules of this scanner */
    protected IRule[] fRules;
    /** The token to be returned by default if no rule fires */
    protected IToken fDefaultReturnToken;
    /** The document to be scanned */
    protected IDocument fDocument;
    /** The cached legal line delimiters of the document */
    protected char[][] fDelimiters;
    /** The offset of the next character to be read */
    protected int fOffset;
    /** The end offset of the range to be scanned */
    protected int fRangeEnd;
    /** The offset of the last read token */
    protected int fTokenOffset;
    /** The cached column of the current scanner position */
    protected int fColumn;
    /** Internal setting for the un-initialized column cache. */
    protected static final int UNDEFINED = -1;

    /**
     * Creates a new rule based scanner which does not have any rule.
     */
    public RuleBasedScanner() {
    }

    /**
     * Configures the scanner with the given sequence of rules.
     *
     * @param rules the sequence of rules controlling this scanner
     */
    public void setRules(IRule[] rules) {
        if (rules != null) {
            fRules = new IRule[rules.length];
            System.arraycopy(rules, 0, fRules, 0, rules.length);
        } else {
            fRules = null;
        }
    }

    /**
     * Configures the scanner's default return token. This is the token
     * which is returned when none of the rules fired and EOF has not been
     * reached.
     *
     * @param defaultReturnToken the default return token
     * @since 2.0
     */
    public void setDefaultReturnToken(IToken defaultReturnToken) {
        Assert.isNotNull(defaultReturnToken.getData());
        fDefaultReturnToken = defaultReturnToken;
    }

    @Override
    public void setRange(final IDocument document, int offset, int length) {
        Assert.isLegal(document != null);
        final int documentLength = document.getLength();
        checkRange(offset, length, documentLength);

        fDocument = document;
        fOffset = offset;
        fColumn = UNDEFINED;
        fRangeEnd = offset + length;

        String[] delimiters = fDocument.getLegalLineDelimiters();
        fDelimiters = new char[delimiters.length][];
        for (int i = 0; i < delimiters.length; i++) {
            fDelimiters[i] = delimiters[i].toCharArray();
        }

        if (fDefaultReturnToken == null) {
            fDefaultReturnToken = new Token(null);
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

    @Override
    public int getTokenOffset() {
        return fTokenOffset;
    }

    @Override
    public int getTokenLength() {
        if (fOffset < fRangeEnd) {
            return fOffset - getTokenOffset();
        }
        return fRangeEnd - getTokenOffset();
    }

    @Override
    public int getColumn() {
        if (fColumn == UNDEFINED) {
            try {
                int line = fDocument.getLineOfOffset(fOffset);
                int start = fDocument.getLineOffset(line);

                fColumn = fOffset - start;

            } catch (BadLocationException ex) {
            }
        }
        return fColumn;
    }

    @Override
    public char[][] getLegalLineDelimiters() {
        return fDelimiters;
    }

    @Override
    public IToken nextToken() {

        fTokenOffset = fOffset;
        fColumn = UNDEFINED;

        if (fRules != null) {
            for (IRule fRule : fRules) {
                IToken token = (fRule.evaluate(this));
                if (!token.isUndefined()) {
                    return token;
                }
            }
        }

        if (read() == EOF) {
            return Token.EOF;
        }
        return fDefaultReturnToken;
    }

    @Override
    public int read() {

        try {

            if (fOffset < fRangeEnd) {
                try {
                    return fDocument.getChar(fOffset);
                } catch (BadLocationException e) {
                }
            }

            return EOF;

        } finally {
            ++fOffset;
            fColumn = UNDEFINED;
        }
    }

    @Override
    public void unread() {
        --fOffset;
        fColumn = UNDEFINED;
    }
}
