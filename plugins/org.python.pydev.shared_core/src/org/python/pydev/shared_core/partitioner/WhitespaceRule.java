package org.python.pydev.shared_core.partitioner;

import org.eclipse.core.runtime.Assert;

/**
 * An implementation of <code>IRule</code> capable of detecting whitespace.
 * A whitespace rule uses a whitespace detector in order to find out which
 * characters are whitespace characters.
 *
 * @see IWhitespaceDetector
 */
public class WhitespaceRule implements IRule {

    /** The whitespace detector used by this rule */
    protected IWhitespaceDetector fDetector;

    /**
     * The token returned for whitespace.
     * @since 3.5
     */
    protected final IToken fWhitespaceToken;

    /**
     * Creates a rule which, with the help of an whitespace detector, will return
     * {@link Token#WHITESPACE} when a whitespace is detected.
     *
     * @param detector the rule's whitespace detector
     */
    public WhitespaceRule(IWhitespaceDetector detector) {
        this(detector, Token.WHITESPACE);
    }

    /**
     * Creates a rule which, with the help of an whitespace detector, will return the given
     * whitespace token when a whitespace is detected.
     *
     * @param detector the rule's whitespace detector
     * @param token the token returned for whitespace
     * @since 3.5
     */
    public WhitespaceRule(IWhitespaceDetector detector, IToken token) {
        Assert.isNotNull(detector);
        Assert.isNotNull(token);
        fDetector = detector;
        fWhitespaceToken = token;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link #fWhitespaceToken} if whitespace got detected, {@link Token#UNDEFINED}
     *         otherwise
     */
    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        if (fDetector.isWhitespace((char) c)) {
            do {
                c = scanner.read();
            } while (fDetector.isWhitespace((char) c));
            scanner.unread();
            return fWhitespaceToken;
        }

        scanner.unread();
        return Token.UNDEFINED;
    }
}
